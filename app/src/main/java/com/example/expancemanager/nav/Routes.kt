package com.example.expancemanager.nav

data object HomeScreenRoute

data object AddExpenseRoute

data class EditExpenseRoute(
    val expenseId: Long
)

data class ExpenseDetailRoute(
    val expenseId: Long
)

data class CategoryExpensesRoute(
    val category: String,
    val month: Int,
    val year: Int
)

data class AllCategoriesRoute(
    val month: Int,
    val year: Int
)

data object SettingsRoute
