package com.example.expensemanager.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensemanager.data.BudgetExcludedCategory
import com.example.expensemanager.data.BudgetRepository
import com.example.expensemanager.data.Category
import com.example.expensemanager.data.CategoryRepository
import com.example.expensemanager.data.Expense
import com.example.expensemanager.data.ExpenseRepository
import com.example.expensemanager.data.MonthlyBudget
import com.example.expensemanager.data.PreferenceRepository
import com.example.expensemanager.util.BackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Recurring worker that exports the full database to a JSON file in the shared
 * Downloads/ExpenseBackup folder. Scheduled to repeat every 15 minutes by [BackupScheduler].
 *
 * Change detection: the worker computes a content fingerprint of the current data and
 * compares it with the fingerprint stored after the previous successful backup. If they
 * match, nothing has changed since the last backup and the run is skipped
 * ([androidx.work.ListenableWorker.Result.success] without writing a file). This avoids
 * piling up identical backups when the user hasn't touched their data.
 *
 * On any transient failure (I/O error, etc.) the worker returns
 * [androidx.work.ListenableWorker.Result.retry] so WorkManager reschedules with backoff.
 */
@HiltWorker
internal class AutoBackupWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val expenseRepository: ExpenseRepository,
        private val budgetRepository: BudgetRepository,
        private val categoryRepository: CategoryRepository,
        private val backupManager: BackupManager,
        private val preferenceRepository: PreferenceRepository
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result =
            try {
                val snapshot = fetchSnapshot()
                when {
                    snapshot.isEmpty -> Result.success()
                    else -> backupIfChanged(snapshot)
                }
            } catch (e: CancellationException) {
                // The worker was stopped (constraint lost, app killed, rescheduled, …).
                // Let cancellation propagate so WorkManager can reschedule cleanly.
                throw e
            } catch (_: Exception) {
                Result.retry()
            }

        /** Reads every backed-up table in parallel and captures a consistent-ish snapshot. */
        private suspend fun fetchSnapshot(): BackupSnapshot =
            coroutineScope {
                val expenses = async { expenseRepository.getAllExpensesForExport() }
                val budgets = async { budgetRepository.getAllBudgetsForExport() }
                val exclusions = async { budgetRepository.getAllExcludedCategoriesForExport() }
                val categories = async { categoryRepository.getAllForExport() }
                BackupSnapshot(
                    expenses = expenses.await(),
                    budgets = budgets.await(),
                    exclusions = exclusions.await(),
                    categories = categories.await()
                )
            }

        private suspend fun backupIfChanged(snapshot: BackupSnapshot): Result {
            val fingerprint = backupManager.computeFingerprint(
                snapshot.expenses,
                snapshot.budgets,
                snapshot.exclusions,
                snapshot.categories
            )
            // No changes since the last backup — skip this run.
            if (fingerprint == preferenceRepository.getLastBackupFingerprint()) {
                return Result.success()
            }

            val exportResult = backupManager.exportToDownloads(
                subDir = BACKUP_SUBDIR,
                expenses = snapshot.expenses,
                monthlyBudgets = snapshot.budgets,
                budgetExcludedCategories = snapshot.exclusions,
                categories = snapshot.categories
            )

            return if (exportResult.isSuccess) {
                preferenceRepository.setLastBackupFingerprint(fingerprint)
                preferenceRepository.setLastBackupTime(System.currentTimeMillis())
                backupManager.pruneDownloads(BACKUP_SUBDIR, MAX_BACKUP_FILES)
                Result.success()
            } else {
                Result.retry()
            }
        }

        /** Immutable capture of everything a single backup run needs. */
        private data class BackupSnapshot(
            val expenses: List<Expense>,
            val budgets: List<MonthlyBudget>,
            val exclusions: List<BudgetExcludedCategory>,
            val categories: List<Category>
        ) {
            val isEmpty: Boolean
                get() = expenses.isEmpty() && budgets.isEmpty() && exclusions.isEmpty() && categories.isEmpty()
        }

        internal companion object {
            /** Subdirectory under the public Downloads folder that holds the JSON backups. */
            const val BACKUP_SUBDIR = "ExpenseBackup"

            /** Retention: number of most-recent backup files to keep on disk. */
            const val MAX_BACKUP_FILES = 7
        }
    }
