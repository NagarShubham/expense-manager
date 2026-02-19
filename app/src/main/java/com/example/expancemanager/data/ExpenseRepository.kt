package com.example.expancemanager.data

import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for expense data. Wraps [ExpenseDao] to simplify ViewModels
 * and allow easier testing and reuse of data logic.
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {
    fun getExpenseCount(): Flow<Int> = expenseDao.getExpenseCount()

    fun getExpensesByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<Expense>> = expenseDao.getExpensesByDateRange(startDate, endDate)

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    fun getTotalAmountByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<Double?> = expenseDao.getTotalAmountByDateRange(startDate, endDate)

    fun getCategoryTotalsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryTotal>> = expenseDao.getCategoryTotalsByDateRange(startDate, endDate)

    suspend fun getAllExpensesForExport(): List<Expense> = expenseDao.getAllExpensesForExport()

    suspend fun insertExpenses(expenses: List<Expense>) = expenseDao.insertExpenses(expenses)

    suspend fun deleteAllExpenses() = expenseDao.deleteAllExpenses()
}
