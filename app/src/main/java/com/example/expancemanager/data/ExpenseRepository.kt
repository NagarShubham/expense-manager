package com.example.expancemanager.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Single entry point for expense data. Wraps [ExpenseDao] to simplify ViewModels
 * and allow easier testing and reuse of data logic.
 *
 * Room re-emits a table-scoped Flow on any write to that table. The aggregate flows
 * (count, total) are wrapped in [distinctUntilChanged] so writes that don't change the
 * derived value (e.g. editing an expense's title) don't ripple downstream recompositions.
 */
internal class ExpenseRepository(
    private val expenseDao: ExpenseDao
) {
    internal fun getExpenseCount(): Flow<Int> = expenseDao.getExpenseCount().distinctUntilChanged()

    internal fun getExpensesByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate).distinctUntilChanged()

    internal suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    internal suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    internal suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    internal suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    internal fun getTotalAmountByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<Double?> = expenseDao.getTotalAmountByDateRange(startDate, endDate).distinctUntilChanged()

    internal fun getCategoryTotalsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryTotal>> =
        expenseDao.getCategoryTotalsByDateRange(startDate, endDate).distinctUntilChanged()

    internal fun getMonthlyTotalsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<MonthlyTotal>> =
        expenseDao.getMonthlyTotalsByDateRange(startDate, endDate).distinctUntilChanged()

    /**
     * Runs [filter] against the whole history. Translating the filter into DAO
     * arguments lives here so the ViewModel never deals with the sentinel values the
     * single-statement query needs (`ignoreCategories`, the sort ordinal).
     *
     * Room cannot bind an empty list to `IN ()`, so [ExpenseFilter.categories] being
     * empty is signalled by `ignoreCategories = 1` with a one-element placeholder list
     * that the disabled clause never evaluates.
     */
    internal fun searchExpenses(
        filter: ExpenseFilter,
        limit: Int = DEFAULT_SEARCH_LIMIT
    ): Flow<List<Expense>> {
        val hasCategoryFilter = filter.categories.isNotEmpty()
        return expenseDao
            .searchExpenses(
                query = filter.query.trim(),
                categories = if (hasCategoryFilter) filter.categories.toList() else listOf(""),
                ignoreCategories = if (hasCategoryFilter) 0 else 1,
                minAmount = filter.minAmount,
                maxAmount = filter.maxAmount,
                startDate = filter.startDate,
                endDate = filter.endDate,
                sortOrder = filter.sortOrder.ordinal,
                limit = limit
            )
            .distinctUntilChanged()
    }

    internal suspend fun getAllExpensesForExport(): List<Expense> = expenseDao.getAllExpensesForExport()

    internal suspend fun insertExpenses(expenses: List<Expense>) = expenseDao.insertExpenses(expenses)

    internal suspend fun deleteAllExpenses() = expenseDao.deleteAllExpenses()

    internal companion object {
        /**
         * Caps search results so a very broad filter over years of history can't stall
         * the list. The UI reports when results are truncated rather than silently
         * showing a partial answer.
         */
        const val DEFAULT_SEARCH_LIMIT = 500
    }
}
