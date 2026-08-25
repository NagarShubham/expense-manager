package com.example.expancemanager.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object DateUtils {
    private val utcTimeZone: TimeZone = TimeZone.getTimeZone("UTC")
    private val localCalendar = ThreadLocal.withInitial { Calendar.getInstance() }
    private val utcCalendar = ThreadLocal.withInitial { Calendar.getInstance(utcTimeZone) }

    /** Returns the current (month, year) as a 0-based month and full year. */
    internal fun currentMonthYear(): Pair<Int, Int> =
        localCalendar.get()!!.run { timeInMillis = System.currentTimeMillis(); get(Calendar.MONTH) to get(Calendar.YEAR) }

    /**
     * Returns (month, year) for the month that is [increment] months from the given month/year.
     * e.g. adjacentMonth(0, 2024, 1) -> (1, 2024), adjacentMonth(11, 2024, 1) -> (0, 2025).
     */
    internal fun adjacentMonth(
        month: Int,
        year: Int,
        increment: Int
    ): Pair<Int, Int> {
        val calendar = localCalendar.get()!!
        calendar.clear()
        calendar.set(year, month, 1)
        calendar.add(Calendar.MONTH, increment)
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

    internal fun formatDate(timestamp: Long): String = dateFormat.get()!!.format(Date(timestamp))

    internal fun formatMonthYear(
        month: Int,
        year: Int
    ): String {
        val calendar = localCalendar.get()!!
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.set(Calendar.MONTH, month)
        calendar.set(Calendar.YEAR, year)
        return monthYearFormat.get()!!.format(calendar.time)
    }

    internal fun formatDayMonth(timestamp: Long): String = dayMonthFormat.get()!!.format(Date(timestamp))

    /**
     * Formats an amount as Indian Rupees with the Indian digit-grouping system
     * (lakh/crore, e.g. ₹1,00,000.00). The grouping pattern "#,##,##0.00" is
     * specified explicitly rather than relying on the en-IN locale's CLDR data,
     * which isn't guaranteed to be present on every JVM/Android runtime.
     *
     * When [hideZeroDecimals] is true, trailing `.00` is removed after formatting.
     * (INR [Currency] forces 2 fraction digits on [DecimalFormat], so a "whole rupees"
     * pattern still prints `.00` unless we strip it.)
     */
    private val amountFormat = ThreadLocal.withInitial {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH).apply {
            currency = Currency.getInstance("INR")
            currencySymbol = "₹"
        }
        DecimalFormat("¤#,##,##0.00", symbols)
    }

    internal fun formatAmount(
        amount: Double,
        hideZeroDecimals: Boolean = false
    ): String {
        val formatted = amountFormat.get()!!.format(amount)
        return if (hideZeroDecimals && formatted.endsWith(".00")) {
            formatted.removeSuffix(".00")
        } else {
            formatted
        }
    }

    internal fun yearMonthFrom(timestamp: Long): YearMonth {
        val calendar = localCalendar.get()!!
        calendar.timeInMillis = timestamp
        return YearMonth(month = calendar.get(Calendar.MONTH), year = calendar.get(Calendar.YEAR))
    }

    /**
     * Interprets a Material date-picker UTC midnight value as that civil date
     * in the device's local timezone, returning local start-of-day millis.
     */
    internal fun utcPickerDateToLocalStart(utcMillis: Long): Long =
        utcPickerDateToLocal(utcMillis, hour = 0, minute = 0, second = 0, millisecond = 0)

    internal fun utcPickerDateToLocalEnd(utcMillis: Long): Long =
        utcPickerDateToLocal(utcMillis, hour = 23, minute = 59, second = 59, millisecond = 999)

    internal fun localMillisToUtcPickerDate(localMillis: Long): Long {
        val local = localCalendar.get()!!
        local.timeInMillis = localMillis
        val year = local.get(Calendar.YEAR)
        val month = local.get(Calendar.MONTH)
        val day = local.get(Calendar.DAY_OF_MONTH)
        val utc = utcCalendar.get()!!
        utc.clear()
        utc.set(year, month, day, 0, 0, 0)
        utc.set(Calendar.MILLISECOND, 0)
        return utc.timeInMillis
    }

    internal fun getMonthDateRange(
        month: Int,
        year: Int
    ): Pair<Long, Long> {
        val calendar = localCalendar.get()!!
        calendar.clear()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.timeInMillis

        return Pair(startDate, endDate)
    }

    private fun utcPickerDateToLocal(
        utcMillis: Long,
        hour: Int,
        minute: Int,
        second: Int,
        millisecond: Int
    ): Long {
        val utc = utcCalendar.get()!!
        utc.timeInMillis = utcMillis
        val year = utc.get(Calendar.YEAR)
        val month = utc.get(Calendar.MONTH)
        val day = utc.get(Calendar.DAY_OF_MONTH)
        val local = localCalendar.get()!!
        local.clear()
        local.set(year, month, day, hour, minute, second)
        local.set(Calendar.MILLISECOND, millisecond)
        return local.timeInMillis
    }
}
