package com.example.expancemanager.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReportPeriodResolverTest {
    private val nowMarch2024: Calendar = Calendar.getInstance().apply {
        set(2024, Calendar.MARCH, 15, 12, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    @Test
    fun lastSixMonths_includesCurrentAndFivePrior() {
        val resolved = ReportPeriodResolver.resolve(
            kind = ReportPeriodKind.LAST_SIX_MONTHS,
            now = nowMarch2024
        )

        assertThat(resolved).isNotNull()
        assertThat(resolved!!.start).isEqualTo(YearMonth(Calendar.OCTOBER, 2023))
        assertThat(resolved.end).isEqualTo(YearMonth(Calendar.MARCH, 2024))
        assertThat(resolved.months).hasSize(6)
    }

    @Test
    fun thisYear_isYearToDateThroughCurrentMonth() {
        val resolved = ReportPeriodResolver.resolve(
            kind = ReportPeriodKind.THIS_YEAR,
            now = nowMarch2024
        )

        assertThat(resolved).isNotNull()
        assertThat(resolved!!.start).isEqualTo(YearMonth(Calendar.JANUARY, 2024))
        assertThat(resolved.end).isEqualTo(YearMonth(Calendar.MARCH, 2024))
        assertThat(resolved.months).hasSize(3)
    }

    @Test
    fun thisYear_inJanuary_isSingleMonth() {
        val now = Calendar.getInstance().apply {
            set(2024, Calendar.JANUARY, 8, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val resolved = ReportPeriodResolver.resolve(
            kind = ReportPeriodKind.THIS_YEAR,
            now = now
        )

        assertThat(resolved!!.months).containsExactly(YearMonth(Calendar.JANUARY, 2024))
    }

    @Test
    fun customRange_returnsNullWhenStartAfterEnd() {
        val start = utcMillis(2024, Calendar.MAY, 1)
        val end = utcMillis(2024, Calendar.MARCH, 1)
        val resolved = ReportPeriodResolver.resolve(
            kind = ReportPeriodKind.CUSTOM,
            customStartMillis = start,
            customEndMillis = end,
            now = nowMarch2024
        )

        assertThat(resolved).isNull()
        assertThat(ReportPeriodResolver.isStartAfterEnd(start, end)).isTrue()
    }

    @Test
    fun customRange_isInclusiveAndUsesLocalDayBounds() {
        val startUtc = utcMillis(2024, Calendar.JANUARY, 15)
        val endUtc = utcMillis(2024, Calendar.MARCH, 10)
        val resolved = ReportPeriodResolver.resolve(
            kind = ReportPeriodKind.CUSTOM,
            customStartMillis = startUtc,
            customEndMillis = endUtc,
            now = nowMarch2024
        )

        assertThat(resolved).isNotNull()
        assertThat(resolved!!.months).containsExactly(
            YearMonth(Calendar.JANUARY, 2024),
            YearMonth(Calendar.FEBRUARY, 2024),
            YearMonth(Calendar.MARCH, 2024)
        ).inOrder()
        assertThat(resolved.startMillis).isEqualTo(DateUtils.utcPickerDateToLocalStart(startUtc))
        assertThat(resolved.endMillis).isEqualTo(DateUtils.utcPickerDateToLocalEnd(endUtc))
        val startDay = Calendar.getInstance().apply { timeInMillis = resolved.startMillis }
        assertThat(startDay.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        val endDay = Calendar.getInstance().apply { timeInMillis = resolved.endMillis }
        assertThat(endDay.get(Calendar.HOUR_OF_DAY)).isEqualTo(23)
    }

    @Test
    fun monthsInclusive_emptyWhenStartAfterEnd() {
        assertThat(
            ReportPeriodResolver.monthsInclusive(
                start = YearMonth(Calendar.APRIL, 2024),
                end = YearMonth(Calendar.MARCH, 2024)
            )
        ).isEmpty()
    }

    @Test
    fun resolveInclusive_returnsNullWhenStartAfterEnd() {
        assertThat(
            ReportPeriodResolver.resolveInclusive(
                start = YearMonth(Calendar.MAY, 2024),
                end = YearMonth(Calendar.APRIL, 2024)
            )
        ).isNull()
    }

    @Test
    fun yearMonth_comparesByYearThenMonth() {
        assertThat(YearMonth(Calendar.DECEMBER, 2023)).isLessThan(YearMonth(Calendar.JANUARY, 2024))
        assertThat(YearMonth(Calendar.FEBRUARY, 2024)).isGreaterThan(YearMonth(Calendar.JANUARY, 2024))
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
