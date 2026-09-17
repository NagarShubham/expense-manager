package com.example.expensemanager.data

import androidx.compose.runtime.Immutable
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
@Immutable
@Entity(tableName = "categories")
internal data class Category(
    @PrimaryKey val name: String,
    val emoji: String,
    val sortOrder: Int
)

/**
 * Name -> emoji lookup, the shape every UI surface wants when resolving a category's
 * emoji via [ExpenseCategories.getCategoryEmoji]. Defined once here so the four
 * screens/states that need it don't each re-derive the same association.
 */
internal fun List<Category>.toEmojiMap(): Map<String, String> = associate { it.name to it.emoji }
