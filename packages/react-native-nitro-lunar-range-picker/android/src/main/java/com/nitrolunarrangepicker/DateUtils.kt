package com.nitrolunarrangepicker

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    fun cleanToStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    fun parseDateString(str: String?): Calendar? {
        if (str.isNullOrBlank()) return null
        val trimmed = str.trim()

        val formats = arrayOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US)
                if (fmt.endsWith("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(trimmed)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    cleanToStartOfDay(cal)
                    return cal
                }
            } catch (e: Exception) {
                // try next
            }
        }

        trimmed.toDoubleOrNull()?.let { num ->
            val millis = if (num > 10000000000.0) num.toLong() else (num * 1000).toLong()
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cleanToStartOfDay(cal)
            return cal
        }

        return null
    }

    fun isSameDay(c1: Calendar?, c2: Calendar?): Boolean {
        if (c1 == null || c2 == null) return false
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isBeforeDay(c1: Calendar, c2: Calendar): Boolean {
        val y1 = c1.get(Calendar.YEAR)
        val y2 = c2.get(Calendar.YEAR)
        return if (y1 != y2) y1 < y2 else c1.get(Calendar.DAY_OF_YEAR) < c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isAfterDay(c1: Calendar, c2: Calendar): Boolean {
        val y1 = c1.get(Calendar.YEAR)
        val y2 = c2.get(Calendar.YEAR)
        return if (y1 != y2) y1 > y2 else c1.get(Calendar.DAY_OF_YEAR) > c2.get(Calendar.DAY_OF_YEAR)
    }
}
