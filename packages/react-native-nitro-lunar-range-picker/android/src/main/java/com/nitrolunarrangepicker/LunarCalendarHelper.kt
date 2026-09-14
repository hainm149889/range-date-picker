package com.nitrolunarrangepicker

import java.util.Calendar
import java.util.Locale

data class LunarDateResult(
    val day: Int,
    val month: Int,
    val year: Int,
    val isLeap: Boolean,
    val lunarDayName: String,
    val isSpecialDay: Boolean
)

object LunarCalendarHelper {
    fun convertSolarToLunar(calendar: Calendar, language: String = "vi"): LunarDateResult {
        return try {
            val uCalendar = android.icu.util.ChineseCalendar()
            uCalendar.timeInMillis = calendar.timeInMillis

            val day = uCalendar.get(android.icu.util.ChineseCalendar.DAY_OF_MONTH)
            val month = uCalendar.get(android.icu.util.ChineseCalendar.MONTH) + 1
            val year = uCalendar.get(android.icu.util.ChineseCalendar.YEAR)
            val isLeap = uCalendar.get(android.icu.util.ChineseCalendar.IS_LEAP_MONTH) == 1

            val lunarDayName = String.format(Locale.getDefault(), "%02d/%02d", day, month)
            val isSpecialDay = (day == 1 || day == 15)

            LunarDateResult(day, month, year, isLeap, lunarDayName, isSpecialDay)
        } catch (e: Exception) {
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            LunarDateResult(
                day = day,
                month = month,
                year = year,
                isLeap = false,
                lunarDayName = String.format(Locale.getDefault(), "%02d/%02d", day, month),
                isSpecialDay = (day == 1 || day == 15)
            )
        }
    }
}
