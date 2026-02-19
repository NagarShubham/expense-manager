package com.example.expancemanager.data

import androidx.room.Entity

/**
 * Categories excluded from the monthly budget for a specific month/year.
 * Expenses in these categories still appear in lists and breakdowns
 * but are excluded from "Used" vs "Expected" and progress for that month.
 */
@Entity(
    tableName = "budget_excluded_categories",
    primaryKeys = ["month", "year", "category"]
)
data class BudgetExcludedCategory(
    val month: Int,
    val year: Int,
    val category: String
)
