package com.nitrolunarrangepicker

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.margelo.nitro.nitrolunarrangepicker.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.concurrent.thread

enum class RangePosition {
    NONE, SINGLE, START, MIDDLE, END
}

sealed class CalendarItem {
    data class MonthHeader(val year: Int, val month: Int, val title: String) : CalendarItem()
    data class Day(val date: Calendar?) : CalendarItem()
}

class LunarRangePickerView(context: Context) : LinearLayout(context) {

    var language: PickerLanguage = PickerLanguage.VI
        set(value) {
            field = value
            setupWeekdayHeader()
            rebuildCalendarData()
        }

    var theme: PickerTheme? = null
        set(value) {
            field = value
            applyTheme()
        }

    var showLunarDate: Boolean = true
        set(value) {
            field = value
            adapter.notifyDataSetChanged()
        }

    var firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY
        set(value) {
            field = value
            setupWeekdayHeader()
            rebuildCalendarData()
        }

    var displayMode: DisplayMode = DisplayMode.MULTI
        set(value) {
            field = value
            rebuildCalendarData()
        }

    var numberOfMonths: Double = 12.0
        set(value) {
            field = value
            rebuildCalendarData()
        }

    var startDate: String? = null
        set(value) {
            field = value
            selectedStartDate = parseDateString(value)
            if (displayMode == DisplayMode.SINGLE) {
                rebuildCalendarData()
            } else {
                adapter.notifyDataSetChanged()
                scrollToSelectedDate()
            }
        }

    var endDate: String? = null
        set(value) {
            field = value
            selectedEndDate = parseDateString(value)
            adapter.notifyDataSetChanged()
        }

    var minDate: String? = null
        set(value) {
            field = value
            parsedMinDate = parseDateString(value)
            adapter.notifyDataSetChanged()
        }

    var maxDate: String? = null
        set(value) {
            field = value
            parsedMaxDate = parseDateString(value)
            adapter.notifyDataSetChanged()
        }

    var closeIconUri: String? = null
        set(value) {
            field = value
            loadCloseIcon()
        }

    var confirmIconUri: String? = null

    var onConfirm: ((DateRangeResult) -> Unit)? = null
    var onClose: (() -> Unit)? = null

    private var parsedMinDate: Calendar? = null
    private var parsedMaxDate: Calendar? = null
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    private val items = ArrayList<CalendarItem>()
    private val adapter: CalendarAdapter
    private val layoutManager: GridLayoutManager

