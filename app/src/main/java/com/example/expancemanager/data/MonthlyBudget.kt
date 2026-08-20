package com.example.expancemanager.data

import androidx.room.Entity

/**
 * Expected monthly expense (budget) for a given month and year.
 * One record per month; used to compare with actual total spent.
 */
@Entity(
    tableName = "monthly_budgets",
    primaryKeys = ["month", "year"]
)
internal data class MonthlyBudget(
    val month: Int,
    val year: Int,
    val expectedAmount: Double
)
