export interface DateInfo {
  // Dữ liệu Dương lịch (dùng cho logic chính)
  year: number; // Ví dụ: 2026
  month: number; // 1 - 12
  day: number; // 1 - 31
  timestamp: number; // Epoch timestamp (ms) để JS dễ sort / so sánh

  // Dữ liệu Âm lịch đi kèm (chỉ phục vụ hiển thị/tham chiếu)
  lunarDay: number; // Ví dụ: 1 hoặc 15
  lunarMonth: number; // Ví dụ: 1 (Tháng Giêng)
  lunarYear: number; // Ví dụ: 2026 (Bính Ngọ)
  isLeapMonth?: boolean; // Có phải tháng nhuận Âm lịch không
  lunarDayName?: string; // Ví dụ hiển thị: "Mùng 1", "Rằm", "01/05"
}

// Định nghĩa Theme tùy chỉnh
export interface PickerTheme {
  primaryColor?: string; // Màu chủ đạo (nền ngày được chọn, nút bấm). Mặc định: "#007AFF"
  backgroundColor?: string; // Màu nền chung của View. Mặc định: "#FFFFFF"
  textColor?: string; // Màu chữ chung (nếu không truyền, tự suy ra lunarTextColor từ opacity)
  selectedTextColor?: string; // Màu chữ khi được chọn. Mặc định: "#FFFFFF"
}

// Định nghĩa Range Ngày được chọn
export interface DateRangeResult {
  startDate: DateInfo;
  endDate: DateInfo;
}

// Định nghĩa type riêng
export type PickerLanguage = "vi" | "en" | "zh";

// Props truyền từ React Native vào Native View
export interface LunarRangePickerProps {
  language: PickerLanguage;
  theme?: PickerTheme; // Theme giao diện
  showLunarDate: boolean; // Bật/Tắt hiển thị Âm lịch
  minDate?: string; // Ngày nhỏ nhất (YYYY-MM-DD)
  maxDate?: string; // Ngày lớn nhất (YYYY-MM-DD)
  closeIconUri?: string; // Image URI cho nút Close
  confirmIconUri?: string; // Image URI cho nút Pick
  onConfirm: (result: DateRangeResult) => void; // Callback khi bấm chọn
  onClose: () => void; // Callback khi đóng
}
