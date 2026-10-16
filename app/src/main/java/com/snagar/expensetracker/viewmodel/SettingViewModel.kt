package com.snagar.expensetracker.viewmodel

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snagar.expensetracker.backup.AutoBackupStore
import com.snagar.expensetracker.backup.BackupFileStore
import com.snagar.expensetracker.data.BudgetRepository
import com.snagar.expensetracker.data.CategoryRepository
import com.snagar.expensetracker.data.ExpenseRepository
import com.snagar.expensetracker.data.PreferenceRepository
import com.snagar.expensetracker.data.TransactionRunner
import com.snagar.expensetracker.util.BackupManager
import com.snagar.expensetracker.util.BackupManager.BackupSnapshot
import com.snagar.expensetracker.util.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
        private val biometricAuthenticator: BiometricAuthenticator,
        private val autoBackupStore: AutoBackupStore,
        private val backupFileStore: BackupFileStore
    ) : ViewModel() {
        internal val expenseCount: StateFlow<Int> = expenseRepository.getExpenseCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
        internal val isDarkTheme: StateFlow<Boolean> = preferenceRepository.isDarkTheme
        internal val isBiometricLockEnabled: StateFlow<Boolean> = preferenceRepository.isBiometricLockEnabled
        internal val isBiometricAvailable: Boolean = biometricAuthenticator.canAuthenticate()
        internal val isAutoBackupEnabled: StateFlow<Boolean> = autoBackupStore.isEnabled

        /** Where automatic backups land, e.g. `Download/Expense Tracker`. */
        internal val autoBackupLocation: String = BackupFileStore.DISPLAY_LOCATION

        /**
         * Exports expenses, budgets, and budget exclusions to a JSON file.
         * @param uri URI where to save the backup file
         * @return Result indicating success or failure
         */
        internal suspend fun exportData(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
            try {
                // Same consistent, single-transaction snapshot the auto-backup worker uses.
                val export = backupManager.loadSnapshot()

                if (export.isEmpty) {
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
        ): Result<BackupSnapshot> =
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

        /** True when auto-backup needs the legacy storage grant (API 26-28 only). */
        internal fun isStoragePermissionNeeded(): Boolean = !backupFileStore.hasLegacyWritePermission()

        /** Turns auto-backup on/off; see [AutoBackupStore.setEnabled]. */
        internal fun setAutoBackupEnabled(enabled: Boolean) {
            autoBackupStore.setEnabled(enabled)
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
