package com.example.expancemanager.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
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

    fun formatAmount(amount: Double): String = NumberFormat.getCurrencyInstance(Locale.getDefault()).format(amount)

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
