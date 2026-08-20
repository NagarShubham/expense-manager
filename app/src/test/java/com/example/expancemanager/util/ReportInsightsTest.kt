package com.example.expancemanager.util

import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.data.MonthlyTotal
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar

class ReportInsightsTest {
    private val march2024 = YearMonth(Calendar.MARCH, 2024)

    @Test
    fun monthlyAverage_dividesByAllMonthsIncludingZeros() {
        val months = listOf(
            YearMonth(Calendar.JANUARY, 2024),
            YearMonth(Calendar.FEBRUARY, 2024),
            YearMonth(Calendar.MARCH, 2024)
        )
        val report = ReportInsights.buildPeriodReport(
            totalSpending = 90.0,
            monthlyTotals = listOf(
                MonthlyTotal(month = Calendar.JANUARY, year = 2024, total = 90.0)
            ),
            categoryTotals = listOf(CategoryTotal("Food", 90.0)),
            monthsInRange = months,
            currentYearMonth = march2024
        )

        assertThat(report.monthlyAverage).isEqualTo(30.0)
        assertThat(report.hasExpenses).isTrue()
        assertThat(report.lowestMonth?.month).isEqualTo(Calendar.FEBRUARY)
        assertThat(report.lowestMonth?.total).isEqualTo(0.0)
        assertThat(report.highestMonth?.month).isEqualTo(Calendar.JANUARY)
        assertThat(report.highestMonth?.total).isEqualTo(90.0)
        assertThat(report.currentMonth?.month).isEqualTo(Calendar.MARCH)
        assertThat(report.currentMonth?.total).isEqualTo(0.0)
    }

    @Test
    fun emptyPeriod_hidesExtremesAndFlagsNoExpenses() {
        val report = ReportInsights.buildPeriodReport(
            totalSpending = 0.0,
            monthlyTotals = emptyList(),
            categoryTotals = emptyList(),
            monthsInRange = listOf(YearMonth(Calendar.JANUARY, 2024)),
            currentYearMonth = YearMonth(Calendar.JANUARY, 2024)
        )

        assertThat(report.hasExpenses).isFalse()
        assertThat(report.highestMonth).isNull()
        assertThat(report.lowestMonth).isNull()
        assertThat(report.currentMonth).isNull()
        assertThat(report.monthlyAverage).isEqualTo(0.0)
    }

    @Test
    fun tiesForHighestAndLowest_pickEarliestMonth() {
        val months = listOf(
            YearMonth(Calendar.JANUARY, 2024),
            YearMonth(Calendar.FEBRUARY, 2024),
            YearMonth(Calendar.MARCH, 2024)
        )
        val report = ReportInsights.buildPeriodReport(
            totalSpending = 30.0,
            monthlyTotals = listOf(
                MonthlyTotal(Calendar.JANUARY, 2024, 10.0),
                MonthlyTotal(Calendar.FEBRUARY, 2024, 10.0),
                MonthlyTotal(Calendar.MARCH, 2024, 10.0)
            ),
            categoryTotals = listOf(CategoryTotal("Food", 30.0)),
            monthsInRange = months,
            currentYearMonth = YearMonth(Calendar.DECEMBER, 2023)
        )

        assertThat(report.highestMonth?.month).isEqualTo(Calendar.JANUARY)
        assertThat(report.lowestMonth?.month).isEqualTo(Calendar.JANUARY)
        assertThat(report.currentMonth).isNull()
    }

    @Test
    fun lowestMonth_excludesCurrentMonth() {
        val months = listOf(
            YearMonth(Calendar.JANUARY, 2024),
            YearMonth(Calendar.FEBRUARY, 2024),
            YearMonth(Calendar.MARCH, 2024)
        )
        val report = ReportInsights.buildPeriodReport(
            totalSpending = 16.0,
            monthlyTotals = listOf(
                MonthlyTotal(Calendar.JANUARY, 2024, 10.0),
                MonthlyTotal(Calendar.FEBRUARY, 2024, 5.0),
                MonthlyTotal(Calendar.MARCH, 2024, 1.0)
            ),
            categoryTotals = listOf(CategoryTotal("Food", 16.0)),
            monthsInRange = months,
            currentYearMonth = march2024
        )

        assertThat(report.currentMonth?.total).isEqualTo(1.0)
        assertThat(report.lowestMonth?.month).isEqualTo(Calendar.FEBRUARY)
        assertThat(report.lowestMonth?.total).isEqualTo(5.0)
        assertThat(report.highestMonth?.month).isEqualTo(Calendar.JANUARY)
    }

    @Test
    fun lowestMonth_isNullWhenRangeIsOnlyCurrentMonth() {
        val report = ReportInsights.buildPeriodReport(
            totalSpending = 20.0,
            monthlyTotals = listOf(MonthlyTotal(Calendar.MARCH, 2024, 20.0)),
            categoryTotals = listOf(CategoryTotal("Food", 20.0)),
            monthsInRange = listOf(march2024),
            currentYearMonth = march2024
        )

        assertThat(report.currentMonth?.total).isEqualTo(20.0)
        assertThat(report.highestMonth?.month).isEqualTo(Calendar.MARCH)
        assertThat(report.lowestMonth).isNull()
    }

    @Test
    fun categoryTotals_sortedDescending() {
        val report = ReportInsights.buildPeriodReport(
            totalSpending = 150.0,
            monthlyTotals = listOf(MonthlyTotal(Calendar.JANUARY, 2024, 150.0)),
            categoryTotals = listOf(
                CategoryTotal("Food", 50.0),
                CategoryTotal("Travel", 100.0)
            ),
            monthsInRange = listOf(YearMonth(Calendar.JANUARY, 2024)),
            currentYearMonth = march2024
        )

        assertThat(report.categoryTotals.map { it.category }).containsExactly("Travel", "Food").inOrder()
    }

    @Test
    fun fillMonthlyTotals_insertsZeroMonthsInOrder() {
        val filled = ReportInsights.fillMonthlyTotals(
            monthsInRange = listOf(
                YearMonth(Calendar.JANUARY, 2024),
                YearMonth(Calendar.FEBRUARY, 2024)
            ),
            monthlyTotals = listOf(MonthlyTotal(Calendar.FEBRUARY, 2024, 12.0))
        )

        assertThat(filled[0].total).isEqualTo(0.0)
        assertThat(filled[1].total).isEqualTo(12.0)
    }
}
