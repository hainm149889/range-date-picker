package com.lunarrangepicker

import android.content.Context
import android.view.View
import com.margelo.nitro.NitroModules

// 1. Class đóng vai trò Manager cho Nitro View
class NitroLunarRangePickerViewManager(context: Context) : HybridLunarRangePickerViewSpec() {

    // 2. Khởi tạo Android View Native
    private val pickerComponentView = LunarRangePickerView(context)

    // 3. Trả View Native về cho Nitro Engine render
    override val view: View
        get() = pickerComponentView

    // 4. Quản lý Props truyền từ JavaScript
    override var props: LunarRangePickerProps
        get() = pickerComponentView.getProps()
        set(value) {
            // Mỗi khi props ở JS thay đổi, truyền dữ liệu mới vào View Native
            pickerComponentView.setProps(value)
        }
}