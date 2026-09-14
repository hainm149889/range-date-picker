package com.nitrolunarrangepicker

import android.graphics.Color

object ColorUtils {

    fun parseHexColor(colorStr: String?, defaultColor: Int): Int {
        if (colorStr.isNullOrBlank()) return defaultColor
        val trimmed = colorStr.trim()
        return try {
            if (trimmed.startsWith("rgba", ignoreCase = true) || trimmed.startsWith("rgb", ignoreCase = true)) {
                val numbers = trimmed.substringAfter("(").substringBefore(")").split(",")
                if (numbers.size >= 3) {
                    val r = numbers[0].trim().toInt()
                    val g = numbers[1].trim().toInt()
                    val b = numbers[2].trim().toInt()
                    val a = if (numbers.size >= 4) (numbers[3].trim().toFloat() * 255).toInt() else 255
                    return Color.argb(a.coerceIn(0, 255), r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
                }
            }
            val cleanHex = if (trimmed.startsWith("#")) trimmed.substring(1) else trimmed
            when (cleanHex.length) {
                6 -> Color.parseColor("#$cleanHex")
                8 -> {
                    val rr = cleanHex.substring(0, 2)
                    val gg = cleanHex.substring(2, 4)
                    val bb = cleanHex.substring(4, 6)
                    val aa = cleanHex.substring(6, 8)
                    Color.parseColor("#$aa$rr$gg$bb")
                }
                else -> Color.parseColor("#$cleanHex")
            }
        } catch (e: Exception) {
            defaultColor
        }
    }
}
