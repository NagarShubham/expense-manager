package com.example.expancemanager.util

import java.util.Calendar

/** 0-based [month] (Calendar), full [year]. */
internal data class YearMonth(
    val month: Int,
    val year: Int
) : Comparable<YearMonth> {
    override fun compareTo(other: YearMonth): Int {
        val yearCmp = year.compareTo(other.year)
        return if (yearCmp != 0) yearCmp else month.compareTo(other.month)
    }
}

internal enum class ReportPeriodKind {
    LAST_SIX_MONTHS,
    THIS_YEAR,
    CUSTOM
}

/**
 * Inclusive calendar-month range. [THIS_YEAR] is year-to-date: January through
 * the current month of [now], so future months never inflate the average.
 */
internal data class ResolvedReportPeriod(
    val start: YearMonth,
    val end: YearMonth,
    val startMillis: Long,
    val endMillis: Long,
    val months: List<YearMonth>
)

internal object ReportPeriodResolver {
    const val LAST_SIX_MONTH_COUNT = 6

    internal fun resolve(
        kind: ReportPeriodKind,
        customStartMillis: Long = 0L,
        customEndMillis: Long = 0L,
        now: Calendar = Calendar.getInstance()
    ): ResolvedReportPeriod? {
        return when (kind) {
            ReportPeriodKind.LAST_SIX_MONTHS -> {
                val current = currentYearMonth(now)
                val startYm = shiftMonths(current, -(LAST_SIX_MONTH_COUNT - 1))
                resolveInclusive(startYm, current)
            }
            ReportPeriodKind.THIS_YEAR -> {
                val current = currentYearMonth(now)
                resolveInclusive(YearMonth(month = Calendar.JANUARY, year = current.year), current)
            }
            ReportPeriodKind.CUSTOM -> {
                if (customStartMillis > customEndMillis) return null
                val startLocal = DateUtils.utcPickerDateToLocalStart(customStartMillis)
                val endLocal = DateUtils.utcPickerDateToLocalEnd(customEndMillis)
                val startYm = DateUtils.yearMonthFrom(startLocal)
                val endYm = DateUtils.yearMonthFrom(endLocal)
                if (startYm > endYm) return null
                val months = monthsInclusive(startYm, endYm)
                ResolvedReportPeriod(
                    start = startYm,
                    end = endYm,
                    startMillis = startLocal,
                    endMillis = endLocal,
                    months = months
                )
            }
        }
    }

    internal fun isStartAfterEnd(
        startMillis: Long,
        endMillis: Long
    ): Boolean = startMillis > endMillis

    internal fun resolveInclusive(
        start: YearMonth,
        end: YearMonth
    ): ResolvedReportPeriod? {
        if (start > end) return null
        val months = monthsInclusive(start, end)
        val (startMillis, _) = DateUtils.getMonthDateRange(start.month, start.year)
        val (_, endMillis) = DateUtils.getMonthDateRange(end.month, end.year)
        return ResolvedReportPeriod(
            start = start,
            end = end,
            startMillis = startMillis,
            endMillis = endMillis,
            months = months
        )
    }

    internal fun monthsInclusive(
        start: YearMonth,
        end: YearMonth
    ): List<YearMonth> {
        if (start > end) return emptyList()
        val months = mutableListOf<YearMonth>()
        var cursor = start
        while (cursor <= end) {
            months += cursor
            cursor = shiftMonths(cursor, 1)
        }
        return months
    }

    internal fun currentYearMonth(now: Calendar = Calendar.getInstance()): YearMonth =
        YearMonth(month = now.get(Calendar.MONTH), year = now.get(Calendar.YEAR))

    internal fun shiftMonths(
        yearMonth: YearMonth,
        increment: Int
    ): YearMonth {
        val (month, year) = DateUtils.adjacentMonth(yearMonth.month, yearMonth.year, increment)
        return YearMonth(month = month, year = year)
    }
}
