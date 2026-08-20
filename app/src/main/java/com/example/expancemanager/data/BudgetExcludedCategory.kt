package com.example.expancemanager.data

import androidx.room.Entity
import androidx.room.Index

/**
 * Categories excluded from the monthly budget for a specific month/year.
 * Expenses in these categories still appear in lists and breakdowns
 * but are excluded from "Used" vs "Expected" and progress for that month.
 *
 * Indexed on [category] so the category-rename cascade (UPDATE ... WHERE category = ?)
 * doesn't full-scan; the composite PK's leftmost column is `month`, not `category`.
 */
@Entity(
    tableName = "budget_excluded_categories",
    primaryKeys = ["month", "year", "category"],
    indices = [Index(value = ["category"])]
)
internal data class BudgetExcludedCategory(
    val month: Int,
    val year: Int,
    val category: String
)
