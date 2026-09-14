import Foundation

enum DateUtils {

    static func cleanToStartOfDay(_ date: Date) -> Date {
        return Calendar.current.startOfDay(for: date)
    }

    static func parseDateString(_ str: String?) -> Date? {
        guard let str = str?.trimmingCharacters(in: .whitespacesAndNewlines), !str.isEmpty else {
            return nil
        }
        
        // 1. ISO 8601 with fractional seconds
        let isoFormatterFractional = ISO8601DateFormatter()
        isoFormatterFractional.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = isoFormatterFractional.date(from: str) {
            return cleanToStartOfDay(date)
        }
        
        // 2. Standard ISO 8601
        let isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime]
        if let date = isoFormatter.date(from: str) {
            return cleanToStartOfDay(date)
        }
        
        // 3. Common date formats
        let df = DateFormatter()
        df.locale = Locale(identifier: "en_US_POSIX")
        for fmt in ["yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy", "dd-MM-yyyy"] {
            df.dateFormat = fmt
            if let date = df.date(from: str) {
                return cleanToStartOfDay(date)
            }
        }
        
        // 4. Epoch timestamp in milliseconds or seconds
        if let num = Double(str) {
            let seconds = (num > 10000000000) ? (num / 1000.0) : num
            return cleanToStartOfDay(Date(timeIntervalSince1970: seconds))
        }
        
        return nil
    }

    static func isSameDay(_ d1: Date?, _ d2: Date?) -> Bool {
        guard let d1 = d1, let d2 = d2 else { return false }
        return Calendar.current.isDate(d1, inSameDayAs: d2)
    }

    static func isBeforeDay(_ d1: Date, _ d2: Date) -> Bool {
        let c1 = cleanToStartOfDay(d1)
        let c2 = cleanToStartOfDay(d2)
        return c1 < c2
    }

    static func isAfterDay(_ d1: Date, _ d2: Date) -> Bool {
        let c1 = cleanToStartOfDay(d1)
        let c2 = cleanToStartOfDay(d2)
        return c1 > c2
    }
}
