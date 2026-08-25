package com.example.expancemanager.nav

internal sealed interface AppRoute

internal data object HomeScreenRoute : AppRoute

internal data object AddExpenseRoute : AppRoute

internal data class EditExpenseRoute(
    val expenseId: Long
) : AppRoute

internal data class ExpenseDetailRoute(
    val expenseId: Long
) : AppRoute

internal data class CategoryExpensesRoute(
    val category: String,
    val month: Int,
    val year: Int
) : AppRoute

internal data class AllCategoriesRoute(
    val month: Int,
    val year: Int
) : AppRoute

internal data object SearchRoute : AppRoute

internal data object ReportsRoute : AppRoute

internal data object SettingsRoute : AppRoute

internal data object BudgetSettingsRoute : AppRoute

internal data object ManageCategoriesRoute : AppRoute
