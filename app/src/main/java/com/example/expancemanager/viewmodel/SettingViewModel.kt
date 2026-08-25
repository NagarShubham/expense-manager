package com.example.expancemanager.viewmodel

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.BudgetExcludedCategory
import com.example.expancemanager.data.BudgetRepository
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.CategoryRepository
import com.example.expancemanager.data.Expense
import com.example.expancemanager.data.ExpenseRepository
import com.example.expancemanager.data.MonthlyBudget
import com.example.expancemanager.data.PreferenceRepository
import com.example.expancemanager.data.TransactionRunner
import com.example.expancemanager.util.BackupManager
import com.example.expancemanager.util.BackupManager.BackupImportResult
import com.example.expancemanager.util.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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
        private val biometricAuthenticator: BiometricAuthenticator
    ) : ViewModel() {
        internal val expenseCount: StateFlow<Int> = expenseRepository
            .getExpenseCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
        internal val isDarkTheme: StateFlow<Boolean> = preferenceRepository.isDarkTheme
        internal val isBiometricLockEnabled: StateFlow<Boolean> = preferenceRepository.isBiometricLockEnabled
        internal val isBiometricAvailable: Boolean = biometricAuthenticator.canAuthenticate()

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
        internal suspend fun exportData(uri: Uri): Result<String> =
            withContext(Dispatchers.IO) {
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
