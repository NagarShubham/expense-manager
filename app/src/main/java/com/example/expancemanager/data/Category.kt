package com.example.expancemanager.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-manageable expense category.
 *
 * The [name] is the stable key: it is what gets stored in [Expense.category] and
 * [BudgetExcludedCategory.category], so renames must cascade to those tables (see
 * [CategoryRepository.updateCategory]). [emoji] is shown throughout the UI and
 * [sortOrder] controls the display order the user arranges.
 */
@Entity(tableName = "categories")
internal data class Category(
    @PrimaryKey val name: String,
    val emoji: String,
    val sortOrder: Int
)
