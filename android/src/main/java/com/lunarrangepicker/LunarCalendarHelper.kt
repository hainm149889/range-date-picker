package com.lunarrangepicker

import java.util.Calendar
import java.util.Locale

data class LunarDateResult(
    val day: Int,
    val month: Int,
    val year: Int,
    val isLeap: Boolean,
    val lunarDayName: String
)

object LunarCalendarHelper {
    fun convertSolarToLunar(calendar: Calendar, language: String = "vi"): LunarDateResult {
        // Sử dụng ChineseCalendar của Android ICU hoặc thuật toán chuyển đổi GMT+7
        val uCalendar = android.icu.util.ChineseCalendar()
        uCalendar.timeInMillis = calendar.timeInMillis

        val day = uCalendar.get(android.icu.util.ChineseCalendar.DAY_OF_MONTH)
        val month = uCalendar.get(android.icu.util.ChineseCalendar.MONTH) + 1
        val year = uCalendar.get(android.icu.util.ChineseCalendar.YEAR)
        val isLeap = uCalendar.get(android.icu.util.ChineseCalendar.IS_LEAP_MONTH) == 1

        val dayName = when {
            language == "vi" && day == 1 -> "Mùng 1"
            language == "vi" && day == 15 -> "Rằm"
            day < 10 -> "0$day"
            else -> day.toString()
        }

        return LunarDateResult(day, month, year, isLeap, dayName)
    }
}