package com.example.expancemanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ExpenseDao {
    @Query("SELECT COUNT(*) FROM expenses")
    fun getExpenseCount(): Flow<Int>

    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :startDate AND date <= :endDate")
    fun getTotalAmountByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<Double?>

    @Query("SELECT category, SUM(amount) as total FROM expenses WHERE date >= :startDate AND date <= :endDate GROUP BY category ORDER BY total DESC")
    fun getCategoryTotalsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryTotal>>

    /**
     * Per-month sums for [startDate]..[endDate]. [MonthlyTotal.month] is 0-based (Calendar).
     */
    @Query(
        """
        SELECT CAST(strftime('%m', date / 1000, 'unixepoch', 'localtime') AS INTEGER) - 1 AS month,
               CAST(strftime('%Y', date / 1000, 'unixepoch', 'localtime') AS INTEGER) AS year,
               SUM(amount) AS total
        FROM expenses
        WHERE date >= :startDate AND date <= :endDate
        GROUP BY year, month
        ORDER BY year ASC, month ASC
        """
    )
    fun getMonthlyTotalsByDateRange(
        startDate: Long,
        endDate: Long
    ): Flow<List<MonthlyTotal>>

    @Query("SELECT COUNT(*) FROM expenses WHERE category = :name")
    suspend fun countExpensesInCategory(name: String): Int

    @Query("UPDATE expenses SET category = :newName WHERE category = :oldName")
    suspend fun renameCategory(
        oldName: String,
        newName: String
    )

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    suspend fun getAllExpensesForExport(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}
