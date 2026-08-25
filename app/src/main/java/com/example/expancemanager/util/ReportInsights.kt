package com.example.expancemanager.util

import androidx.compose.runtime.Immutable
import com.example.expancemanager.data.CategoryTotal
import com.example.expancemanager.data.MonthlyTotal

@Immutable
internal data class PeriodSpendingReport(
    val totalSpending: Double,
    val monthlyAverage: Double,
    val currentMonth: MonthlyTotal?,
    val highestMonth: MonthlyTotal?,
    val lowestMonth: MonthlyTotal?,
    val categoryTotals: List<CategoryTotal>,
    val hasExpenses: Boolean
) {
    companion object {
        val Empty = PeriodSpendingReport(
            totalSpending = 0.0,
            monthlyAverage = 0.0,
            currentMonth = null,
            highestMonth = null,
            lowestMonth = null,
            categoryTotals = emptyList(),
            hasExpenses = false
        )
    }
}

internal object ReportInsights {
    /**
     * Builds period metrics. [monthsInRange] is the inclusive calendar span
     * (zero-spend months included in the average and min/max). Ties for
     * highest/lowest pick the earliest month.
     *
     * [currentYearMonth] is shown on its own card and is excluded from lowest
     * so a partial in-progress month is not treated as the cheapest month.
     */
    internal fun buildPeriodReport(
        totalSpending: Double,
        monthlyTotals: List<MonthlyTotal>,
        categoryTotals: List<CategoryTotal>,
        monthsInRange: List<YearMonth>,
        currentYearMonth: YearMonth = ReportPeriodResolver.currentYearMonth()
    ): PeriodSpendingReport {
        val filledMonths = fillMonthlyTotals(monthsInRange, monthlyTotals)
        val monthCount = filledMonths.size
        val monthlyAverage = if (monthCount == 0) 0.0 else totalSpending / monthCount
        val hasExpenses = totalSpending > 0.0 || categoryTotals.isNotEmpty()
        val currentMonth = filledMonths.find {
            it.month == currentYearMonth.month && it.year == currentYearMonth.year
        }
        val monthsForLowest = filledMonths.filterNot {
            it.month == currentYearMonth.month && it.year == currentYearMonth.year
        }
        return PeriodSpendingReport(
            totalSpending = totalSpending,
            monthlyAverage = monthlyAverage,
            currentMonth = if (hasExpenses) currentMonth else null,
            highestMonth = if (hasExpenses) pickExtreme(filledMonths, preferMax = true) else null,
            lowestMonth = if (hasExpenses) pickExtreme(monthsForLowest, preferMax = false) else null,
            categoryTotals = categoryTotals.sortedByDescending { it.total },
            hasExpenses = hasExpenses
        )
    }

    internal fun fillMonthlyTotals(
        monthsInRange: List<YearMonth>,
        monthlyTotals: List<MonthlyTotal>
    ): List<MonthlyTotal> {
        val byKey = monthlyTotals.associateBy { YearMonth(it.month, it.year) }
        return monthsInRange.map { ym ->
            byKey[ym] ?: MonthlyTotal(month = ym.month, year = ym.year, total = 0.0)
        }
    }

    internal fun pickExtreme(
        months: List<MonthlyTotal>,
        preferMax: Boolean
    ): MonthlyTotal? {
        if (months.isEmpty()) return null
        return if (preferMax) {
            months.maxByOrNull { it.total }
        } else {
            months.minByOrNull { it.total }
        }
    }
}
