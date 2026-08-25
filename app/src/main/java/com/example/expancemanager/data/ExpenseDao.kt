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

    /**
     * History-wide search. Every filter is optional: a null (or blank, for [query])
     * parameter disables that clause, so one prepared statement serves every filter
     * combination the UI can build.
     *
     * [sortOrder] is an ordinal from [ExpenseSortOrder] rather than a string spliced into
     * the SQL — this keeps the query compile-time verified and injection-proof.
     */
    @Query(
        """
        SELECT * FROM expenses
        WHERE (:query = '' OR title LIKE '%' || :query || '%' COLLATE NOCASE
                          OR description LIKE '%' || :query || '%' COLLATE NOCASE)
          AND (:ignoreCategories = 1 OR category IN (:categories))
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
        ORDER BY
          CASE WHEN :sortOrder = 0 THEN date END DESC,
          CASE WHEN :sortOrder = 1 THEN date END ASC,
          CASE WHEN :sortOrder = 2 THEN amount END DESC,
          CASE WHEN :sortOrder = 3 THEN amount END ASC,
          id DESC
        LIMIT :limit
        """
    )
    fun searchExpenses(
        query: String,
        categories: List<String>,
        ignoreCategories: Int,
        minAmount: Double?,
        maxAmount: Double?,
        startDate: Long?,
        endDate: Long?,
        sortOrder: Int,
        limit: Int
    ): Flow<List<Expense>>

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
