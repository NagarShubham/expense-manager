package com.example.expensemanager.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

internal object DateUtils {
    private val localZone: ZoneId = ZoneId.systemDefault()

    /**
     * Formatters below are built from [java.time.format.DateTimeFormatter], which (unlike
     * [java.text.SimpleDateFormat]) is immutable and thread-safe, so a single shared instance
     * is reused instead of one per thread.
     */
    private val dateFormat = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    private val monthYearFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dayMonthFormat = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

    /** Returns the current (month, year) as a 0-based month and full year. */
    internal fun currentMonthYear(): Pair<Int, Int> =
        LocalDate.now(localZone).let { it.monthValue - 1 to it.year }

    /**
     * Returns (month, year) for the month that is [increment] months from the given month/year.
     * e.g. adjacentMonth(0, 2024, 1) -> (1, 2024), adjacentMonth(11, 2024, 1) -> (0, 2025).
     */
    internal fun adjacentMonth(
        month: Int,
        year: Int,
        increment: Int
    ): Pair<Int, Int> =
        LocalDate.of(year, month + 1, 1).plusMonths(increment.toLong())
            .let { it.monthValue - 1 to it.year }

    internal fun formatDate(timestamp: Long): String = toLocalDateTime(timestamp).format(dateFormat)

    /**
     * Formats a 0-based [month] and [year] as e.g. "February 2026".
     *
     * Anchored to the 1st of the month rather than "now": [LocalDate.of] rejects an
     * out-of-range day outright instead of silently rolling into the next month, so this
     * can't repeat the old bug where, on the 31st, February formatted as "March".
     */
    internal fun formatMonthYear(
        month: Int,
        year: Int
    ): String = LocalDate.of(year, month + 1, 1).format(monthYearFormat)

    internal fun formatDayMonth(timestamp: Long): String = toLocalDateTime(timestamp).format(dayMonthFormat)

    /**
     * Formats an amount as Indian Rupees with the Indian digit-grouping system
     * (lakh/crore, e.g. ₹1,00,000.00). The grouping pattern "#,##,##0.00" is
     * specified explicitly rather than relying on the en-IN locale's CLDR data,
     * which isn't guaranteed to be present on every JVM/Android runtime.
     *
     * When [hideZeroDecimals] is true, trailing `.00` is removed after formatting.
     * (INR [Currency] forces 2 fraction digits on [DecimalFormat], so a "whole rupees"
     * pattern still prints `.00` unless we strip it.)
     *
     * [DecimalFormat] is not thread-safe, but unlike [SimpleDateFormat] it's cheap to
     * construct, so a fresh instance per call is simpler than caching one per thread.
     */
    internal fun formatAmount(
        amount: Double,
        hideZeroDecimals: Boolean = false
    ): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH).apply {
            currency = Currency.getInstance("INR")
            currencySymbol = "₹"
        }
        val formatted = DecimalFormat("¤#,##,##0.00", symbols).format(amount)
        return if (hideZeroDecimals && formatted.endsWith(".00")) {
            formatted.removeSuffix(".00")
        } else {
            formatted
        }
    }

    /**
     * Renders an amount into the format a text field expects: plain digits, and no
     * trailing ".0" for whole rupees (the common case) so the field reads like
     * something the user typed rather than a machine value.
     */
    internal fun formatAmountForInput(amount: Double): String =
        if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            amount.toString()
        }

    internal fun yearMonthFrom(timestamp: Long): YearMonth =
        toLocalDateTime(timestamp).let { YearMonth(month = it.monthValue - 1, year = it.year) }

    /**
     * Interprets a Material date-picker UTC midnight value as that civil date
     * in the device's local timezone, returning local start-of-day millis.
     */
    internal fun utcPickerDateToLocalStart(utcMillis: Long): Long =
        utcPickerDateToLocal(utcMillis).atStartOfDay(localZone).toInstant().toEpochMilli()

    internal fun utcPickerDateToLocalEnd(utcMillis: Long): Long =
        utcPickerDateToLocal(utcMillis).atTime(23, 59, 59, 999_000_000)
            .atZone(localZone).toInstant().toEpochMilli()

    internal fun localMillisToUtcPickerDate(localMillis: Long): Long =
        toLocalDateTime(localMillis).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    internal fun getMonthDateRange(
        month: Int,
        year: Int
    ): Pair<Long, Long> {
        val firstDay = LocalDate.of(year, month + 1, 1)
        val startDate = firstDay.atStartOfDay(localZone).toInstant().toEpochMilli()
        val lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth())
        val endDate = lastDay.atTime(23, 59, 59, 999_000_000).atZone(localZone).toInstant().toEpochMilli()
        return Pair(startDate, endDate)
    }

    private fun utcPickerDateToLocal(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    private fun toLocalDateTime(timestamp: Long): LocalDateTime =
        Instant.ofEpochMilli(timestamp).atZone(localZone).toLocalDateTime()
}
