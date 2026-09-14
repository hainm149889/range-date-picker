package com.nitrolunarrangepicker

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.margelo.nitro.nitrolunarrangepicker.PickerTheme
import java.util.Calendar

class CalendarAdapter(
    private val context: Context,
    private val items: List<CalendarItem>,
    private val getTheme: () -> PickerTheme?,
    private val getShowLunarDate: () -> Boolean,
    private val getLanguageCode: () -> String,
    private val getParsedMinDate: () -> Calendar?,
    private val getParsedMaxDate: () -> Calendar?,
    private val getRangePosition: (Calendar) -> RangePosition,
    private val onDayClicked: (Calendar, DayCellView) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is CalendarItem.MonthHeader -> 0
            is CalendarItem.Day -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == 0) {
            val tv = TextView(context).apply {
                val dp = context.resources.displayMetrics.density
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (38 * dp).toInt()
                )
                setPadding((16 * dp).toInt(), (8 * dp).toInt(), (16 * dp).toInt(), 0)
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.BLACK)
                gravity = Gravity.CENTER_VERTICAL
            }
            MonthHeaderViewHolder(tv)
        } else {
            val dayView = DayCellView(context)
            DayViewHolder(dayView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is CalendarItem.MonthHeader -> {
                (holder as MonthHeaderViewHolder).bind(item)
            }
            is CalendarItem.Day -> {
                (holder as DayViewHolder).bind(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    inner class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: CalendarItem.MonthHeader) {
            val tv = itemView as TextView
            tv.text = item.title
            getTheme()?.textColor?.let {
                tv.setTextColor(ColorUtils.parseHexColor(it, Color.BLACK))
            }
        }
    }

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: CalendarItem.Day) {
            val cellView = itemView as DayCellView
            val date = item.date
            if (date == null) {
                cellView.clear()
                return
            }

            var isDisabled = false
            getParsedMaxDate()?.let { if (DateUtils.isAfterDay(date, it)) isDisabled = true }
            getParsedMinDate()?.let { if (DateUtils.isBeforeDay(date, it)) isDisabled = true }

            val position = getRangePosition(date)
            cellView.configure(
                date = date,
                showLunar = getShowLunarDate(),
                position = position,
                isDisabled = isDisabled,
                theme = getTheme(),
                language = getLanguageCode()
            )

            cellView.setOnClickListener {
                if (!isDisabled) {
                    onDayClicked(date, cellView)
                }
            }
        }
    }
}
