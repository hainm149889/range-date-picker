import NitroModules

// Hàm tự động chạy khi App khởi chạy
public class NitroLunarRangePickerRegistration {
    public static func register() {
        // Đăng ký Name совпада với tên định nghĩa trong Nitro
        HybridObjectRegistry.register(
            hybridObjectName: "NitroLunarRangePickerView",
            constructor: { NitroLunarRangePickerViewManager() }
        )
    }
}