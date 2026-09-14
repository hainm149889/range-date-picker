import UIKit
import NitroModules

// 1. Class kế thừa từ Protocol tự động sinh ra bởi nitro-codegen
class NitroLunarRangePickerViewManager: HybridLunarRangePickerViewSpec {
    
    // 2. Tạo biến lưu trữ Instance UIView thực tế
    private let pickerComponentView = LunarRangePickerView()

    // 3. Biến view bắt buộc của Spec, trả về UIView để React Native nhúng vào màn hình
    var view: UIView {
        return pickerComponentView
    }

    // 4. Biến props chứa dữ liệu từ JS (Theme, language, showLunarDate, callbacks...)
    var props: LunarRangePickerProps {
        get {
            // Trả về props hiện tại (hoặc mock nếu chưa gán)
            return pickerComponentView.props ?? LunarRangePickerProps()
        }
        set {
            // Khi phía React Native thay đổi props, setter này được gọi.
            // Ta chuyển props mới vào View Native để UI cập nhật (re-render).
            pickerComponentView.props = newValue
        }
    }
}