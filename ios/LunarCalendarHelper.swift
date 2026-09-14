import Foundation

public struct LunarDateResult {
    public let day: Int
    public let month: Int
    public let year: Int
    public let isLeap: Bool
    public let lunarDayName: String
}

public class LunarCalendarHelper {
    /// Chuyển đổi từ NSDate sang Âm Lịch (Sử dụng Calendar Chinese chuẩn của hệ thống)
    public static func convertSolarToLunar(date: Date, language: String = "vi") -> LunarDateResult {
        let chineseCalendar = Calendar(identifier: .chinese)
        let components = chineseCalendar.dateComponents([.year, .month, .day, .isLeapMonth], from: date)
        
        let day = components.day ?? 1
        let month = components.month ?? 1
        let year = components.year ?? 1
        let isLeap = components.isLeapMonth ?? false
        
        // Tạo tên hiển thị (Ví dụ: Mùng 1, Rằm hoặc 01/05)
        var dayName = "\(day)"
        if language == "vi" {
            if day == 1 { dayName = "Mùng 1" }
            else if day == 15 { dayName = "Rằm" }
            else if day < 10 { dayName = "0\(day)" }
        }
        
        return LunarDateResult(
            day: day,
            month: month,
            year: year,
            isLeap: isLeap,
            lunarDayName: dayName
        )
    }
}