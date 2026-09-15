package com.nitrolunarrangepicker

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.margelo.nitro.nitrolunarrangepicker.PickerTheme
import java.util.Calendar

/// DayCellView: Custom View vẽ một ô ngày đơn lẻ trong lưới lịch Android (tương đương DayCell bên iOS).
/// Quản lý: Dải màu kết nối khoảng chọn (chia 2 nửa trái/phải), Badge tròn nổi bật ngày được chọn, Chữ số ngày dương và Chữ ngày âm.
class DayCellView(context: Context) : FrameLayout(context) {
    
    // =========================================================================
    // MARK: - Khai báo View thành phần & Hằng số quy đổi
    // =========================================================================
    
    /// Layout ngang chứa 2 nửa dải màu highlight (nửa trái và nửa phải)
    private val highlightContainer = LinearLayout(context)
    
    /// Dải màu nửa bên trái của ô (dùng khi ô là MIDDLE hoặc END)
    private val leftHighlightView = View(context)
    
    /// Dải màu nửa bên phải của ô (dùng khi ô là START hoặc MIDDLE)
    private val rightHighlightView = View(context)
    
    /// View hình tròn/oval màu chủ đạo bọc ngày được chọn
    private val circleBackgroundView = View(context)
    
    /// TextView hiển thị số ngày dương lịch (1..31)
    private val solarTextView = TextView(context)
    
    /// TextView hiển thị ngày âm lịch phía dưới (ví dụ: "15", "1/8", "Tết")
    private val lunarTextView = TextView(context)
    
    /// Cột dọc chứa cả 2 TextView ngày dương và âm để căn giữa ô
    private val textContainer = LinearLayout(context)

    /// Drawable tạo hình tròn OVAL cho circleBackgroundView
    private val circleDrawable = GradientDrawable()
    
    /// Tỉ lệ mật độ điểm ảnh của màn hình thiết bị Android (dp to pixel factor).
    /// Mọi kích thước dp nhân với hệ số này để quy đổi ra Pixel thực tế trên màn hình.
    private val dp = context.resources.displayMetrics.density

    // =========================================================================
    // MARK: - Khởi tạo Giao diện & Thiết lập Hằng số Kích thước (Init & Layout Math)
    // =========================================================================
    
    init {
        // --- 1. CHIỀU CAO Ô NGÀY ---
        // - cellHeight = (48 * dp).toInt(): Chiều cao cố định của mỗi ô ngày là 48dp
        // (Tuân theo chuẩn kích thước chạm ngón tay tối thiểu 48dp của Material Design).
        // -> Muốn ô ngày cao hơn: tăng 48 lên 52 hoặc 56dp
        // -> Muốn ô ngày gọn hơn: giảm 48 về 42 hoặc 44dp
        val cellHeight = (48 * dp).toInt()
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, cellHeight)

        // --- 2. KÍCH THƯỚC VÒNG TRÒN CHỌN NGÀY (CIRCLE BADGE) ---
        // - badgeSize = (38 * dp).toInt(): Đường kính vòng tròn chọn ngày là 38dp
        // Với ô cao 48dp, badge 38dp sẽ chừa lại khoảng đệm 5dp ở đỉnh và đáy cực kỳ cân đối.
        // -> Muốn vòng tròn to hơn: tăng 38 lên 40 hoặc 42dp
        // -> Muốn vòng tròn nhỏ hơn: giảm 38 về 32 hoặc 34dp
        val badgeSize = (38 * dp).toInt()
        
        // Công thức tính lề đệm theo trục Y để căn giữa badge trong ô 48dp:
        // (48 - 38) / 2 = 5dp
        val badgeMarginY = (cellHeight - badgeSize) / 2

