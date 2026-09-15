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

/// LunarRangePickerView: Giao diện chính của bộ chọn lịch âm dương trên Android (kế thừa LinearLayout dạng đứng).
/// Quản lý: TopBar (nút Close & Title), Weekday Header (T2..CN), RecyclerView dạng lưới 7 cột chia theo tháng,
/// và thuật toán chọn khoảng ngày âm dương, theme màu sắc.
class LunarRangePickerView(context: Context) : LinearLayout(context) {

    // =========================================================================
    // MARK: - Properties từ Nitro Hybrid View (Giao tiếp với React Native)
    // =========================================================================

    /// Ngôn ngữ hiển thị (PickerLanguage.VI, EN, ZH). Khi đổi:
    /// - Vẽ lại thanh thứ trong tuần (T2..CN hoặc Mon..Sun)
    /// - Tạo lại danh sách các tháng và tiêu đề tháng tương ứng
    var language: PickerLanguage = PickerLanguage.VI
        set(value) {
            field = value
            setupWeekdayHeader()
            rebuildCalendarData()
        }

    /// Theme màu sắc truyền từ React Native (primaryColor, backgroundColor, textColor...)
    var theme: PickerTheme? = null
        set(value) {
            field = value
            applyTheme()
        }

    /// Cờ bật/tắt hiển thị dòng chữ ngày âm lịch
    var showLunarDate: Boolean = true
        set(value) {
            field = value
            adapter.notifyDataSetChanged()
        }

    /// Ngày bắt đầu tuần (MONDAY hoặc SUNDAY)
    var firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY
        set(value) {
            field = value
            setupWeekdayHeader()
            rebuildCalendarData()
        }

    /// Chế độ hiển thị: MULTI (cuộn dọc nhiều tháng) hoặc SINGLE (chỉ hiển thị 1 tháng)
    var displayMode: DisplayMode = DisplayMode.MULTI
        set(value) {
            field = value
            rebuildCalendarData()
        }

    /// Số lượng tháng hiển thị trong chế độ MULTI (Mặc định: 12 tháng)
    /// -> Muốn đổi số tháng: truyền prop numberOfMonths từ React Native
    var numberOfMonths: Double = 12.0
        set(value) {
            field = value
            rebuildCalendarData()
        }

    /// Ngày bắt đầu được chọn (chuỗi ISO hoặc YYYY-MM-DD)
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

    /// Ngày kết thúc được chọn (chuỗi ISO hoặc YYYY-MM-DD)
    var endDate: String? = null
        set(value) {
            field = value
            selectedEndDate = DateUtils.parseDateString(value)
            adapter.notifyDataSetChanged()
        }

    /// Ngày tối thiểu cho phép chọn (các ngày trước minDate sẽ bị disable)
    var minDate: String? = null
        set(value) {
            field = value
            parsedMinDate = DateUtils.parseDateString(value)
            adapter.notifyDataSetChanged()
        }

    /// Ngày tối đa cho phép chọn (các ngày sau maxDate sẽ bị disable)
    var maxDate: String? = null
        set(value) {
            field = value
            parsedMaxDate = DateUtils.parseDateString(value)
            adapter.notifyDataSetChanged()
        }

    /// Đường dẫn URI ảnh icon Close tùy chỉnh từ JS (local asset hoặc link mạng)
    var closeIconUri: String? = null
        set(value) {
            field = value
            loadCloseIcon()
        }

    /// Đường dẫn URI icon Confirm tùy chỉnh
    var confirmIconUri: String? = null

    /// Callback gọi về React Native khi chọn xong ngày (trả về DateRangeResult)
    var onConfirm: ((DateRangeResult) -> Unit)? = null
    
    /// Callback gọi về React Native khi bấm nút Close
    var onClose: (() -> Unit)? = null

    // =========================================================================
    // MARK: - State nội bộ (Internal State)
    // =========================================================================

    private var parsedMinDate: Calendar? = null
    private var parsedMaxDate: Calendar? = null
    private var selectedStartDate: Calendar? = null
    private var selectedEndDate: Calendar? = null

    /// Danh sách các item trong danh sách (gồm MonthHeader và Day)
    private val items = ArrayList<CalendarItem>()
    private val adapter: CalendarAdapter
    private val layoutManager: GridLayoutManager

    // =========================================================================
    // MARK: - Khai báo UI Elements
    // =========================================================================

    /// Thanh tiêu đề phía trên cùng
    private val topBarLayout: FrameLayout
    
    /// Nhãn hiển thị tiêu đề ("Chọn ngày")
    private val titleTextView: TextView
    
    /// Nút Close mặc định dạng chữ TextView ("✕")
    private val closeButton: TextView
    
    /// Nút Close dạng hình ảnh ImageView (hiển thị khi có closeIconUri)
    private val closeImageView: ImageView
    
    /// Thanh ngang chứa 7 thứ trong tuần (T2..CN)
    private val weekdayHeaderLayout: LinearLayout
    
    /// Danh sách hiển thị lịch 7 cột
    private val recyclerView: RecyclerView

