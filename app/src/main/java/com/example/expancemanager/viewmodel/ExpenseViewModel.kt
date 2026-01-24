package com.example.expancemanager.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.data.Expense
import com.example.expancemanager.data.ExpenseDao
import com.example.expancemanager.data.ExpenseDatabase
import com.example.expancemanager.util.BackupManager
import com.example.expancemanager.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val totalAmount: Double = 0.0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR)
)

class ExpenseViewModel(
    application: Application
) : AndroidViewModel(application) {
    // Direct DAO access - removed unnecessary Repository layer
    private val expenseDao: ExpenseDao = ExpenseDatabase.getDatabase(application).expenseDao()
    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    init {
        loadExpensesForCurrentMonth()
    }

    private fun loadExpensesForCurrentMonth() {
        val currentState = _uiState.value
        loadExpensesForMonth(currentState.selectedMonth, currentState.selectedYear)
    }

    fun loadExpensesForMonth(
        month: Int,
        year: Int
    ) {
        _uiState.update { it.copy(selectedMonth = month, selectedYear = year) }

        val (startDate, endDate) = DateUtils.getMonthDateRange(month, year)

        viewModelScope.launch {
            combine(
                expenseDao.getExpensesByDateRange(startDate, endDate),
                expenseDao.getTotalAmountByDateRange(startDate, endDate),
                expenseDao.getCategoryTotalsByDateRange(startDate, endDate)
            ) { expenses, total, categoryTotals ->
                ExpenseUiState(
                    expenses = expenses,
                    totalAmount = total ?: 0.0,
                    categoryTotals = categoryTotals,
                    selectedMonth = month,
                    selectedYear = year
                )
            }.collect { _uiState.value = it }
        }
    }

    fun insertExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseDao.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseDao.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch(Dispatchers.IO) {
            expenseDao.deleteExpense(expense)
        }
    }

    suspend fun getExpenseById(id: Long): Expense? =
        withContext(Dispatchers.IO) {
            expenseDao.getExpenseById(id)
        }

    fun changeMonth(increment: Int) {
        val calendar = Calendar.getInstance().apply {
            val (month, year) = _uiState.value.run { Pair(selectedMonth, selectedYear) }
            set(year, month, 1)
            add(Calendar.MONTH, increment)
        }
        loadExpensesForMonth(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
    }

    /**
     * Exports all expenses to a JSON file
     * @param uri URI where to save the backup file
     * @return Result indicating success or failure
     */
    suspend fun exportData(uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val expenses = expenseDao.getAllExpensesForExport()
                if (expenses.isEmpty()) {
                    return@withContext Result.failure(Exception("No expenses to export"))
                }
                BackupManager.exportToJson(getApplication(), uri, expenses)
            } catch (e: Exception) {
                Result.failure(Exception("Export failed: ${e.message}"))
            }
        }

    /**
     * Imports expenses from a JSON file
     * @param uri URI of the backup file to import
     * @param replaceExisting If true, deletes existing data before import
     * @return Result indicating success or failure with count of imported expenses
     */
    suspend fun importData(
        uri: Uri,
        replaceExisting: Boolean = false
    ): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val result = BackupManager.importFromJson(getApplication(), uri)

                if (result.isFailure) {
                    return@withContext Result.failure(result.exceptionOrNull()!!)
                }

                val importedExpenses = result.getOrNull()!!

                if (replaceExisting) {
                    expenseDao.deleteAllExpenses()
                }

                // Remove auto-generated IDs to avoid conflicts
                val expensesToInsert = importedExpenses.map { it.copy(id = 0) }
                expenseDao.insertExpenses(expensesToInsert)

                // Reload current month's data
                loadExpensesForCurrentMonth()

                Result.success(expensesToInsert.size)
            } catch (e: Exception) {
                Result.failure(Exception("Import failed: ${e.message}"))
            }
        }
}
