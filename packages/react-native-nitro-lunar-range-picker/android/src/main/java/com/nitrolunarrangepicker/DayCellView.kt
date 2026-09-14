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

class DayCellView(context: Context) : FrameLayout(context) {
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
        val primaryColor = ColorUtils.parseHexColor(theme?.primaryColor, Color.parseColor("#007AFF"))
        val selectedTextColor = ColorUtils.parseHexColor(theme?.selectedTextColor, Color.WHITE)
        val textColor = ColorUtils.parseHexColor(theme?.textColor, Color.BLACK)
        val specialColor = ColorUtils.parseHexColor(theme?.specialDayColor, Color.parseColor("#FF3B30"))

        val defaultRangeBg = Color.argb(46, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor))
        val rangeBgColor = ColorUtils.parseHexColor(theme?.rangeColor, defaultRangeBg)

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
                leftHighlightView.visibility = INVISIBLE
                rightHighlightView.visibility = VISIBLE
                circleBackgroundView.visibility = VISIBLE
                solarTextView.setTextColor(selectedTextColor)
                lunarTextView.setTextColor(selectedTextColor)
                lunarTextView.typeface = Typeface.DEFAULT
            }
            RangePosition.MIDDLE -> {
                leftHighlightView.visibility = VISIBLE
                rightHighlightView.visibility = VISIBLE
                circleBackgroundView.visibility = INVISIBLE
                solarTextView.setTextColor(primaryColor)
                lunarTextView.setTextColor(primaryColor)
                lunarTextView.typeface = Typeface.DEFAULT
            }
            RangePosition.END -> {
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
