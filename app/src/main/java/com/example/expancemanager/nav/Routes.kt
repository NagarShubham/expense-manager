package com.example.expancemanager.nav

sealed interface AppRoute

data object HomeScreenRoute : AppRoute

data object AddExpenseRoute : AppRoute

data class EditExpenseRoute(
    val expenseId: Long
) : AppRoute

data class ExpenseDetailRoute(
    val expenseId: Long
) : AppRoute

data class CategoryExpensesRoute(
    val category: String,
    val month: Int,
    val year: Int
) : AppRoute

data class AllCategoriesRoute(
    val month: Int,
    val year: Int
) : AppRoute

data object SettingsRoute : AppRoute

data object BudgetSettingsRoute : AppRoute
