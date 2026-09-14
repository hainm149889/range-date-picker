package com.nitrolunarrangepicker

import java.util.Calendar

enum class RangePosition {
    NONE, SINGLE, START, MIDDLE, END
}

sealed class CalendarItem {
    data class MonthHeader(val year: Int, val month: Int, val title: String) : CalendarItem()
    data class Day(val date: Calendar?) : CalendarItem()
}
