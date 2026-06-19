package com.example.expancemanager.viewmodel

import app.cash.turbine.test
import com.example.expancemanager.MainDispatcherRule
import com.example.expancemanager.data.BudgetRepository
import com.example.expancemanager.data.Expense
import com.example.expancemanager.data.ExpenseRepository
import com.example.expancemanager.data.MonthlyBudget
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val expenseRepository = mockk<ExpenseRepository>()
    private val budgetRepository = mockk<BudgetRepository>()

    @Before
    fun setUp() {
        every { expenseRepository.getExpensesByDateRange(any(), any()) } returns flowOf(emptyList())
        every { expenseRepository.getTotalAmountByDateRange(any(), any()) } returns flowOf(null)
        every { expenseRepository.getCategoryTotalsByDateRange(any(), any()) } returns flowOf(emptyList())
        every { budgetRepository.getBudgetByMonthYear(any(), any()) } returns flowOf(null)
        every { budgetRepository.getExcludedCategoriesByMonthYear(any(), any()) } returns flowOf(emptyList())
    }

    @Test
    fun loadExpensesForMonth_updatesSelectedMonthAndYear() = runTest {
        val viewModel = ExpenseViewModel(expenseRepository, budgetRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.loadExpensesForMonth(month = 5, year = 2024)
            val updated = awaitItem()

            assertThat(updated.selectedMonth).isEqualTo(5)
            assertThat(updated.selectedYear).isEqualTo(2024)
        }
    }

    @Test
    fun changeMonth_advancesSelectedMonth() = runTest {
        val viewModel = ExpenseViewModel(expenseRepository, budgetRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.loadExpensesForMonth(month = 0, year = 2024)
            awaitItem()
            viewModel.changeMonth(increment = 1)
            val february = awaitItem()

            assertThat(february.selectedMonth).isEqualTo(1)
            assertThat(february.selectedYear).isEqualTo(2024)
        }
    }

    @Test
    fun excludedCategories_areSubtractedFromBudgetTotal() = runTest {
        val expenses = listOf(
            Expense(title = "Groceries", amount = 100.0, category = "Food", date = 1L),
            Expense(title = "Flight", amount = 50.0, category = "Travel", date = 1L)
        )
        every { expenseRepository.getExpensesByDateRange(any(), any()) } returns flowOf(expenses)
        every { expenseRepository.getTotalAmountByDateRange(any(), any()) } returns flowOf(150.0)
        every { budgetRepository.getBudgetByMonthYear(any(), any()) } returns flowOf(
            MonthlyBudget(month = 0, year = 2024, expectedAmount = 500.0)
        )
        every { budgetRepository.getExcludedCategoriesByMonthYear(any(), any()) } returns flowOf(listOf("Travel"))

        val viewModel = ExpenseViewModel(expenseRepository, budgetRepository)

        viewModel.uiState.test {
            val state = awaitItem()

            assertThat(state.totalAmount).isEqualTo(150.0)
            assertThat(state.totalAmountForBudget).isEqualTo(100.0)
            assertThat(state.expectedMonthlyAmount).isEqualTo(500.0)
            assertThat(state.excludedCategoryNames).containsExactly("Travel")
        }
    }
}
