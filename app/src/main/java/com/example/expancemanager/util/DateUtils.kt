package com.example.expancemanager.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    /**
     * Returns (month, year) for the month that is [increment] months from the given month/year.
     * e.g. adjacentMonth(0, 2024, 1) -> (1, 2024), adjacentMonth(11, 2024, 1) -> (0, 2025).
     */
    /** Returns the current (month, year) as a 0-based month and full year. */
    fun currentMonthYear(): Pair<Int, Int> =
        Calendar.getInstance().run { get(Calendar.MONTH) to get(Calendar.YEAR) }

    fun adjacentMonth(
        month: Int,
        year: Int,
        increment: Int
    ): Pair<Int, Int> {
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1)
            add(Calendar.MONTH, increment)
        }
        return calendar.get(Calendar.MONTH) to calendar.get(Calendar.YEAR)
    }

    // Thread-safe formatters using ThreadLocal
    private val dateFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }
    private val monthYearFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    }
    private val dayMonthFormat = ThreadLocal.withInitial {
        SimpleDateFormat("dd MMM", Locale.getDefault())
    }

    fun formatDate(timestamp: Long): String = dateFormat.get()!!.format(Date(timestamp))

    fun formatMonthYear(
        month: Int,
        year: Int
    ): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
            set(Calendar.YEAR, year)
        }
        return monthYearFormat.get()!!.format(calendar.time)
    }

    fun formatDayMonth(timestamp: Long): String = dayMonthFormat.get()!!.format(Date(timestamp))

    /**
     * Formats an amount as Indian Rupees with the Indian digit-grouping system
     * (lakh/crore, e.g. ₹1,00,000.00). The grouping pattern "#,##,##0.00" is
     * specified explicitly rather than relying on the en-IN locale's CLDR data,
     * which isn't guaranteed to be present on every JVM/Android runtime.
     */
    private val amountFormat = ThreadLocal.withInitial {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH).apply {
            currency = Currency.getInstance("INR")
            currencySymbol = "₹"
        }
        DecimalFormat("¤#,##,##0.00", symbols)
    }

    fun formatAmount(amount: Double): String = amountFormat.get()!!.format(amount)

    fun getMonthDateRange(
        month: Int,
        year: Int
    ): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startDate = calendar.timeInMillis

        calendar.apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }
}
