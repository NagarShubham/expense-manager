package com.example.expensemanager.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.expensemanager.data.BudgetExcludedCategory
import com.example.expensemanager.data.BudgetRepository
import com.example.expensemanager.data.Category
import com.example.expensemanager.data.CategoryRepository
import com.example.expensemanager.data.Expense
import com.example.expensemanager.data.ExpenseRepository
import com.example.expensemanager.data.MonthlyBudget
import com.example.expensemanager.util.BackupManager
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * Writes a backup file when the data has actually changed since the last one.
 *
 * Runs periodically rather than per-edit, and exits early unless [AutoBackupStore] flags a
 * change; the comparison is a content signature, so an edit-then-undo writes nothing.
 *
 * Every dependency is [Lazy]: most runs decide to do nothing, so nothing should open the
 * encrypted database before that decision is made.
 */
@HiltWorker
internal class AutoBackupWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParameters: WorkerParameters,
        private val expenseRepository: Lazy<ExpenseRepository>,
        private val budgetRepository: Lazy<BudgetRepository>,
        private val categoryRepository: Lazy<CategoryRepository>,
        private val backupManager: Lazy<BackupManager>,
        private val store: AutoBackupStore,
        private val fileStore: BackupFileStore
    ) : CoroutineWorker(appContext, workerParameters) {
        override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                if (!store.isEnabled.value) return@withContext Result.success()
                // Nothing to retry for: without storage access, a later run cannot do better
                // until the user changes something, and that re-triggers scheduling anyway.
                if (!fileStore.isWritable()) return@withContext Result.success()

                val state = store.state()
                val now = System.currentTimeMillis()
                if (!needsInspection(state, now)) return@withContext Result.success()

                try {
                    runBackup(state, now)
                } catch (cancellation: CancellationException) {
                    // Cooperative cancellation (WorkManager stopped us) is not a failure.
                    throw cancellation
                } catch (_: Exception) {
                    retryOrGiveUp()
                }
            }

        /**
         * Whether it is worth reading the data at all. The periodic re-check catches a backup
         * deleted outside the app; `now < lastVerifiedAt` covers the clock moving backwards.
         */
        private fun needsInspection(
            state: AutoBackupStore.State,
            now: Long
        ): Boolean =
            state.dirty ||
                now < state.lastVerifiedAt ||
                now - state.lastVerifiedAt >= VERIFICATION_INTERVAL_MS

        private suspend fun runBackup(
            state: AutoBackupStore.State,
            now: Long
        ): Result {
            val export = loadExportData()
            // An empty database is not worth a backup file, and writing one would let the
            // retention sweep push a real backup out of the window.
            if (export.expenses.isEmpty()) {
                store.recordVerified(now)
                return Result.success()
            }

            val signature = backupManager.get().contentSignature(
                export.expenses,
                export.monthlyBudgets,
                export.budgetExcludedCategories,
                export.categories
            )
            // Both halves matter: the content must match AND the file must still exist.
            if (signature == state.contentSignature && fileStore.exists(state.lastBackupUri)) {
                store.recordVerified(now)
                return Result.success()
            }

            val target = fileStore.create(backupManager.get().generateAutoBackupFileName())
                ?: return retryOrGiveUp()

            val written = backupManager.get().exportToJson(
                target.uri,
                export.expenses,
                export.monthlyBudgets,
                export.budgetExcludedCategories,
                export.categories
            )
            return if (written.isFailure) {
                fileStore.discard(target)
                retryOrGiveUp()
            } else {
                fileStore.publish(target)
                store.recordBackup(signature, target.uri.toString(), now)
                fileStore.prune(MAX_RETAINED_BACKUPS)
                Result.success()
            }
        }

        /** Reads the four independent tables concurrently. */
        private suspend fun loadExportData(): ExportData =
            coroutineScope {
                val expenses = async { expenseRepository.get().getAllExpensesForExport() }
                val budgets = async { budgetRepository.get().getAllBudgetsForExport() }
                val exclusions = async { budgetRepository.get().getAllExcludedCategoriesForExport() }
                val categories = async { categoryRepository.get().getAllForExport() }
                ExportData(
                    expenses = expenses.await(),
                    monthlyBudgets = budgets.await(),
                    budgetExcludedCategories = exclusions.await(),
                    categories = categories.await()
                )
            }

        /**
         * Retries a few times, then reports success anyway: `failure()` would drop this out of
         * the periodic chain permanently. The data stays dirty, so the next run retries.
         */
        private fun retryOrGiveUp(): Result =
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.success()

        /** Everything a backup file contains, read in one pass. */
        private data class ExportData(
            val expenses: List<Expense>,
            val monthlyBudgets: List<MonthlyBudget>,
            val budgetExcludedCategories: List<BudgetExcludedCategory>,
            val categories: List<Category>
        )

        companion object {
            /** How many auto-backup files are kept in the folder. */
            const val MAX_RETAINED_BACKUPS = 5

            private const val MAX_ATTEMPTS = 3

            /** How often to re-check even when nothing looks changed, to catch a deleted file. */
            private val VERIFICATION_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)

            private const val TAG = "auto_backup"
            private const val PERIODIC_WORK_NAME = "auto_backup_periodic"
            private const val ONE_SHOT_WORK_NAME = "auto_backup_now"

            /** How often the data is checked for changes worth backing up. */
            private val REPEAT_INTERVAL = Duration.ofHours(24)

            /** Tail of each period the run may land in, so the OS can batch the wake-up. */
            private val FLEX_INTERVAL = Duration.ofHours(4)

            /** Brings the periodic schedule in line with whether auto-backup is enabled. */
            internal fun sync(
                context: Context,
                enabled: Boolean
            ) {
                if (enabled) schedule(context) else cancel(context)
            }

            /**
             * Runs one backup immediately, so enabling the feature produces a file now rather
             * than at the next daily run. Same constraints and backoff as the periodic path.
             */
            internal fun runNow(context: Context) {
                val request = OneTimeWorkRequestBuilder<AutoBackupWorker>()
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiresBatteryNotLow(true)
                            .setRequiresStorageNotLow(true)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10_000L, TimeUnit.MILLISECONDS)
                    .addTag(TAG)
                    .build()
                WorkManager
                    .getInstance(context)
                    .enqueueUniqueWork(ONE_SHOT_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
            }

            private fun schedule(context: Context) {
                val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(REPEAT_INTERVAL, FLEX_INTERVAL)
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiresBatteryNotLow(true)
                            .setRequiresStorageNotLow(true)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10_000L, TimeUnit.MILLISECONDS)
                    .addTag(TAG)
                    .build()
                // UPDATE, not KEEP or REPLACE: KEEP does nothing if no schedule exists (a lost
                // schedule stays lost); REPLACE pushes the next run out a full interval on every
                // cold start. UPDATE creates it when missing and keeps the existing run window.
                // No setInitialDelay: the early-exit guard makes an immediate first window cheap.
                WorkManager
                    .getInstance(context)
                    .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
            }

            private fun cancel(context: Context) {
                WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            }
        }
    }
