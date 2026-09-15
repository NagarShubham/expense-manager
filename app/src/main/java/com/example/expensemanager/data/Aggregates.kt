package com.example.expensemanager.data

import androidx.compose.runtime.Immutable

@Immutable
internal data class CategoryTotal(
    val category: String,
    val total: Double
)

@Immutable
internal data class MonthlyTotal(
    val month: Int,
    val year: Int,
    val total: Double
)
