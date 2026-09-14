package com.nitrolunarrangepicker

import android.view.View
import androidx.annotation.Keep
import com.facebook.proguard.annotations.DoNotStrip
import com.facebook.react.uimanager.ThemedReactContext
import com.margelo.nitro.nitrolunarrangepicker.*

@Keep
@DoNotStrip
class HybridNitroLunarRangePicker(val context: ThemedReactContext): HybridNitroLunarRangePickerSpec() {
    private val picker = LunarRangePickerView(context)

    override val view: View
        get() = picker

    override var language: PickerLanguage
        get() = picker.language
        set(value) {
            picker.language = value
        }

    override var theme: PickerTheme?
        get() = picker.theme
        set(value) {
            picker.theme = value
        }

    override var showLunarDate: Boolean
        get() = picker.showLunarDate
        set(value) {
            picker.showLunarDate = value
        }

    override var firstDayOfWeek: FirstDayOfWeek?
        get() = picker.firstDayOfWeek
        set(value) {
            if (value != null) {
                picker.firstDayOfWeek = value
            }
        }

    override var displayMode: DisplayMode?
        get() = picker.displayMode
        set(value) {
            if (value != null) {
                picker.displayMode = value
            }
        }

    override var numberOfMonths: Double?
        get() = picker.numberOfMonths
        set(value) {
            if (value != null) {
                picker.numberOfMonths = value
            }
        }

    override var startDate: String?
        get() = picker.startDate
        set(value) {
            picker.startDate = value
        }

    override var endDate: String?
        get() = picker.endDate
        set(value) {
            picker.endDate = value
        }

    override var minDate: String?
        get() = picker.minDate
        set(value) {
            picker.minDate = value
        }

    override var maxDate: String?
        get() = picker.maxDate
        set(value) {
            picker.maxDate = value
        }

    override var closeIconUri: String?
        get() = picker.closeIconUri
        set(value) {
            picker.closeIconUri = value
        }

    override var confirmIconUri: String?
        get() = picker.confirmIconUri
        set(value) {
            picker.confirmIconUri = value
        }

    override var onConfirm: (result: DateRangeResult) -> Unit
        get() = picker.onConfirm ?: {}
        set(value) {
            picker.onConfirm = value
        }

    override var onClose: () -> Unit
        get() = picker.onClose ?: {}
        set(value) {
            picker.onClose = value
        }
}
