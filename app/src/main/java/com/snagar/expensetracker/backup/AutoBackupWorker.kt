package com.snagar.expensetracker.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.snagar.expensetracker.util.BackupManager
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Periodic (12h) backup. Writes only when expenses exist and the content hash is not already
 * stored as a live file; otherwise skips. [Lazy] deps keep a disabled/unwritable run from
 * opening the database until a real backup is attempted.
 */
@HiltWorker
internal class AutoBackupWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParameters: WorkerParameters,
        private val backupManager: Lazy<BackupManager>,
        private val store: AutoBackupStore,
        private val fileStore: BackupFileStore
    ) : CoroutineWorker(appContext, workerParameters) {
        override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                if (!store.isEnabled.value) return@withContext Result.success()
                // Retrying can't help until the user grants storage access.
                if (!fileStore.isWritable()) {
                    Log.w(LOG_TAG, "Skipping auto-backup: storage not writable")
                    return@withContext Result.success()
                }

                try {
                    runBackup(store.state())
                } catch (cancellation: CancellationException) {
                    // Cooperative cancellation (WorkManager stopped us) is not a failure.
                    throw cancellation
                } catch (e: Exception) {
                    Log.w(LOG_TAG, "Auto-backup failed (attempt ${runAttemptCount + 1})", e)
                    retryOrGiveUp()
                }
            }

        private suspend fun runBackup(state: AutoBackupStore.State): Result {
            val manager = backupManager.get()
            val snapshot = manager.loadSnapshot()
            // Empty list: never write a file (and never open retention) — scheduling is
            // cancelled from AutoBackupStore when the count hits zero.
            if (snapshot.expenses.isEmpty()) {
                Log.d(LOG_TAG, "Skipping auto-backup: no expenses")
                store.cancelIfStillEmpty()
                return Result.success()
            }

            val signature = manager.contentSignature(
                snapshot.expenses,
                snapshot.monthlyBudgets,
                snapshot.budgetExcludedCategories,
                snapshot.categories
            )
            // Already backed up (same content and the file is still on disk).
            if (signature == state.contentSignature && fileStore.exists(state.lastBackupUri)) {
                Log.d(LOG_TAG, "Skipping auto-backup: data already backed up")
                return Result.success()
            }
            // The feature may have been switched off during the read.
            if (!store.isEnabled.value) return Result.success()

            val target = fileStore.create(manager.generateAutoBackupFileName())
                ?: return failed("could not create backup file")

            val written = manager.exportToJson(
                target.uri,
                snapshot.expenses,
                snapshot.monthlyBudgets,
                snapshot.budgetExcludedCategories,
                snapshot.categories
            )
            // An unpublished (IS_PENDING) file is invisible and auto-deleted, so treat it as failed.
            if (written.isFailure || !fileStore.publish(target)) {
                fileStore.discard(target)
                return failed("write/publish failed", written.exceptionOrNull())
            }

            store.recordBackup(signature, target.uri.toString())
            fileStore.prune(MAX_RETAINED_BACKUPS)
            return Result.success()
        }

        private fun failed(
            reason: String,
            cause: Throwable? = null
        ): Result {
            Log.w(LOG_TAG, "Auto-backup failed: $reason (attempt ${runAttemptCount + 1})", cause)
            return retryOrGiveUp()
        }

        /** Retries a few times, then succeeds anyway: `failure()` would end the periodic chain. */
        private fun retryOrGiveUp(): Result =
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()

        internal companion object {
            /** How many auto-backup files are kept in the folder. */
            const val MAX_RETAINED_BACKUPS = 5

            private const val MAX_ATTEMPTS = 3
            private const val LOG_TAG = "AutoBackupWorker"
        }
    }
