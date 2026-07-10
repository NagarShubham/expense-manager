package com.example.expancemanager.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class DateUtilsTest {
    @Test
    fun adjacentMonth_wrapsToNextYear() {
        val (month, year) = DateUtils.adjacentMonth(month = 11, year = 2024, increment = 1)

        assertThat(month).isEqualTo(0)
        assertThat(year).isEqualTo(2025)
    }

    @Test
    fun adjacentMonth_wrapsToPreviousYear() {
        val (month, year) = DateUtils.adjacentMonth(month = 0, year = 2024, increment = -1)

        assertThat(month).isEqualTo(11)
        assertThat(year).isEqualTo(2023)
    }

    @Test
    fun formatAmount_usesRupeeSymbol() {
        val formatted = DateUtils.formatAmount(100.0)

        assertThat(formatted).contains("₹")
    }

    @Test
    fun formatAmount_usesIndianDigitGroupingForLargeAmounts() {
        // Indian system groups as lakh: 1,00,000.00 — not 100,000.00
        val formatted = DateUtils.formatAmount(100000.0)

        assertThat(formatted).contains("₹100,000.00")
    }

    @Test
    fun formatAmount_formatsWithTwoDecimalPlaces() {
        val formatted = DateUtils.formatAmount(1234.5)

        assertThat(formatted).contains("1,234.50")
    }

    @Test
    fun formatAmount_isLocaleIndependent() {
        // Should produce INR formatting regardless of the JVM default locale at test time.
        val previousDefault = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            val formatted = DateUtils.formatAmount(50.0)

            assertThat(formatted).contains("₹")
            assertThat(formatted).doesNotContain("$")
        } finally {
            Locale.setDefault(previousDefault)
        }
    }

    @Test
    fun getMonthDateRange_startsOnFirstDayAndEndsOnLastDay() {
        val (startDate, endDate) = DateUtils.getMonthDateRange(month = 1, year = 2024)

        val startCalendar = Calendar.getInstance().apply { timeInMillis = startDate }
        assertThat(startCalendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
        assertThat(startCalendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        assertThat(startCalendar.get(Calendar.MINUTE)).isEqualTo(0)
        assertThat(startCalendar.get(Calendar.SECOND)).isEqualTo(0)
        assertThat(startCalendar.get(Calendar.MILLISECOND)).isEqualTo(0)

        val endCalendar = Calendar.getInstance().apply { timeInMillis = endDate }
        assertThat(endCalendar.get(Calendar.MONTH)).isEqualTo(1)
        assertThat(endCalendar.get(Calendar.YEAR)).isEqualTo(2024)
        assertThat(endCalendar.get(Calendar.DAY_OF_MONTH)).isEqualTo(29)
        assertThat(endCalendar.get(Calendar.HOUR_OF_DAY)).isEqualTo(23)
        assertThat(endCalendar.get(Calendar.MINUTE)).isEqualTo(59)
        assertThat(endCalendar.get(Calendar.SECOND)).isEqualTo(59)
        assertThat(endCalendar.get(Calendar.MILLISECOND)).isEqualTo(999)
    }
}
