package com.example.expancemanager.data

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ExpenseRepositoryTest {
    private val expenseDao = mockk<ExpenseDao>(relaxed = true)
    private val repository = ExpenseRepository(expenseDao)

    @Test
    fun getExpensesByDateRange_delegatesToDao() = runTest {
        val expected = listOf(
            Expense(title = "Lunch", amount = 12.0, category = "Food", date = 1L)
        )
        every { expenseDao.getExpensesByDateRange(startDate = 10L, endDate = 20L) } returns flowOf(expected)

        repository.getExpensesByDateRange(startDate = 10L, endDate = 20L).test {
            assertThat(awaitItem()).isEqualTo(expected)
            awaitComplete()
        }
    }

    @Test
    fun insertExpense_delegatesToDao() = runTest {
        val expense = Expense(title = "Bus", amount = 3.0, category = "Travel", date = 2L)
        coEvery { expenseDao.insertExpense(expense) } returns 42L

        val insertedId = repository.insertExpense(expense)

        assertThat(insertedId).isEqualTo(42L)
        coVerify(exactly = 1) { expenseDao.insertExpense(expense) }
    }
}
