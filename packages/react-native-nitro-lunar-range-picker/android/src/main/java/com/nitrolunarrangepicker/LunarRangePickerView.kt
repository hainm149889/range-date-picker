package com.nitrolunarrangepicker

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.margelo.nitro.nitrolunarrangepicker.*
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread

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
            selectedStartDate = DateUtils.parseDateString(value)
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
            selectedEndDate = DateUtils.parseDateString(value)
            adapter.notifyDataSetChanged()
        }

    var minDate: String? = null
        set(value) {
            field = value
            parsedMinDate = DateUtils.parseDateString(value)
            adapter.notifyDataSetChanged()
        }

    var maxDate: String? = null
        set(value) {
            field = value
            parsedMaxDate = DateUtils.parseDateString(value)
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
                marginStart = (48 * dp).toInt()
                marginEnd = (48 * dp).toInt()
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

        adapter = CalendarAdapter(
            context = context,
            items = items,
            getTheme = { theme },
            getShowLunarDate = { showLunarDate },
            getLanguageCode = { getLanguageCode() },
            getParsedMinDate = { parsedMinDate },
            getParsedMaxDate = { parsedMaxDate },
            getRangePosition = { date -> getRangePosition(date) },
            onDayClicked = { date, cellView -> onDayClicked(date, cellView) }
        )

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

    private fun getLanguageCode(): String {
        return when (language) {
            PickerLanguage.VI -> "vi"
            PickerLanguage.ZH -> "zh"
            PickerLanguage.EN -> "en"
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
            val color = ColorUtils.parseHexColor(it, Color.WHITE)
            setBackgroundColor(color)
        }
        theme?.textColor?.let {
            val color = ColorUtils.parseHexColor(it, Color.BLACK)
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
        DateUtils.cleanToStartOfDay(today)

        val totalMonths = if (displayMode == DisplayMode.MULTI) maxOf(1, numberOfMonths.toInt()) else 1
        val baseCal = (if (displayMode == DisplayMode.SINGLE && selectedStartDate != null) selectedStartDate!!.clone() as Calendar else today.clone() as Calendar)

        for (i in 0 until totalMonths) {
            val monthCal = baseCal.clone() as Calendar
            monthCal.add(Calendar.MONTH, -i)
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            DateUtils.cleanToStartOfDay(monthCal)

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
                DateUtils.cleanToStartOfDay(dayCal)
                items.add(CalendarItem.Day(dayCal))
            }
        }

        adapter.notifyDataSetChanged()
    }

    private fun getRangePosition(date: Calendar): RangePosition {
        val start = selectedStartDate ?: return RangePosition.NONE
        val end = selectedEndDate

        if (end == null || DateUtils.isSameDay(start, end)) {
            return if (DateUtils.isSameDay(date, start)) RangePosition.SINGLE else RangePosition.NONE
        }

        val minCal = if (DateUtils.isBeforeDay(start, end)) start else end
        val maxCal = if (DateUtils.isAfterDay(start, end)) start else end

        return when {
            DateUtils.isSameDay(date, minCal) -> RangePosition.START
            DateUtils.isSameDay(date, maxCal) -> RangePosition.END
            DateUtils.isAfterDay(date, minCal) && DateUtils.isBeforeDay(date, maxCal) -> RangePosition.MIDDLE
            else -> RangePosition.NONE
        }
    }

    private fun onDayClicked(date: Calendar, clickedCellView: DayCellView? = null) {
        val cleanDate = date.clone() as Calendar
        DateUtils.cleanToStartOfDay(cleanDate)

        parsedMaxDate?.let { if (DateUtils.isAfterDay(cleanDate, it)) return }
        parsedMinDate?.let { if (DateUtils.isBeforeDay(cleanDate, it)) return }

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
            if (DateUtils.isBeforeDay(cleanDate, start)) {
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
                language = getLanguageCode()
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
        val langStr = getLanguageCode()

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
}
