import Foundation

enum RangePosition {
    case none
    case single
    case start
    case middle
    case end
}

struct MonthData {
    let year: Int
    let month: Int
    let title: String
    let days: [Date?]
}
