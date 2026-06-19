package com.example.expancemanager.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.BudgetRepository
import com.example.expancemanager.data.ExpenseRepository
import com.example.expancemanager.data.MonthlyBudget
import com.example.expancemanager.data.PreferenceRepository
import com.example.expancemanager.util.BackupManager
import com.example.expancemanager.util.BackupManager.BackupImportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val backupManager: BackupManager,
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {
    private val _expenseCount = MutableStateFlow(0)
    val expenseCount: StateFlow<Int> = _expenseCount.asStateFlow()
    val isDarkTheme: StateFlow<Boolean> = preferenceRepository.isDarkTheme

    init {
        viewModelScope.launch(Dispatchers.IO) {
            expenseRepository.getExpenseCount().collect { _expenseCount.value = it }
        }
    }

    /**
     * Exports expenses, budgets, and budget exclusions to a JSON file.
     * @param uri URI where to save the backup file
     * @return Result indicating success or failure
     */
    suspend fun exportData(uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val (expenses, monthlyBudgets, budgetExcludedCategories) = coroutineScope {
                    val expensesDeferred = async { expenseRepository.getAllExpensesForExport() }
                    val budgetsDeferred = async { budgetRepository.getAllBudgetsForExport() }
                    val exclusionsDeferred = async { budgetRepository.getAllExcludedCategoriesForExport() }
                    Triple(expensesDeferred.await(), budgetsDeferred.await(), exclusionsDeferred.await())
                }

                if (expenses.isEmpty() && monthlyBudgets.isEmpty() && budgetExcludedCategories.isEmpty()) {
                    return@withContext Result.failure(Exception("No data to export"))
                }

                backupManager.exportToJson(uri, expenses, monthlyBudgets, budgetExcludedCategories)
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
    suspend fun importData(
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

                if (replaceExisting) {
                    expenseRepository.deleteAllExpenses()
                    budgetRepository.deleteAllBudgetData()
                }

                val expensesToInsert =
                    if (imported.expenses.isEmpty()) {
                        emptyList()
                    } else {
                        imported.expenses.map { it.copy(id = 0) }
                    }
                if (expensesToInsert.isNotEmpty()) {
                    expenseRepository.insertExpenses(expensesToInsert)
                }
                budgetRepository.insertBudgets(imported.monthlyBudgets)
                budgetRepository.insertExcludedCategories(imported.budgetExcludedCategories)

                Result.success(imported.copy(expenses = expensesToInsert))
            } catch (e: Exception) {
                Result.failure(Exception("Import failed: ${e.message}"))
            }
        }

    /** Set or update expected monthly expense (budget) for the given month/year. */
    fun setMonthlyBudget(
        month: Int,
        year: Int,
        expectedAmount: Double
    ) {
        viewModelScope.launch { insertBudget(month, year, expectedAmount) }
    }

    /** Returns current expected budget for the given month/year, or null if not set. */
    suspend fun getExpectedBudgetForMonth(
        month: Int,
        year: Int
    ): Double? =
        withContext(Dispatchers.IO) {
            budgetRepository.getBudgetByMonthYearOnce(month, year)?.expectedAmount
        }

    /** Set monthly budget and wait for the write to complete (e.g. before navigating back). */
    suspend fun setMonthlyBudgetAndWait(
        month: Int,
        year: Int,
        expectedAmount: Double
    ) = insertBudget(month, year, expectedAmount)

    /** Removes the budget for the given month/year. */
    fun clearMonthlyBudget(
        month: Int,
        year: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetRepository.deleteBudgetByMonthYear(month, year)
        }
    }

    /** Sets whether a category is excluded from the monthly budget for the given month/year. Excluded categories don't count toward Used/Remaining/Progress. */
    fun setCategoryExcludedFromBudget(
        month: Int,
        year: Int,
        category: String,
        excluded: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            budgetRepository.setCategoryExcluded(month, year, category, excluded)
        }
    }

    /** Flow of category names excluded from budget for the given month/year. Used by Budget Settings screen. */
    fun getExcludedByMonthYear(
        month: Int,
        year: Int
    ): Flow<List<String>> = budgetRepository.getExcludedCategoriesByMonthYear(month, year)

    private suspend fun insertBudget(
        month: Int,
        year: Int,
        expectedAmount: Double
    ) = withContext(Dispatchers.IO) {
        budgetRepository.insertOrUpdateBudget(
            MonthlyBudget(month = month, year = year, expectedAmount = expectedAmount)
        )
    }

    fun generateBackupFileName() = backupManager.generateBackupFileName()

    fun setDarkTheme(enabled: Boolean) {
        preferenceRepository.setDarkTheme(enabled)
    }
}