    /// Fix lỗi hiển thị layout trong React Native Fabric / Paper khi view con cần đo lại kích thước
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

    // =========================================================================
    // MARK: - Khởi tạo Giao diện & Hằng số Kích thước (Init & Layout Dimensions)
    // =========================================================================

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.WHITE)

        // Hệ số quy đổi dp sang pixel
        val dp = context.resources.displayMetrics.density

        // --- 1. THANH TIÊU ĐỀ TOP BAR (44dp) ---
        // - Chiều cao: (44 * dp).toInt() = 44dp (tương đương chuẩn UINavigationBar iOS)
        // -> Muốn thanh bar cao hơn: tăng lên 48 hoặc 52dp
        topBarLayout = FrameLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (44 * dp).toInt())
        }

        // Nhãn Tiêu đề (Title):
        titleTextView = TextView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
                // marginStart & marginEnd = 48dp: Chừa 48dp hai bên lề
                // để tiêu đề luôn nằm chính giữa thanh bar và không bị đè lên nút Close
                marginStart = (48 * dp).toInt()
                marginEnd = (48 * dp).toInt()
            }
            gravity = Gravity.CENTER
            // - textSize = 16f: Cỡ chữ tiêu đề 16sp
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            text = getLocalizedTitle()
        }
        topBarLayout.addView(titleTextView)

        // Nút Đóng Close mặc định (dạng chữ TextView):
        closeButton = TextView(context).apply {
            // - width = 48dp, height = 48dp: Diện tích chạm chuẩn 48dp của Android Accessibility
            layoutParams = FrameLayout.LayoutParams((48 * dp).toInt(), (48 * dp).toInt()).apply {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            gravity = Gravity.CENTER
            // - text = "✕": Ký tự đóng mặc định
            // -> Muốn đổi ký tự đóng (ví dụ chữ "X" hoặc "Đóng"): sửa tại đây
            text = "✕"
            // - textSize = 20f: Cỡ chữ ký tự đóng 20sp
            // -> Muốn ký tự to/nhỏ hơn: sửa 20f
            textSize = 20f
            // - setTextColor(Color.GRAY): Màu xám mặc định
            setTextColor(Color.GRAY)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClose?.invoke() }
        }
        topBarLayout.addView(closeButton)
        closeButton.bringToFront()

        // Nút Đóng Close dạng ảnh (ImageView):
        // Mặc định ẩn (GONE), chỉ hiện khi người dùng truyền prop closeIconUri
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

        // --- 2. THANH THỨ TRONG TUẦN (WEEKDAY HEADER) ---
        // - Chiều cao: (30 * dp).toInt() = 30dp
        // -> Muốn thanh thứ dày hơn: tăng 30 lên 34 hoặc 36dp
        weekdayHeaderLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (30 * dp).toInt())
            gravity = Gravity.CENTER_VERTICAL
        }
        addView(weekdayHeaderLayout)
        setupWeekdayHeader()

        // --- 3. LƯỚI LỊCH RECYCLERVIEW VỚI 7 CỘT ---
        // Sử dụng GridLayoutManager với số cột cố định spanCount = 7
        layoutManager = GridLayoutManager(context, 7).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    // Nếu là MonthHeader: chiếm trọn 7 cột (full-width)
                    // Nếu là Day: chiếm 1 cột
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
            // - top padding = 8dp: Đệm trên đầu danh sách
            // - bottom padding = 20dp: Đệm dưới đáy danh sách
            setPadding(0, (8 * dp).toInt(), 0, (20 * dp).toInt())
        }
        addView(recyclerView)

        rebuildCalendarData()
    }

    /// Khi View gắn vào màn hình: tự động cuộn đến tháng của ngày được chọn
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

    /// Xây dựng thanh nhãn thứ trong tuần (T2..CN hoặc CN..T7)
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
                // - textSize = 12f: Cỡ chữ thứ 12sp
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                text = names[i]
                val isSunday = if (isMonFirst) (i == 6) else (i == 0)
                // Chủ Nhật: Đỏ #E53935, các ngày khác: Xám #8E8E93
                setTextColor(if (isSunday) Color.parseColor("#E53935") else Color.parseColor("#8E8E93"))
            }
            weekdayHeaderLayout.addView(tv)
        }
    }

    /// Áp dụng màu sắc giao diện (Theme) khi nhận được từ React Native
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

    /// Tải hình ảnh icon Close từ URI chạy trên background thread
    private fun loadCloseIcon() {
        val uri = closeIconUri ?: return
        thread {
            try {
                val url = URL(uri)
                val bitmap = BitmapFactory.decodeStream(url.openStream())
                post {
                    closeImageView.setImageBitmap(bitmap)
                    closeImageView.visibility = VISIBLE
                    closeButton.visibility = GONE // Ẩn nút text mặc định khi đã có ảnh
                }
            } catch (e: Exception) {
                // Nếu lỗi thì giữ nguyên nút text "✕"
            }
        }
    }

    /// Cuộn danh sách đến vị trí tháng của ngày bắt đầu
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

    // =========================================================================
    // MARK: - Tạo dữ liệu Lịch (Calendar Data Generation)
    // =========================================================================

    /// Sinh danh sách dữ liệu các tháng và các ngày tương ứng
    private fun rebuildCalendarData() {
        items.clear()
        val today = Calendar.getInstance()
        DateUtils.cleanToStartOfDay(today)

        val totalMonths = if (displayMode == DisplayMode.MULTI) maxOf(1, numberOfMonths.toInt()) else 1
        val baseCal = (if (displayMode == DisplayMode.SINGLE && selectedStartDate != null) selectedStartDate!!.clone() as Calendar else today.clone() as Calendar)

        // Duyệt lùi numberOfMonths tháng bắt đầu từ baseCal (-i)
        // -> Muốn duyệt tiến về tương lai: đổi -i thành +i
        for (i in 0 until totalMonths) {
            val monthCal = baseCal.clone() as Calendar
            monthCal.add(Calendar.MONTH, -i)
            monthCal.set(Calendar.DAY_OF_MONTH, 1)
            DateUtils.cleanToStartOfDay(monthCal)

            val year = monthCal.get(Calendar.YEAR)
            val month = monthCal.get(Calendar.MONTH) + 1
            val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)

            // 1. Thêm Header của tháng vào danh sách
            val monthTitle = when (language) {
                PickerLanguage.VI -> "Tháng $month, $year"
                PickerLanguage.ZH -> "${year}年 ${month}月"
                PickerLanguage.EN -> {
                    val names = arrayOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                    "${names[month]} $year"
                }
            }
            items.add(CalendarItem.MonthHeader(year, month, monthTitle))

            // 2. Chèn các ô trống ở đầu tháng (Leading empty days)
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

            // 3. Thêm các ngày thực tế trong tháng
            for (day in 1..maxDay) {
                val dayCal = monthCal.clone() as Calendar
                dayCal.set(Calendar.DAY_OF_MONTH, day)
                DateUtils.cleanToStartOfDay(dayCal)
                items.add(CalendarItem.Day(dayCal))
            }
        }

        adapter.notifyDataSetChanged()
    }

    // =========================================================================
    // MARK: - Logic Phân Loại Vị Trí Khoảng Chọn (Range Position Logic)
    // =========================================================================

    private fun getRangePosition(date: Calendar): RangePosition {
        val start = selectedStartDate ?: return RangePosition.NONE
        val end = selectedEndDate

        // Chưa có ngày kết thúc hoặc 2 ngày trùng nhau -> SINGLE
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

    // =========================================================================
    // MARK: - Logic Xử Lý Click Chọn Ngày (Day Click Logic)
    // =========================================================================

    private fun onDayClicked(date: Calendar, clickedCellView: DayCellView? = null) {
        val cleanDate = date.clone() as Calendar
        DateUtils.cleanToStartOfDay(cleanDate)

        // Bỏ qua nếu ngày nằm ngoài minDate / maxDate
        parsedMaxDate?.let { if (DateUtils.isAfterDay(cleanDate, it)) return }
        parsedMinDate?.let { if (DateUtils.isBeforeDay(cleanDate, it)) return }

        val isRangeCompleted: Boolean
        val startCal: Calendar
        val endCal: Calendar

        // THUẬT TOÁN CHỌN KHOẢNG:
        // 1. Nếu chưa có start HOẶC đã có đủ cả start & end -> Bắt đầu lượt chọn mới
        if (selectedStartDate == null || (selectedStartDate != null && selectedEndDate != null)) {
            selectedStartDate = cleanDate
            selectedEndDate = null
            isRangeCompleted = false
            startCal = cleanDate
            endCal = cleanDate
        } else {
            val start = selectedStartDate!!
            // 2. Nếu người dùng bấm ngày nhỏ hơn start -> Đổi ngày đó làm start mới
            if (DateUtils.isBeforeDay(cleanDate, start)) {
                selectedStartDate = cleanDate
                selectedEndDate = null
                isRangeCompleted = false
                startCal = cleanDate
                endCal = cleanDate
            } else {
                // 3. Người dùng chọn ngày >= start -> Hoàn tất chọn khoảng!
                selectedEndDate = cleanDate
                isRangeCompleted = true
                startCal = start
                endCal = cleanDate
            }
        }

        // Cập nhật tức thì ô cell vừa bấm (0ms delay) để người dùng thấy phản hồi ngay
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

        // Cập nhật lại toàn bộ danh sách để vẽ dải màu nối
        adapter.notifyItemRangeChanged(0, items.size)
        recyclerView.invalidate()

        // Nếu đã hoàn tất chọn khoảng:
        // - delay 300ms: Cho người dùng nhìn thấy hiệu ứng dải màu được vẽ hoàn chỉnh
        // trước khi đóng modal hoặc trả kết quả về React Native
        // -> Muốn phản hồi nhanh hơn: giảm 300ms về 150ms hoặc 200ms
        if (isRangeCompleted) {
            postDelayed({
                dispatchConfirmResult(startCal, endCal)
            }, 300)
        }
    }

    /// Đóng gói dữ liệu kết quả DateRangeResult gửi về React Native
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
