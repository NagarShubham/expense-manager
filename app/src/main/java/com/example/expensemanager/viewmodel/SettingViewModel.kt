package com.example.expensemanager.viewmodel

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensemanager.backup.AutoBackupStore
import com.example.expensemanager.backup.AutoBackupWorker
import com.example.expensemanager.backup.BackupFileStore
import com.example.expensemanager.data.BudgetExcludedCategory
import com.example.expensemanager.data.BudgetRepository
import com.example.expensemanager.data.Category
import com.example.expensemanager.data.CategoryRepository
import com.example.expensemanager.data.Expense
import com.example.expensemanager.data.ExpenseRepository
import com.example.expensemanager.data.MonthlyBudget
import com.example.expensemanager.data.PreferenceRepository
import com.example.expensemanager.data.TransactionRunner
import com.example.expensemanager.util.BackupManager
import com.example.expensemanager.util.BackupManager.BackupImportResult
import com.example.expensemanager.util.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class SettingViewModel
    @Inject
    constructor(
        private val expenseRepository: ExpenseRepository,
        private val budgetRepository: BudgetRepository,
        private val categoryRepository: CategoryRepository,
        private val backupManager: BackupManager,
        private val preferenceRepository: PreferenceRepository,
        private val transactionRunner: TransactionRunner,
        private val biometricAuthenticator: BiometricAuthenticator,
        private val autoBackupStore: AutoBackupStore,
        private val backupFileStore: BackupFileStore,
        // Application context only: needed to reach WorkManager, never retained as a View
        // or Activity reference.
        @ApplicationContext private val appContext: Context
    ) : ViewModel() {
        internal val expenseCount: StateFlow<Int> = expenseRepository.getExpenseCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
        internal val isDarkTheme: StateFlow<Boolean> = preferenceRepository.isDarkTheme
        internal val isBiometricLockEnabled: StateFlow<Boolean> = preferenceRepository.isBiometricLockEnabled
        internal val isBiometricAvailable: Boolean = biometricAuthenticator.canAuthenticate()
        internal val isAutoBackupEnabled: StateFlow<Boolean> = autoBackupStore.isEnabled

        /** Process-lifetime scope, so enqueueing survives leaving the Settings screen. */
        private val autoBackupScope = autoBackupStore.backgroundScope

        /** Where automatic backups land, e.g. `Download/Expense Manager`. */
        internal val autoBackupLocation: String = BackupFileStore.DISPLAY_LOCATION

        /** Aggregates everything gathered for an export before handing it to [BackupManager]. */
        private data class ExportData(
            val expenses: List<Expense>,
            val monthlyBudgets: List<MonthlyBudget>,
            val budgetExcludedCategories: List<BudgetExcludedCategory>,
            val categories: List<Category>
        )

        /**
         * Exports expenses, budgets, and budget exclusions to a JSON file.
         * @param uri URI where to save the backup file
         * @return Result indicating success or failure
         */
        internal suspend fun exportData(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
            try {
                val export = coroutineScope {
                    val expensesDeferred = async { expenseRepository.getAllExpensesForExport() }
                    val budgetsDeferred = async { budgetRepository.getAllBudgetsForExport() }
                    val exclusionsDeferred = async { budgetRepository.getAllExcludedCategoriesForExport() }
                    val categoriesDeferred = async { categoryRepository.getAllForExport() }
                    ExportData(
                        expenses = expensesDeferred.await(),
                        monthlyBudgets = budgetsDeferred.await(),
                        budgetExcludedCategories = exclusionsDeferred.await(),
                        categories = categoriesDeferred.await()
                    )
                }

                if (export.expenses.isEmpty() &&
                    export.monthlyBudgets.isEmpty() &&
                    export.budgetExcludedCategories.isEmpty() &&
                    export.categories.isEmpty()
                ) {
                    return@withContext Result.failure(Exception("No data to export"))
                }

                backupManager.exportToJson(
                    uri,
                    export.expenses,
                    export.monthlyBudgets,
                    export.budgetExcludedCategories,
                    export.categories
                )
            } catch (e: Exception) {
                Result.failure(Exception("Export failed: ${e.message}"))
            }
        }

        /**
         * Imports expenses, budgets, and budget exclusions from a JSON file.
         * Supports v1 backups (expenses only) and v2 backups (full data).
         * @param uri URI of the backup file to import
         * @param replaceExisting If true, deletes existing data before import
         * @return Result with counts of imported records per type
         */
        internal suspend fun importData(
            uri: Uri,
            replaceExisting: Boolean = false
        ): Result<BackupImportResult> =
            withContext(Dispatchers.IO) {
                try {
                    val importResult = backupManager.importFromJson(uri)
                    val imported = importResult.getOrNull()
                        ?: return@withContext Result.failure(
                            importResult.exceptionOrNull() ?: Exception("Import failed")
                        )

                    val expensesToInsert =
                        if (imported.expenses.isEmpty()) {
                            emptyList()
                        } else {
                            imported.expenses.map { it.copy(id = 0) }
                        }

                    // Wipe + insert run in one transaction so a mid-restore failure rolls back
                    // to the pre-import state instead of leaving data half-deleted/half-written.
                    transactionRunner {
                        if (replaceExisting) {
                            expenseRepository.deleteAllExpenses()
                            budgetRepository.deleteAllBudgetData()
                            // Only wipe categories when the backup actually carries them; otherwise
                            // an older (v1/v2) backup would leave the user with zero categories.
                            if (imported.categories.isNotEmpty()) {
                                categoryRepository.deleteAllCategories()
                            }
                        }

                        if (expensesToInsert.isNotEmpty()) {
                            expenseRepository.insertExpenses(expensesToInsert)
                        }
                        budgetRepository.insertBudgets(imported.monthlyBudgets)
                        budgetRepository.insertExcludedCategories(imported.budgetExcludedCategories)
                        // REPLACE-conflict insert merges/updates categories by name.
                        categoryRepository.insertCategories(imported.categories)
                    }

                    Result.success(imported.copy(expenses = expensesToInsert))
                } catch (e: Exception) {
                    Result.failure(Exception("Import failed: ${e.message}"))
                }
            }

        internal fun generateBackupFileName() = backupManager.generateBackupFileName()

        /**
         * True when enabling automatic backup still needs the legacy storage grant. Always
         * false on API 29+, where MediaStore writes need no permission at all.
         */
        internal fun isStoragePermissionNeeded(): Boolean = !backupFileStore.hasLegacyWritePermission()

        /**
         * Turns automatic backup on/off and syncs the WorkManager schedule. Enabling resets
         * change tracking and kicks off one immediate run, so the user isn't waiting up to a day.
         *
         * No "already in that state" guard on purpose: a recomposition may call this twice with
         * the same value, and an early return would skip scheduling. Re-running is safe —
         * `setEnabled` no-ops on an unchanged value and the WorkManager calls are idempotent.
         *
         * On [autoBackupScope], not `viewModelScope`: leaving Settings clears the ViewModel and
         * would cancel the enqueue before the schedule is created.
         */
        internal fun setAutoBackupEnabled(enabled: Boolean) {
            autoBackupStore.setEnabled(enabled)
            if (enabled) {
                autoBackupStore.resetChangeTracking()
            }
            // WorkManager calls touch its own database, so they stay off the main thread.
            autoBackupScope.launch(Dispatchers.Default) {
                AutoBackupWorker.sync(appContext, enabled)
                if (enabled) {
                    AutoBackupWorker.runNow(appContext)
                }
            }
        }

        internal fun setDarkTheme(enabled: Boolean) {
            preferenceRepository.setDarkTheme(enabled)
        }

        internal fun disableBiometricLock() {
            preferenceRepository.setBiometricLockEnabled(false)
        }

        internal fun requestEnableBiometricLock(
            activity: ComponentActivity,
            onEnabled: () -> Unit,
            onFailed: (String) -> Unit
        ) {
            biometricAuthenticator.authenticate(
                activity = activity,
                onSuccess = {
                    preferenceRepository.setBiometricLockEnabled(true)
                    onEnabled()
                },
                onError = onFailed
            )
        }
    }
