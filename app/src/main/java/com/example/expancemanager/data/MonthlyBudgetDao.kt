package com.example.expancemanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlyBudgetDao {
    @Query("SELECT * FROM monthly_budgets WHERE month = :month AND year = :year LIMIT 1")
    fun getBudgetByMonthYear(
        month: Int,
        year: Int
    ): Flow<MonthlyBudget?>

    @Query("SELECT * FROM monthly_budgets WHERE month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetByMonthYearOnce(
        month: Int,
        year: Int
    ): MonthlyBudget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: MonthlyBudget)

    @Query("DELETE FROM monthly_budgets WHERE month = :month AND year = :year")
    suspend fun deleteByMonthYear(
        month: Int,
        year: Int
    )

    @Query("SELECT * FROM monthly_budgets")
    suspend fun getAllBudgets(): List<MonthlyBudget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<MonthlyBudget>)

    @Query("DELETE FROM monthly_budgets")
    suspend fun deleteAllBudgets()
}
