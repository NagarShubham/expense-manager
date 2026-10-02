package com.example.expensemanager.backup

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensemanager.util.BackupManager
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes a backup only when the data's content hash differs from the last backup; skips when
 * there are no expenses. [Lazy] deps keep a disabled/unwritable run from opening the database.
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
            // An empty backup would push a real one out of the retention window.
            if (snapshot.expenses.isEmpty()) return Result.success()

            val signature = manager.contentSignature(
                snapshot.expenses,
                snapshot.monthlyBudgets,
                snapshot.budgetExcludedCategories,
                snapshot.categories
            )
            // The user may have deleted the last file, so existence is checked too.
            if (signature == state.contentSignature && fileStore.exists(state.lastBackupUri)) {
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
