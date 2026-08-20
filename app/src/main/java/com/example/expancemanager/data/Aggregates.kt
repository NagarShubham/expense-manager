package com.example.expancemanager.data

internal data class CategoryTotal(
    val category: String,
    val total: Double
)

internal data class MonthlyTotal(
    val month: Int,
    val year: Int,
    val total: Double
)
