package com.example.expancemanager.data

import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for budget-related data. Wraps [MonthlyBudgetDao] and
 * [BudgetExcludedCategoryDao] to provide a consistent data layer and simplify ViewModels.
 */
class BudgetRepository(
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val budgetExcludedCategoryDao: BudgetExcludedCategoryDao
) {
    fun getBudgetByMonthYear(
        month: Int,
        year: Int
    ): Flow<MonthlyBudget?> = monthlyBudgetDao.getBudgetByMonthYear(month, year)

    suspend fun getBudgetByMonthYearOnce(
        month: Int,
        year: Int
    ): MonthlyBudget? = monthlyBudgetDao.getBudgetByMonthYearOnce(month, year)

    suspend fun insertOrUpdateBudget(budget: MonthlyBudget) = monthlyBudgetDao.insertOrUpdateBudget(budget)

    suspend fun deleteBudgetByMonthYear(
        month: Int,
        year: Int
    ) = monthlyBudgetDao.deleteByMonthYear(month, year)

    fun getExcludedCategoriesByMonthYear(
        month: Int,
        year: Int
    ): Flow<List<String>> = budgetExcludedCategoryDao.getExcludedByMonthYear(month, year)

    suspend fun setCategoryExcluded(
        month: Int,
        year: Int,
        category: String,
        excluded: Boolean
    ) {
        if (excluded) {
            budgetExcludedCategoryDao.insertExcluded(
                BudgetExcludedCategory(month = month, year = year, category = category)
            )
        } else {
            budgetExcludedCategoryDao.removeExcluded(month, year, category)
        }
    }
}