        // --- 3. DẢI MÀU NỐI KHOẢNG CHỌN (HIGHLIGHT CONTAINER) ---
        // Sử dụng kĩ thuật chia đôi (2 nửa Trái & Phải, mỗi nửa chiếm weight = 1.0f):
        // Kĩ thuật này đảm bảo dải màu không bao giờ bị lệch pixel hay tràn mép màn hình.
        highlightContainer.orientation = LinearLayout.HORIZONTAL
        highlightContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, badgeSize).apply {
            gravity = Gravity.CENTER_VERTICAL
            topMargin = badgeMarginY
            bottomMargin = badgeMarginY
        }

        // Nửa trái: weight = 1.0f, mặc định ẩn (INVISIBLE)
        leftHighlightView.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
        // Nửa phải: weight = 1.0f, mặc định ẩn (INVISIBLE)
        rightHighlightView.layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
        leftHighlightView.visibility = INVISIBLE
        rightHighlightView.visibility = INVISIBLE

        highlightContainer.addView(leftHighlightView)
        highlightContainer.addView(rightHighlightView)
        addView(highlightContainer)

        // --- 4. CẤU HÌNH VÒNG TRÒN BADGE NỀN ---
        circleBackgroundView.layoutParams = LayoutParams(badgeSize, badgeSize).apply {
            gravity = Gravity.CENTER // Căn chính giữa ô cả ngang lẫn dọc
        }
        // Bo tròn OVAL tạo thành hình tròn hoàn hảo
        circleDrawable.shape = GradientDrawable.OVAL
        circleBackgroundView.background = circleDrawable
        circleBackgroundView.visibility = INVISIBLE
        addView(circleBackgroundView)

        // --- 5. CẤU HÌNH CỤM CHỮ NGÀY DƯƠNG VÀ ÂM (TEXT CONTAINER) ---
        textContainer.orientation = LinearLayout.VERTICAL
        textContainer.gravity = Gravity.CENTER
        textContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // TextView Ngày dương lịch:
        solarTextView.apply {
            gravity = Gravity.CENTER
            // - textSize = 15f: Cỡ chữ ngày dương 15sp (rõ ràng, dễ đọc)
            // -> Muốn chữ to hơn: tăng lên 16f hoặc 17f
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD // In đậm để tách biệt với ngày âm
            // includeFontPadding = false: Tắt padding vô hình của Android để chữ căn giữa tuyệt đối
            includeFontPadding = false
        }

        // TextView Ngày âm lịch:
        lunarTextView.apply {
            gravity = Gravity.CENTER
            // - textSize = 9f: Cỡ chữ ngày âm 9sp (nhỏ gọn nằm bên dưới)
            // -> Muốn chữ âm to hơn: tăng lên 10f
            textSize = 9f
            typeface = Typeface.DEFAULT // Chữ nét thường
            includeFontPadding = false
            
            // - topMargin = (1 * dp).toInt(): Khoảng cách dọc 1dp giữa đáy chữ dương và đỉnh chữ âm
            // -> Muốn 2 dòng chữ cách xa nhau hơn: tăng lên 2 * dp hoặc 3 * dp
            val lp = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = (1 * dp).toInt()
            }
            layoutParams = lp
        }

        textContainer.addView(solarTextView)
        textContainer.addView(lunarTextView)
        addView(textContainer)
    }

    /// Xoá trắng ô ngày (dành cho ô trống đầu tháng hoặc khi tái sử dụng view)
    fun clear() {
        solarTextView.text = ""
        lunarTextView.text = ""
        leftHighlightView.visibility = INVISIBLE
        rightHighlightView.visibility = INVISIBLE
        circleBackgroundView.visibility = INVISIBLE
        isClickable = false
    }

    // =========================================================================
    // MARK: - Cấu hình Dữ liệu, Trạng thái & Màu sắc (Configure State & Colors)
    // =========================================================================
    
    /// Cấu hình dữ liệu và phong cách giao diện cho ô ngày
    /// - Parameters:
    ///   - date: Đối tượng Calendar ngày dương
    ///   - showLunar: Cờ hiển thị ngày âm
    ///   - position: Vị trí trong dải chọn (NONE, SINGLE, START, MIDDLE, END)
    ///   - isDisabled: Ô có bị vô hiệu hoá do nằm ngoài minDate/maxDate không
    ///   - theme: Bảng màu tùy chỉnh từ React Native
    ///   - language: Ngôn ngữ hiển thị ("vi", "en", "zh")
    fun configure(
        date: Calendar,
        showLunar: Boolean,
        position: RangePosition,
        isDisabled: Boolean,
        theme: PickerTheme?,
        language: String
    ) {
        // Lấy số ngày trong tháng (ví dụ: ngày 15 -> "15")
        val day = date.get(Calendar.DAY_OF_MONTH)
        solarTextView.text = day.toString()

        // Tính toán âm lịch từ ngày dương
        val lunar = LunarCalendarHelper.convertSolarToLunar(date, language)
        lunarTextView.visibility = if (showLunar) VISIBLE else GONE
        lunarTextView.text = lunar.lunarDayName

        // --- XỬ LÝ BẢNG MÀU SẮC (THEME COLORS) ---
        // Màu chủ đạo: Mặc định là xanh #007AFF
        val primaryColor = ColorUtils.parseHexColor(theme?.primaryColor, Color.parseColor("#007AFF"))
        // Màu chữ khi được chọn: Mặc định là trắng
        val selectedTextColor = ColorUtils.parseHexColor(theme?.selectedTextColor, Color.WHITE)
        // Màu chữ thường: Mặc định là đen
        val textColor = ColorUtils.parseHexColor(theme?.textColor, Color.BLACK)
        // Màu ngày đặc biệt (mùng 1, rằm): Mặc định là đỏ #FF3B30
        val specialColor = ColorUtils.parseHexColor(theme?.specialDayColor, Color.parseColor("#FF3B30"))

        // - defaultRangeBg: Màu nền mặc định cho dải nối khoảng chọn
        // Dùng alpha = 46/255 (xấp xỉ 18% mờ, tương đồng với withAlphaComponent(0.18) bên iOS)
        // -> Muốn dải nối đậm hơn: tăng alpha 46 lên 60 hoặc 80
        val defaultRangeBg = Color.argb(46, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
        val rangeBgColor = ColorUtils.parseHexColor(theme?.rangeColor, defaultRangeBg)

        // Gán màu cho badge tròn và 2 nửa dải màu nối
        circleDrawable.setColor(primaryColor)
        leftHighlightView.setBackgroundColor(rangeBgColor)
        rightHighlightView.setBackgroundColor(rangeBgColor)

        // --- TRƯỜNG HỢP Ô BỊ KHÓA (DISABLED) ---
        if (isDisabled) {
            isClickable = false // Khóa sự kiện bấm
            leftHighlightView.visibility = INVISIBLE
            rightHighlightView.visibility = INVISIBLE
            circleBackgroundView.visibility = INVISIBLE
            
            // Làm mờ chữ sang màu xám nhạt #BDBDBD
            solarTextView.setTextColor(Color.parseColor("#BDBDBD"))
            lunarTextView.setTextColor(Color.parseColor("#BDBDBD"))
            lunarTextView.typeface = Typeface.DEFAULT
            return
        }

        // Mở quyền click
        isClickable = true

        // --- HIỂN THỊ DẢI NỐI VÀ MÀU CHỮ THEO VỊ TRÍ (RANGE POSITION) ---
        when (position) {
            RangePosition.NONE -> {
                // 1. Ngày thường: Không có dải màu, không có badge tròn
                leftHighlightView.visibility = INVISIBLE
                rightHighlightView.visibility = INVISIBLE
                circleBackgroundView.visibility = INVISIBLE
                solarTextView.setTextColor(textColor)
                
                // Ngày rằm (15) hoặc mùng 1 âm lịch: Đổi chữ âm sang màu đỏ đặc biệt và in đậm
                if (lunar.isSpecialDay) {
                    lunarTextView.setTextColor(specialColor)
                    lunarTextView.typeface = Typeface.DEFAULT_BOLD
                } else {
                    // Ngày thường: Màu xám #8E8E93
                    lunarTextView.setTextColor(Color.parseColor("#8E8E93"))
                    lunarTextView.typeface = Typeface.DEFAULT
                }
            }
            RangePosition.SINGLE -> {
                // 2. Chọn duy nhất 1 ngày: Hiện badge tròn, ẩn cả 2 nửa dải nối
                leftHighlightView.visibility = INVISIBLE
                rightHighlightView.visibility = INVISIBLE
                circleBackgroundView.visibility = VISIBLE
                solarTextView.setTextColor(selectedTextColor)
                lunarTextView.setTextColor(selectedTextColor)
                lunarTextView.typeface = Typeface.DEFAULT
            }
            RangePosition.START -> {
                // 3. Ngày Bắt đầu khoảng chọn (Start):
                // Ẩn nửa trái, HIỆN NỬA PHẢI (để nối sang các ngày tiếp theo), hiện badge tròn
                leftHighlightView.visibility = INVISIBLE
                rightHighlightView.visibility = VISIBLE
                circleBackgroundView.visibility = VISIBLE
                solarTextView.setTextColor(selectedTextColor)
                lunarTextView.setTextColor(selectedTextColor)
                lunarTextView.typeface = Typeface.DEFAULT
            }
            RangePosition.MIDDLE -> {
                // 4. Các ngày ở GIỮA khoảng chọn:
                // HIỆN CẢ 2 NỬA TRÁI & PHẢI để phủ kín trọn vẹn ô ngày, ẩn badge tròn
                leftHighlightView.visibility = VISIBLE
                rightHighlightView.visibility = VISIBLE
                circleBackgroundView.visibility = INVISIBLE
                // Đổi chữ sang primaryColor để nổi bật trên nền mờ 18%
                solarTextView.setTextColor(primaryColor)
                lunarTextView.setTextColor(primaryColor)
                lunarTextView.typeface = Typeface.DEFAULT
            }
            RangePosition.END -> {
                // 5. Ngày Kết thúc khoảng chọn (End):
                // HIỆN NỬA TRÁI (nối từ các ngày trước tới), ẩn nửa phải, hiện badge tròn
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
