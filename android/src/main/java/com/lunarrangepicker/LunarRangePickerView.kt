package com.lunarrangepicker

import android.content.Context
import android.graphics.Color
import android.widget.FrameLayout
import android.widget.ImageView
import com.facebook.react.bridge.Arguments
import java.util.Calendar

class LunarRangePickerView(context: Context) : FrameLayout(context) {

    private var props: LunarRangePickerProps? = null

    fun setProps(newProps: LunarRangePickerProps) {
        this.props = newProps
        renderUI()
    }

    private fun renderUI() {
        val currentProps = props ?: return

        // 1. Áp dụng Theme
        currentProps.theme?.backgroundColor?.let { hexColor ->
            try {
                setBackgroundColor(Color.parseColor(hexColor))
            } catch (e: Exception) {
                setBackgroundColor(Color.WHITE)
            }
        }
    }

    // Trigger callback về JS khi user bấm Confirm
    fun dispatchConfirm(startDate: Calendar, endDate: Calendar) {
        val currentProps = props ?: return
        val lang = currentProps.language ?: "vi"

        val startLunar = LunarCalendarHelper.convertSolarToLunar(startDate, lang)
        val endLunar = LunarCalendarHelper.convertSolarToLunar(endDate, lang)

        val startInfo = DateInfo(
            year = startDate.get(Calendar.YEAR).toDouble(),
            month = (startDate.get(Calendar.MONTH) + 1).toDouble(),
            day = startDate.get(Calendar.DAY_OF_MONTH).toDouble(),
            timestamp = startDate.timeInMillis.toDouble(),
            lunarDay = startLunar.day.toDouble(),
            lunarMonth = startLunar.month.toDouble(),
            lunarYear = startLunar.year.toDouble(),
            isLeapMonth = startLunar.isLeap,
            lunarDayName = startLunar.lunarDayName
        )

        val endInfo = DateInfo(
            year = endDate.get(Calendar.YEAR).toDouble(),
            month = (endDate.get(Calendar.MONTH) + 1).toDouble(),
            day = endDate.get(Calendar.DAY_OF_MONTH).toDouble(),
            timestamp = endDate.timeInMillis.toDouble(),
            lunarDay = endLunar.day.toDouble(),
            lunarMonth = endLunar.month.toDouble(),
            lunarYear = endLunar.year.toDouble(),
            isLeapMonth = endLunar.isLeap,
            lunarDayName = endLunar.lunarDayName
        )

        currentProps.onConfirm(DateRangeResult(startDate = startInfo, endDate = endInfo))
    }
}