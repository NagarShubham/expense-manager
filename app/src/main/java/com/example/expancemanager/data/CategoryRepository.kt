package com.example.expancemanager.data

import kotlinx.coroutines.flow.Flow

/**
 * Single entry point for category data. Wraps [CategoryDao] and, for renames,
 * coordinates cascading updates to [ExpenseDao] and [BudgetExcludedCategoryDao]
 * inside a single transaction so the category name stays consistent everywhere.
 *
 * Multi-table writes run through [transactionRunner] rather than calling Room's
 * `withTransaction` directly, which keeps this class decoupled from Room and unit-testable.
 */
internal class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao,
    private val budgetExcludedCategoryDao: BudgetExcludedCategoryDao,
    private val transactionRunner: TransactionRunner
) {
    /** Outcome of an add/update/delete so the UI can show a specific message. */
    sealed interface CategoryResult {
        data object Success : CategoryResult

        data object BlankName : CategoryResult

        data object DuplicateName : CategoryResult

        data object InUse : CategoryResult
    }

    internal fun getCategories(): Flow<List<Category>> = categoryDao.getAllOrdered()

    internal suspend fun addCategory(
        name: String,
        emoji: String
    ): CategoryResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return CategoryResult.BlankName
        if (categoryDao.countByName(trimmed) > 0) return CategoryResult.DuplicateName

        val nextSortOrder = (categoryDao.getMaxSortOrder() ?: -1) + 1
        categoryDao.insert(Category(name = trimmed, emoji = emoji, sortOrder = nextSortOrder))
        return CategoryResult.Success
    }

    /**
     * Updates a category's name and/or emoji. If the name changes, the primary-key
     * row is replaced and the change cascades to expenses and budget exclusions,
     * all in one transaction.
     */
    internal suspend fun updateCategory(
        oldName: String,
        newName: String,
        emoji: String
    ): CategoryResult {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return CategoryResult.BlankName

        val nameChanged = trimmed != oldName
        // Result is set inside the transaction so the duplicate check and the write are
        // atomic — a racing insert of `trimmed` can't slip between check and REPLACE.
        var result: CategoryResult = CategoryResult.Success
        transactionRunner {
            if (nameChanged && categoryDao.countByName(trimmed) > 0) {
                result = CategoryResult.DuplicateName
                return@transactionRunner
            }

            val existing = categoryDao.getAllOrderedOnce().firstOrNull { it.name == oldName }
            val sortOrder = existing?.sortOrder ?: ((categoryDao.getMaxSortOrder() ?: -1) + 1)

            if (nameChanged) {
                // Re-key the category, then cascade the name to referencing tables.
                categoryDao.deleteByName(oldName)
                categoryDao.insert(Category(name = trimmed, emoji = emoji, sortOrder = sortOrder))
                expenseDao.renameCategory(oldName, trimmed)
                budgetExcludedCategoryDao.renameExcludedCategory(oldName, trimmed)
            } else {
                categoryDao.update(Category(name = trimmed, emoji = emoji, sortOrder = sortOrder))
            }
        }
        return result
    }

    /**
     * Deletes a category, unless expenses still reference it. Also clears any budget
     * exclusions for the name (in the same transaction) so no orphan exclusion rows
     * survive to affect a future category that reuses the name.
     */
    internal suspend fun deleteCategory(name: String): CategoryResult {
        var result: CategoryResult = CategoryResult.Success
        transactionRunner {
            if (expenseDao.countExpensesInCategory(name) > 0) {
                result = CategoryResult.InUse
                return@transactionRunner
            }
            categoryDao.deleteByName(name)
            budgetExcludedCategoryDao.removeExcludedByCategory(name)
        }
        return result
    }

    /**
     * Persists a new order; [orderedNames] is the full list of category names in display order.
     * The per-row updates run in one transaction, so reordering ~dozens of categories is a
     * single atomic write — fine at this scale, and avoids a variable-length CASE query.
     */
    internal suspend fun reorder(orderedNames: List<String>) {
        transactionRunner {
            orderedNames.forEachIndexed { index, name ->
                categoryDao.updateSortOrder(name, index)
            }
        }
    }

    internal suspend fun getAllForExport(): List<Category> = categoryDao.getAllOrderedOnce()

    internal suspend fun insertCategories(categories: List<Category>) {
        if (categories.isNotEmpty()) {
            categoryDao.insertAll(categories)
        }
    }

    internal suspend fun deleteAllCategories() = categoryDao.deleteAll()
}
