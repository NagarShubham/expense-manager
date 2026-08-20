package com.example.expancemanager.viewmodel

import app.cash.turbine.test
import com.example.expancemanager.MainDispatcherRule
import com.example.expancemanager.data.Category
import com.example.expancemanager.data.CategoryRepository
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.data.ExpenseRepository
import com.example.expancemanager.data.MonthlyTotal
import com.example.expancemanager.util.ReportPeriodKind
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val expenseRepository = mockk<ExpenseRepository>()
    private val categoryRepository = mockk<CategoryRepository>()

    @Before
    fun setUp() {
        every { expenseRepository.getTotalAmountByDateRange(any(), any()) } returns flowOf(0.0)
        every { expenseRepository.getCategoryTotalsByDateRange(any(), any()) } returns flowOf(emptyList())
        every { expenseRepository.getMonthlyTotalsByDateRange(any(), any()) } returns flowOf(emptyList())
        every { categoryRepository.getCategories() } returns flowOf(emptyList())
    }

    @Test
    fun customRangeStartAfterEnd_marksRangeInvalid() = runTest {
        val viewModel = ReportsViewModel(expenseRepository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.setCustomDateRange(
                startMillis = utcMillis(2024, Calendar.DECEMBER, 1),
                endMillis = utcMillis(2024, Calendar.JANUARY, 1)
            )
            val invalid = awaitItem()

            assertThat(invalid.selection.kind).isEqualTo(ReportPeriodKind.CUSTOM)
            assertThat(invalid.isRangeInvalid).isTrue()
            assertThat(invalid.report.hasExpenses).isFalse()
            assertThat(invalid.rangeLabel).isEmpty()
        }
    }

    @Test
    fun validCustomRange_usesSelectedDates() = runTest {
        every { expenseRepository.getTotalAmountByDateRange(any(), any()) } returns flowOf(80.0)
        every { expenseRepository.getCategoryTotalsByDateRange(any(), any()) } returns flowOf(
            listOf(CategoryTotal("Food", 80.0))
        )
        every { expenseRepository.getMonthlyTotalsByDateRange(any(), any()) } returns flowOf(
            listOf(MonthlyTotal(Calendar.MARCH, 2024, 80.0))
        )

        val viewModel = ReportsViewModel(expenseRepository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.setCustomDateRange(
                startMillis = utcMillis(2024, Calendar.JANUARY, 15),
                endMillis = utcMillis(2024, Calendar.MARCH, 10)
            )
            val updated = awaitItem()

            assertThat(updated.selection.kind).isEqualTo(ReportPeriodKind.CUSTOM)
            assertThat(updated.isRangeInvalid).isFalse()
            assertThat(updated.periodEndMonth).isEqualTo(Calendar.MARCH)
            assertThat(updated.periodEndYear).isEqualTo(2024)
            assertThat(updated.report.totalSpending).isEqualTo(80.0)
            assertThat(updated.report.hasExpenses).isTrue()
            assertThat(updated.rangeLabel).contains("–")
        }
    }

    @Test
    fun thisYear_buildsReportFromAggregatesAndCategories() = runTest {
        every { expenseRepository.getTotalAmountByDateRange(any(), any()) } returns flowOf(80.0)
        every { expenseRepository.getCategoryTotalsByDateRange(any(), any()) } returns flowOf(
            listOf(CategoryTotal("Food", 80.0))
        )
        every { expenseRepository.getMonthlyTotalsByDateRange(any(), any()) } returns flowOf(
            listOf(MonthlyTotal(Calendar.JANUARY, 2024, 80.0))
        )
        every { categoryRepository.getCategories() } returns flowOf(
            listOf(Category(name = "Food", emoji = "🍔", sortOrder = 0))
        )

        val viewModel = ReportsViewModel(expenseRepository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.selectPeriod(ReportPeriodKind.THIS_YEAR)
            val updated = awaitItem()

            assertThat(updated.isRangeInvalid).isFalse()
            assertThat(updated.selection.kind).isEqualTo(ReportPeriodKind.THIS_YEAR)
            assertThat(updated.report.totalSpending).isEqualTo(80.0)
            assertThat(updated.report.hasExpenses).isTrue()
            assertThat(updated.report.categoryTotals).hasSize(1)
            assertThat(updated.categoryEmojiMap).containsEntry("Food", "🍔")
        }
    }

    @Test
    fun selectPeriod_switchesBetweenPresets() = runTest {
        val viewModel = ReportsViewModel(expenseRepository, categoryRepository)

        viewModel.uiState.test {
            awaitItem()
            viewModel.selectPeriod(ReportPeriodKind.THIS_YEAR)
            awaitItem()
            viewModel.selectPeriod(ReportPeriodKind.LAST_SIX_MONTHS)
            val updated = awaitItem()

            assertThat(updated.selection.kind).isEqualTo(ReportPeriodKind.LAST_SIX_MONTHS)
            assertThat(updated.isRangeInvalid).isFalse()
        }
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int
    ): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
