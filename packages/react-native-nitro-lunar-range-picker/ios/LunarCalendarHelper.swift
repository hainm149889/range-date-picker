import Foundation

public struct LunarDateResult {
    public let day: Int
    public let month: Int
    public let year: Int
    public let isLeap: Bool
    public let lunarDayName: String
    public let isSpecialDay: Bool
}

public class LunarCalendarHelper {
    /// Chuyển đổi từ NSDate sang Âm Lịch (Sử dụng Calendar Chinese chuẩn của hệ thống)
    public static func convertSolarToLunar(date: Date, language: String = "vi") -> LunarDateResult {
        let chineseCalendar = Calendar(identifier: .chinese)
        let day: Int
        let month: Int
        let year: Int
        var isLeap = false

        if #available(iOS 17.0, *) {
            let components = chineseCalendar.dateComponents([.year, .month, .day, .isLeapMonth], from: date)
            day = components.day ?? 1
            month = components.month ?? 1
            year = components.year ?? 1
            isLeap = components.isLeapMonth ?? false
        } else {
            let components = chineseCalendar.dateComponents([.year, .month, .day], from: date)
            day = components.day ?? 1
            month = components.month ?? 1
            year = components.year ?? 1
        }
        
        // Hiển thị ngày âm lịch dạng DD/MM (ví dụ 01/08, 15/08)
        let dayName = String(format: "%02d/%02d", day, month)
        let isSpecial = (day == 1 || day == 15)
        
        return LunarDateResult(
            day: day,
            month: month,
            year: year,
            isLeap: isLeap,
            lunarDayName: dayName,
            isSpecialDay: isSpecial
        )
    }
}