package com.example.expancemanager.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetExcludedCategoryDao {
    @Query("SELECT category FROM budget_excluded_categories WHERE month = :month AND year = :year ORDER BY category")
    fun getExcludedByMonthYear(
        month: Int,
        year: Int
    ): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExcluded(entity: BudgetExcludedCategory)

    @Query("DELETE FROM budget_excluded_categories WHERE month = :month AND year = :year AND category = :category")
    suspend fun removeExcluded(
        month: Int,
        year: Int,
        category: String
    )

    @Query("UPDATE budget_excluded_categories SET category = :newName WHERE category = :oldName")
    suspend fun renameExcludedCategory(
        oldName: String,
        newName: String
    )

    @Query("DELETE FROM budget_excluded_categories WHERE category = :category")
    suspend fun removeExcludedByCategory(category: String)

    @Query("SELECT * FROM budget_excluded_categories")
    suspend fun getAllExcluded(): List<BudgetExcludedCategory>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExcludedList(entities: List<BudgetExcludedCategory>)

    @Query("DELETE FROM budget_excluded_categories")
    suspend fun deleteAllExcluded()
}
