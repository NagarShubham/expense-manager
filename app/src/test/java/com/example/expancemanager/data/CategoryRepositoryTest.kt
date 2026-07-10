package com.example.expancemanager.data

import com.example.expancemanager.data.CategoryRepository.CategoryResult
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CategoryRepositoryTest {
    private val categoryDao = mockk<CategoryDao>(relaxed = true)
    private val expenseDao = mockk<ExpenseDao>(relaxed = true)
    private val budgetExcludedCategoryDao = mockk<BudgetExcludedCategoryDao>(relaxed = true)

    // Runs the transaction block inline so multi-table logic is exercised directly.
    private val repository = CategoryRepository(
        categoryDao = categoryDao,
        expenseDao = expenseDao,
        budgetExcludedCategoryDao = budgetExcludedCategoryDao,
        transactionRunner = { block -> block() }
    )

    @Test
    fun addCategory_blankName_returnsBlankName() = runTest {
        val result = repository.addCategory("   ", "😀")
        assertThat(result).isEqualTo(CategoryResult.BlankName)
    }

    @Test
    fun addCategory_duplicate_returnsDuplicate() = runTest {
        coEvery { categoryDao.countByName("Food") } returns 1
        val result = repository.addCategory("Food", "🍔")
        assertThat(result).isEqualTo(CategoryResult.DuplicateName)
    }

    @Test
    fun addCategory_new_insertsWithNextSortOrder() = runTest {
        coEvery { categoryDao.countByName("Gym") } returns 0
        coEvery { categoryDao.getMaxSortOrder() } returns 4

        val result = repository.addCategory(" Gym ", "🏋️")

        assertThat(result).isEqualTo(CategoryResult.Success)
        coVerify { categoryDao.insert(Category(name = "Gym", emoji = "🏋️", sortOrder = 5)) }
    }

    @Test
    fun addCategory_firstCategory_getsSortOrderZero() = runTest {
        coEvery { categoryDao.countByName(any()) } returns 0
        coEvery { categoryDao.getMaxSortOrder() } returns null

        repository.addCategory("First", "1️⃣")

        coVerify { categoryDao.insert(Category(name = "First", emoji = "1️⃣", sortOrder = 0)) }
    }

    @Test
    fun updateCategory_rename_cascadesToExpensesAndExclusions() = runTest {
        coEvery { categoryDao.countByName("Dining") } returns 0
        coEvery { categoryDao.getAllOrderedOnce() } returns
            listOf(Category(name = "Food", emoji = "🍔", sortOrder = 2))

        val result = repository.updateCategory(oldName = "Food", newName = "Dining", emoji = "🍽️")

        assertThat(result).isEqualTo(CategoryResult.Success)
        coVerify { categoryDao.deleteByName("Food") }
        coVerify { categoryDao.insert(Category(name = "Dining", emoji = "🍽️", sortOrder = 2)) }
        coVerify { expenseDao.renameCategory("Food", "Dining") }
        coVerify { budgetExcludedCategoryDao.renameExcludedCategory("Food", "Dining") }
    }

    @Test
    fun updateCategory_sameName_updatesInPlaceWithoutCascade() = runTest {
        coEvery { categoryDao.getAllOrderedOnce() } returns
            listOf(Category(name = "Food", emoji = "🍔", sortOrder = 2))

        val result = repository.updateCategory(oldName = "Food", newName = "Food", emoji = "🥗")

        assertThat(result).isEqualTo(CategoryResult.Success)
        coVerify { categoryDao.update(Category(name = "Food", emoji = "🥗", sortOrder = 2)) }
        coVerify(exactly = 0) { expenseDao.renameCategory(any(), any()) }
    }

    @Test
    fun updateCategory_renameToExistingName_returnsDuplicate() = runTest {
        coEvery { categoryDao.countByName("Travel") } returns 1

        val result = repository.updateCategory(oldName = "Food", newName = "Travel", emoji = "✈️")

        assertThat(result).isEqualTo(CategoryResult.DuplicateName)
        coVerify(exactly = 0) { categoryDao.deleteByName(any()) }
    }

    @Test
    fun deleteCategory_inUse_returnsInUseAndDoesNotDelete() = runTest {
        coEvery { expenseDao.countExpensesInCategory("Food") } returns 3

        val result = repository.deleteCategory("Food")

        assertThat(result).isEqualTo(CategoryResult.InUse)
        coVerify(exactly = 0) { categoryDao.deleteByName(any()) }
    }

    @Test
    fun deleteCategory_unused_deletesAndClearsExclusions() = runTest {
        coEvery { expenseDao.countExpensesInCategory("Food") } returns 0

        val result = repository.deleteCategory("Food")

        assertThat(result).isEqualTo(CategoryResult.Success)
        coVerify { categoryDao.deleteByName("Food") }
        // Orphan budget exclusions for the name must be cleared too.
        coVerify { budgetExcludedCategoryDao.removeExcludedByCategory("Food") }
    }

    @Test
    fun deleteCategory_inUse_doesNotClearExclusions() = runTest {
        coEvery { expenseDao.countExpensesInCategory("Food") } returns 2

        val result = repository.deleteCategory("Food")

        assertThat(result).isEqualTo(CategoryResult.InUse)
        coVerify(exactly = 0) { budgetExcludedCategoryDao.removeExcludedByCategory(any()) }
    }

    @Test
    fun reorder_assignsSortOrderByIndex() = runTest {
        repository.reorder(listOf("Travel", "Food", "Rent"))

        coVerify { categoryDao.updateSortOrder("Travel", 0) }
        coVerify { categoryDao.updateSortOrder("Food", 1) }
        coVerify { categoryDao.updateSortOrder("Rent", 2) }
    }
}
