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
    /** Returns the current (month, year) as a 0-based month and full year. */
    internal fun currentMonthYear(): Pair<Int, Int> =
        Calendar.getInstance().run { get(Calendar.MONTH) to get(Calendar.YEAR) }

    /**
     * Returns (month, year) for the month that is [increment] months from the given month/year.
     * e.g. adjacentMonth(0, 2024, 1) -> (1, 2024), adjacentMonth(11, 2024, 1) -> (0, 2025).
     */
    internal fun adjacentMonth(
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

    internal fun formatDate(timestamp: Long): String = dateFormat.get()!!.format(Date(timestamp))

    internal fun formatMonthYear(
        month: Int,
        year: Int
    ): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.MONTH, month)
            set(Calendar.YEAR, year)
        }
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
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
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
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    internal fun getMonthDateRange(
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

    private fun utcPickerDateToLocal(
        utcMillis: Long,
        hour: Int,
        minute: Int,
        second: Int,
        millisecond: Int
    ): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        return Calendar.getInstance().apply {
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
                hour,
                minute,
                second
            )
            set(Calendar.MILLISECOND, millisecond)
        }.timeInMillis
    }
}