    // UI elements
    private val topBarLayout: FrameLayout
    private val titleTextView: TextView
    private val closeButton: TextView
    private val closeImageView: ImageView
    private val weekdayHeaderLayout: LinearLayout
    private val recyclerView: RecyclerView

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.WHITE)

        val dp = context.resources.displayMetrics.density

        // 1. Top Bar (44dp)
        topBarLayout = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (44 * dp).toInt())
        }

        titleTextView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
                marginStart = (44 * dp).toInt()
                marginEnd = (44 * dp).toInt()
            }
            gravity = Gravity.CENTER
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            text = getLocalizedTitle()
        }
        topBarLayout.addView(titleTextView)

        closeButton = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt()).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            gravity = Gravity.CENTER
            text = "✕"
            textSize = 20f
            setTextColor(Color.GRAY)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClose?.invoke() }
        }
        topBarLayout.addView(closeButton)
        closeButton.bringToFront()

        closeImageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt()).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            visibility = GONE
            isClickable = true
            isFocusable = true
            setOnClickListener { onClose?.invoke() }
        }
        topBarLayout.addView(closeImageView)
        closeImageView.bringToFront()

        addView(topBarLayout)

        // 2. Weekday Header (30dp)
        weekdayHeaderLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (30 * dp).toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(weekdayHeaderLayout)
        setupWeekdayHeader()

        // 3. RecyclerView with 7 columns
        layoutManager = GridLayoutManager(context, 7).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (position < items.size && items[position] is CalendarItem.MonthHeader) 7 else 1
                }
            }
        }

        adapter = CalendarAdapter()
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
            layoutManager = this@LunarRangePickerView.layoutManager
            adapter = this@LunarRangePickerView.adapter
            clipToPadding = false
            setPadding(0, (8 * dp).toInt(), 0, (20 * dp).toInt())
        }
        addView(recyclerView)

        rebuildCalendarData()
    }

    private val measureAndLayout = Runnable {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(left, top, right, bottom)
    }

    override fun requestLayout() {
        super.requestLayout()
        post(measureAndLayout)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (displayMode == DisplayMode.SINGLE) {
            rebuildCalendarData()
        } else {
            adapter.notifyDataSetChanged()
            if (selectedStartDate != null) {
                post { scrollToSelectedDate() }
            }
        }
    }

    private fun getLocalizedTitle(): String {
        return when (language) {
            PickerLanguage.VI -> "Chọn ngày"
            PickerLanguage.ZH -> "选择日期"
            PickerLanguage.EN -> "Select Dates"
        }
    }

    private fun setupWeekdayHeader() {
        weekdayHeaderLayout.removeAllViews()
        val isMonFirst = (firstDayOfWeek == FirstDayOfWeek.MONDAY)
        val names: Array<String> = when (language) {
            PickerLanguage.VI -> if (isMonFirst) arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN") else arrayOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
            PickerLanguage.ZH -> if (isMonFirst) arrayOf("一", "二", "三", "四", "五", "六", "日") else arrayOf("日", "一", "二", "三", "四", "五", "六")
            PickerLanguage.EN -> if (isMonFirst) arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun") else arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        }

        for (i in names.indices) {
            val tv = TextView(context).apply {
                layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
                gravity = Gravity.CENTER
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                text = names[i]
                val isSunday = if (isMonFirst) (i == 6) else (i == 0)
                setTextColor(if (isSunday) Color.parseColor("#E53935") else Color.parseColor("#8E8E93"))
            }
            weekdayHeaderLayout.addView(tv)
        }
    }

    private fun applyTheme() {
        theme?.backgroundColor?.let {
            val color = parseHexColor(it, Color.WHITE)
            setBackgroundColor(color)
        }
        theme?.textColor?.let {
            val color = parseHexColor(it, Color.BLACK)
            titleTextView.setTextColor(color)
        }
        titleTextView.text = getLocalizedTitle()
        adapter.notifyDataSetChanged()
    }

    private fun loadCloseIcon() {
        val uri = closeIconUri ?: return
        thread {
            try {
                val url = URL(uri)
                val bitmap = BitmapFactory.decodeStream(url.openStream())
                post {
                    closeImageView.setImageBitmap(bitmap)
                    closeImageView.visibility = VISIBLE
                    closeButton.visibility = GONE
                }
            } catch (e: Exception) {
                // Keep default text button
            }
        }
    }

    private fun parseDateString(str: String?): Calendar? {
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

    private fun cleanToStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    fun scrollToSelectedDate() {
        if (displayMode != DisplayMode.MULTI) return
        val start = selectedStartDate ?: return
        val targetYear = start.get(Calendar.YEAR)
        val targetMonth = start.get(Calendar.MONTH) + 1

        val index = items.indexOfFirst {
            it is CalendarItem.MonthHeader && it.year == targetYear && it.month == targetMonth
        }
        if (index > 0) {
            layoutManager.scrollToPositionWithOffset(index, 0)
        }
    }

    private fun rebuildCalendarData() {
        items.clear()
        val today = Calendar.getInstance()
        cleanToStartOfDay(today)

        val totalMonths = if (displayMode == DisplayMode.MULTI) maxOf(1, numberOfMonths.toInt()) else 1
        val baseCal = (if (displayMode == DisplayMode.SINGLE && selectedStartDate != null) selectedStartDate!!.clone() as Calendar else today.clone() as Calendar)

        for (i in 0 until totalMonths) {
            val monthCal = baseCal.clone() as Calendar
            monthCal.add(Calendar.MONTH, -i)
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            cleanToStartOfDay(monthCal)

            val year = monthCal.get(Calendar.YEAR)
            val month = monthCal.get(Calendar.MONTH) + 1
            val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // Month Header
            val monthTitle = when (language) {
                PickerLanguage.VI -> "Tháng $month, $year"
                PickerLanguage.ZH -> "${year}年 ${month}月"
                PickerLanguage.EN -> {
                    val names = arrayOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                    "${names[month]} $year"
                }
            }
            items.add(CalendarItem.MonthHeader(year, month, monthTitle))

            // Leading empty days
            val firstDayOfWeekInt = monthCal.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
            val isMonFirst = (firstDayOfWeek == FirstDayOfWeek.MONDAY)
            val leadingEmptyCount = if (isMonFirst) {
                (firstDayOfWeekInt - 2 + 7) % 7
            } else {
                (firstDayOfWeekInt - 1) % 7
            }

            for (empty in 0 until leadingEmptyCount) {
                items.add(CalendarItem.Day(null))
            }

            // Days in month
            for (day in 1..maxDay) {
                val dayCal = monthCal.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, day)
                cleanToStartOfDay(dayCal)
                items.add(CalendarItem.Day(dayCal))
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun isSameDay(c1: Calendar?, c2: Calendar?): Boolean {
        if (c1 == null || c2 == null) return false
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isBeforeDay(c1: Calendar, c2: Calendar): Boolean {
        val y1 = c1.get(Calendar.YEAR)
        val y2 = c2.get(Calendar.YEAR)
        return if (y1 != y2) y1 < y2 else c1.get(Calendar.DAY_OF_YEAR) < c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isAfterDay(c1: Calendar, c2: Calendar): Boolean {
        val y1 = c1.get(Calendar.YEAR)
        val y2 = c2.get(Calendar.YEAR)
        return if (y1 != y2) y1 > y2 else c1.get(Calendar.DAY_OF_YEAR) > c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun getRangePosition(date: Calendar): RangePosition {
        val start = selectedStartDate ?: return RangePosition.NONE
        val end = selectedEndDate

        if (end == null || isSameDay(start, end)) {
            return if (isSameDay(date, start)) RangePosition.SINGLE else RangePosition.NONE
        }

        val minCal = if (isBeforeDay(start, end)) start else end
        val maxCal = if (isAfterDay(start, end)) start else end

        return when {
            isSameDay(date, minCal) -> RangePosition.START
            isSameDay(date, maxCal) -> RangePosition.END
            isAfterDay(date, minCal) && isBeforeDay(date, maxCal) -> RangePosition.MIDDLE
            else -> RangePosition.NONE
        }
    }

    private fun onDayClicked(date: Calendar, clickedCellView: DayCellView? = null) {
        val cleanDate = date.clone() as Calendar
        cleanToStartOfDay(cleanDate)

        parsedMaxDate?.let { if (isAfterDay(cleanDate, it)) return }
        parsedMinDate?.let { if (isBeforeDay(cleanDate, it)) return }

        val isRangeCompleted: Boolean
        val startCal: Calendar
        val endCal: Calendar

        if (selectedStartDate == null || (selectedStartDate != null && selectedEndDate != null)) {
            selectedStartDate = cleanDate
            selectedEndDate = null
            isRangeCompleted = false
            startCal = cleanDate
            endCal = cleanDate
        } else {
            val start = selectedStartDate!!
            if (isBeforeDay(cleanDate, start)) {
                selectedStartDate = cleanDate
                selectedEndDate = null
                isRangeCompleted = false
                startCal = cleanDate
                endCal = cleanDate
            } else {
                selectedEndDate = cleanDate
                isRangeCompleted = true
                startCal = start
                endCal = cleanDate
            }
        }

        // 1. Cập nhật tức thì ô cell vừa click ngay trong frame này (0ms delay)
        clickedCellView?.let { cell ->
            val newPos = getRangePosition(cleanDate)
            cell.configure(
                date = cleanDate,
                showLunar = showLunarDate,
                position = newPos,
                isDisabled = false,
                theme = theme,
                language = when (language) {
                    PickerLanguage.VI -> "vi"
                    PickerLanguage.ZH -> "zh"
                    PickerLanguage.EN -> "en"
                }
            )
        }

        // 2. Ép RecyclerView re-bind ngay lập tức tất cả các item trên màn hình
        adapter.notifyItemRangeChanged(0, items.size)
        recyclerView.invalidate()

        // 3. Nếu đã chọn xong khoảng ngày (start + end):
        if (isRangeCompleted) {
            postDelayed({
                dispatchConfirmResult(startCal, endCal)
            }, 300)
        }
    }

    private fun dispatchConfirmResult(start: Calendar, end: Calendar) {
        val langStr = when (language) {
            PickerLanguage.VI -> "vi"
            PickerLanguage.ZH -> "zh"
            PickerLanguage.EN -> "en"
        }

        val startLunar = LunarCalendarHelper.convertSolarToLunar(start, langStr)
        val endLunar = LunarCalendarHelper.convertSolarToLunar(end, langStr)

        val startDateInfo = DateInfo(
            year = start.get(Calendar.YEAR).toDouble(),
            month = (start.get(Calendar.MONTH) + 1).toDouble(),
            day = start.get(Calendar.DAY_OF_MONTH).toDouble(),
            timestamp = start.timeInMillis.toDouble(),
            lunarDay = startLunar.day.toDouble(),
            lunarMonth = startLunar.month.toDouble(),
            lunarYear = startLunar.year.toDouble(),
            isLeapMonth = startLunar.isLeap,
            lunarDayName = startLunar.lunarDayName
        )

        val endDateInfo = DateInfo(
            year = end.get(Calendar.YEAR).toDouble(),
            month = (end.get(Calendar.MONTH) + 1).toDouble(),
            day = end.get(Calendar.DAY_OF_MONTH).toDouble(),
            timestamp = end.timeInMillis.toDouble(),
            lunarDay = endLunar.day.toDouble(),
            lunarMonth = endLunar.month.toDouble(),
            lunarYear = endLunar.year.toDouble(),
            isLeapMonth = endLunar.isLeap,
            lunarDayName = endLunar.lunarDayName
        )

        onConfirm?.invoke(DateRangeResult(startDate = startDateInfo, endDate = endDateInfo))
    }

    private fun parseHexColor(colorStr: String?, defaultColor: Int): Int {
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

    // MARK: - RecyclerView Adapter
    private inner class CalendarAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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
    }

    private inner class MonthHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: CalendarItem.MonthHeader) {
            val tv = itemView as TextView
            tv.text = item.title
            theme?.textColor?.let {
                tv.setTextColor(parseHexColor(it, Color.BLACK))
            }
        }
    }

    private inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: CalendarItem.Day) {
            val cellView = itemView as DayCellView
            val date = item.date
            if (date == null) {
                cellView.clear()
                return
            }

            var isDisabled = false
            parsedMaxDate?.let { if (isAfterDay(date, it)) isDisabled = true }
            parsedMinDate?.let { if (isBeforeDay(date, it)) isDisabled = true }

            val position = getRangePosition(date)
            cellView.configure(
                date = date,
                showLunar = showLunarDate,
                position = position,
                isDisabled = isDisabled,
                theme = theme,
                language = when (language) {
                    PickerLanguage.VI -> "vi"
                    PickerLanguage.ZH -> "zh"
                    PickerLanguage.EN -> "en"
                }
            )

            cellView.setOnClickListener {
                if (!isDisabled) {
                    onDayClicked(date, cellView)
                }
            }
        }
    }

    // MARK: - DayCellView
    private inner class DayCellView(context: Context) : FrameLayout(context) {
        private val highlightContainer = LinearLayout(context)
        private val leftHighlightView = View(context)
        private val rightHighlightView = View(context)
        private val circleBackgroundView = View(context)
        private val solarTextView = TextView(context)
        private val lunarTextView = TextView(context)
        private val textContainer = LinearLayout(context)

        private val circleDrawable = GradientDrawable()
        private val dp = context.resources.displayMetrics.density

        init {
            val cellHeight = (48 * dp).toInt()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, cellHeight)

            val badgeSize = (38 * dp).toInt()
            val badgeMarginY = (cellHeight - badgeSize) / 2

            // 1. Highlight Container (2 nửa trái & phải để không bao giờ bị tràn mép)
            highlightContainer.orientation = LinearLayout.HORIZONTAL
            highlightContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, badgeSize).apply {
                gravity = Gravity.CENTER_VERTICAL
                topMargin = badgeMarginY
                bottomMargin = badgeMarginY
            }

            leftHighlightView.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            rightHighlightView.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            leftHighlightView.visibility = INVISIBLE
            rightHighlightView.visibility = INVISIBLE

            highlightContainer.addView(leftHighlightView)
            highlightContainer.addView(rightHighlightView)
            addView(highlightContainer)

            // 2. Circle Badge
            circleBackgroundView.layoutParams = LayoutParams(badgeSize, badgeSize).apply {
                gravity = Gravity.CENTER
            }
            circleDrawable.shape = GradientDrawable.OVAL
            circleBackgroundView.background = circleDrawable
            circleBackgroundView.visibility = INVISIBLE
            addView(circleBackgroundView)

            // 3. Solar & Lunar Text Container
            textContainer.orientation = LinearLayout.VERTICAL
            textContainer.gravity = Gravity.CENTER
            textContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            solarTextView.apply {
                gravity = Gravity.CENTER
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
            }

            lunarTextView.apply {
                gravity = Gravity.CENTER
                textSize = 9f
                typeface = Typeface.DEFAULT
                includeFontPadding = false
                val lp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = (1 * dp).toInt() // 1dp spacing
                }
                layoutParams = lp
            }

            textContainer.addView(solarTextView)
            textContainer.addView(lunarTextView)
            addView(textContainer)
        }

        fun clear() {
            solarTextView.text = ""
            lunarTextView.text = ""
            leftHighlightView.visibility = INVISIBLE
            rightHighlightView.visibility = INVISIBLE
            circleBackgroundView.visibility = INVISIBLE
            isClickable = false
        }

        fun configure(
            date: Calendar,
            showLunar: Boolean,
            position: RangePosition,
            isDisabled: Boolean,
            theme: PickerTheme?,
            language: String
        ) {
            val day = date.get(Calendar.DAY_OF_MONTH)
            solarTextView.text = day.toString()

            val lunar = LunarCalendarHelper.convertSolarToLunar(date, language)
            lunarTextView.visibility = if (showLunar) VISIBLE else GONE
            lunarTextView.text = lunar.lunarDayName

            // Colors
            val primaryColor = parseHexColor(theme?.primaryColor, Color.parseColor("#007AFF"))
            val selectedTextColor = parseHexColor(theme?.selectedTextColor, Color.WHITE)
            val textColor = parseHexColor(theme?.textColor, Color.BLACK)
            val specialColor = parseHexColor(theme?.specialDayColor, Color.parseColor("#FF3B30"))

            val defaultRangeBg = Color.argb(46, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
            val rangeBgColor = parseHexColor(theme?.rangeColor, defaultRangeBg)

            circleDrawable.setColor(primaryColor)
            leftHighlightView.setBackgroundColor(rangeBgColor)
            rightHighlightView.setBackgroundColor(rangeBgColor)

            if (isDisabled) {
                isClickable = false
                leftHighlightView.visibility = INVISIBLE
                rightHighlightView.visibility = INVISIBLE
                circleBackgroundView.visibility = INVISIBLE
                solarTextView.setTextColor(Color.parseColor("#BDBDBD"))
                lunarTextView.setTextColor(Color.parseColor("#BDBDBD"))
                lunarTextView.typeface = Typeface.DEFAULT
                return
            }

            isClickable = true

            // Layout highlight bar depending on position
            when (position) {
                RangePosition.NONE -> {
                    leftHighlightView.visibility = INVISIBLE
                    rightHighlightView.visibility = INVISIBLE
                    circleBackgroundView.visibility = INVISIBLE
                    solarTextView.setTextColor(textColor)
                    if (lunar.isSpecialDay) {
                        lunarTextView.setTextColor(specialColor)
                        lunarTextView.typeface = Typeface.DEFAULT_BOLD
                    } else {
                        lunarTextView.setTextColor(Color.parseColor("#8E8E93"))
                        lunarTextView.typeface = Typeface.DEFAULT
                    }
                }
                RangePosition.SINGLE -> {
                    leftHighlightView.visibility = INVISIBLE
                    rightHighlightView.visibility = INVISIBLE
                    circleBackgroundView.visibility = VISIBLE
                    solarTextView.setTextColor(selectedTextColor)
                    lunarTextView.setTextColor(selectedTextColor)
                    lunarTextView.typeface = Typeface.DEFAULT
                }
                RangePosition.START -> {
                    // Nửa trái trống, chỉ nửa phải kéo dài sang ngày tiếp theo, badge tròn che tâm
                    leftHighlightView.visibility = INVISIBLE
                    rightHighlightView.visibility = VISIBLE
                    circleBackgroundView.visibility = VISIBLE
                    solarTextView.setTextColor(selectedTextColor)
                    lunarTextView.setTextColor(selectedTextColor)
                    lunarTextView.typeface = Typeface.DEFAULT
                }
                RangePosition.MIDDLE -> {
                    // Cả 2 nửa đều có highlight
                    leftHighlightView.visibility = VISIBLE
                    rightHighlightView.visibility = VISIBLE
                    circleBackgroundView.visibility = INVISIBLE
                    solarTextView.setTextColor(primaryColor)
                    lunarTextView.setTextColor(primaryColor)
                    lunarTextView.typeface = Typeface.DEFAULT
                }
                RangePosition.END -> {
                    // Nửa trái nhận highlight từ ngày trước đến, nửa phải trống, badge tròn che tâm
                    leftHighlightView.visibility = VISIBLE
                    rightHighlightView.visibility = INVISIBLE
                    circleBackgroundView.visibility = VISIBLE
                    solarTextView.setTextColor(selectedTextColor)
                    lunarTextView.setTextColor(selectedTextColor)
                    lunarTextView.typeface = Typeface.DEFAULT
                }
            }
        }
    }
}
