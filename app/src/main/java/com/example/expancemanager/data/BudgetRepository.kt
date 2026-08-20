package com.example.expancemanager.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Single entry point for budget-related data. Wraps [MonthlyBudgetDao] and
 * [BudgetExcludedCategoryDao] to provide a consistent data layer and simplify ViewModels.
 */
internal class BudgetRepository(
    private val monthlyBudgetDao: MonthlyBudgetDao,
    private val budgetExcludedCategoryDao: BudgetExcludedCategoryDao
) {
    internal fun getBudgetByMonthYear(
        month: Int,
        year: Int
    ): Flow<MonthlyBudget?> =
        monthlyBudgetDao.getBudgetByMonthYear(month, year).distinctUntilChanged()

    internal suspend fun getBudgetByMonthYearOnce(
        month: Int,
        year: Int
    ): MonthlyBudget? = monthlyBudgetDao.getBudgetByMonthYearOnce(month, year)

    internal suspend fun insertOrUpdateBudget(budget: MonthlyBudget) = monthlyBudgetDao.insertOrUpdateBudget(budget)

    internal suspend fun deleteBudgetByMonthYear(
        month: Int,
        year: Int
    ) = monthlyBudgetDao.deleteByMonthYear(month, year)

    internal fun getExcludedCategoriesByMonthYear(
        month: Int,
        year: Int
    ): Flow<List<String>> =
        budgetExcludedCategoryDao.getExcludedByMonthYear(month, year).distinctUntilChanged()

    internal suspend fun setCategoryExcluded(
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

    internal suspend fun getAllBudgetsForExport(): List<MonthlyBudget> = monthlyBudgetDao.getAllBudgets()

    internal suspend fun getAllExcludedCategoriesForExport(): List<BudgetExcludedCategory> =
        budgetExcludedCategoryDao.getAllExcluded()

    internal suspend fun insertBudgets(budgets: List<MonthlyBudget>) {
        if (budgets.isNotEmpty()) {
            monthlyBudgetDao.insertBudgets(budgets)
        }
    }

    internal suspend fun insertExcludedCategories(categories: List<BudgetExcludedCategory>) {
        if (categories.isNotEmpty()) {
            budgetExcludedCategoryDao.insertExcludedList(categories)
        }
    }

    internal suspend fun deleteAllBudgetData() {
        monthlyBudgetDao.deleteAllBudgets()
        budgetExcludedCategoryDao.deleteAllExcluded()
    }
}
