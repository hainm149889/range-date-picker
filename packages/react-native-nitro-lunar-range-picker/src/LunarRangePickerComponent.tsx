import React, { useMemo, useState, useEffect, useCallback } from "react";
import {
  type StyleProp,
  type ViewStyle,
  StyleSheet,
  Modal,
  SafeAreaView,
  View,
  Platform,
  Pressable,
} from "react-native";
import { getHostComponent, callback } from "react-native-nitro-modules";

import { getNitroImageUri } from "./utils/imageHelper";
import NitroLunarRangePickerConfig from "../nitrogen/generated/shared/json/NitroLunarRangePickerConfig.json";
import type {
  NitroLunarRangePickerProps,
  NitroLunarRangePickerMethods,
  PickerTheme,
  DateRangeResult,
  FirstDayOfWeek,
  DisplayMode,
} from "./specs/nitro-lunar-range-picker.nitro";

/// Khởi tạo Native Host Component thông qua Nitro Modules JSI (Zero-overhead bridge)
export const NitroLunarRangePicker = getHostComponent<
  NitroLunarRangePickerProps,
  NitroLunarRangePickerMethods
>("NitroLunarRangePicker", () => NitroLunarRangePickerConfig);

/// Interface định nghĩa tất cả các Props mà developer có thể truyền vào Component <LunarRangePicker />
export interface LunarRangePickerComponentProps
  extends Omit<
    NitroLunarRangePickerProps,
    "closeIconUri" | "confirmIconUri" | "onConfirm" | "onClose"
  > {
  /**
   * Style tùy chỉnh áp dụng cho container bọc ngoài của picker
   */
  style?: StyleProp<ViewStyle>;

  /**
   * Icon nút đóng Close tùy chỉnh (nhận require('./close.png') hoặc { uri: '...' } hoặc URL string)
   */
  closeIcon?: any;

  /**
   * Icon nút xác nhận Confirm tùy chỉnh
   */
  confirmIcon?: any;

  /**
   * Callback được kích hoạt khi người dùng hoàn tất chọn khoảng ngày (trả về kết quả DateRangeResult)
   */
  onConfirm: (result: DateRangeResult) => void;

  /**
   * Callback được kích hoạt khi người dùng nhấn nút Close hoặc chạm ra vùng backdrop đóng modal
   */
  onClose?: () => void;

  /**
   * Ngày đầu tiên trong tuần: "monday" (Thứ 2) hoặc "sunday" (Chủ Nhật)
   */
  firstDayOfWeek?: FirstDayOfWeek;

  /**
   * Chế độ hiển thị: "multi" (danh sách cuộn nhiều tháng) hoặc "single" (1 tháng)
   */
  displayMode?: DisplayMode;

  /**
   * Số lượng tháng hiển thị trong chế độ cuộn "multi" (mặc định: 12 tháng)
   */
  numberOfMonths?: number;

  /**
   * Ngày bắt đầu mặc định (định dạng "YYYY-MM-DD" hoặc ISO string)
   */
  startDate?: string;

  /**
   * Ngày kết thúc mặc định (định dạng "YYYY-MM-DD" hoặc ISO string)
   */
  endDate?: string;

  /**
   * Bật chế độ hiển thị dạng Modal:
   * - Trên iOS: FormSheet (PageSheet) chuẩn phong cách thẻ Apple, hỗ trợ vuốt xuống để đóng.
   * - Trên Android: BottomSheet với nền đen mờ và hiệu ứng trượt từ dưới lên.
   */
  isModal?: boolean;

  /**
   * Trạng thái đóng/mở khi sử dụng chế độ `isModal`
   */
  visible?: boolean;
}

/// LunarRangePicker: Component bọc (Wrapper) chính của thư viện bên phía React Native / TypeScript.
/// Xử lý: Chuẩn hóa Theme, chuyển đổi Asset ảnh sang URI cho Nitro Native, bọc Callback JSI,
/// và hỗ trợ hiển thị linh hoạt giữa dạng nhúng trực tiếp (Inline) hoặc dạng Modal (FormSheet / BottomSheet).
export const LunarRangePicker: React.FC<LunarRangePickerComponentProps> = ({
  style,
  theme,
  language = "vi",           // Mặc định: Tiếng Việt ("vi", "en", "zh")
  showLunarDate = true,      // Mặc định: Luôn hiển thị ngày âm lịch
  firstDayOfWeek = "monday", // Mặc định: Thứ 2 là ngày đầu tuần
  displayMode = "multi",     // Mặc định: Cuộn nhiều tháng
  numberOfMonths = 12,       // Mặc định: Hiển thị 12 tháng
  startDate,
  endDate,
  closeIcon,
  confirmIcon,
  onConfirm,
  onClose,
  minDate,
  maxDate,
  isModal = false,           // Mặc định: Hiển thị Inline (không phải Modal)
  visible = true,            // Mặc định: Luôn mở nếu không kiểm soát
}) => {
  // State nội bộ lưu trữ ngày bắt đầu và kết thúc để đồng bộ giao diện
  const [internalStartDate, setInternalStartDate] = useState<string | undefined>(startDate);
  const [internalEndDate, setInternalEndDate] = useState<string | undefined>(endDate);

  // Đồng bộ state nội bộ khi props startDate / endDate từ bên ngoài thay đổi
  useEffect(() => {
    if (startDate !== undefined) setInternalStartDate(startDate);
  }, [startDate]);

  useEffect(() => {
    if (endDate !== undefined) setInternalEndDate(endDate);
  }, [endDate]);

  // Xử lý lưu state nội bộ và chuyển tiếp kết quả lên onConfirm của màn hình cha
  const handleInternalConfirm = useCallback(
    (result: DateRangeResult) => {
      // Định dạng ngày thành chuỗi chuẩn "YYYY-MM-DD"
      const s = `${result.startDate.year}-${String(result.startDate.month).padStart(2, "0")}-${String(result.startDate.day).padStart(2, "0")}`;
      const e = `${result.endDate.year}-${String(result.endDate.month).padStart(2, "0")}-${String(result.endDate.day).padStart(2, "0")}`;
      setInternalStartDate(s);
      setInternalEndDate(e);
      onConfirm(result);
    },
    [onConfirm]
  );

  // --- HỆ THỐNG MÀU SẮC MẶC ĐỊNH (THEME DEFAULTS) ---
  const mergedTheme = useMemo<PickerTheme>(() => {
    return {
      // Màu chủ đạo: Mặc định là xanh dương iOS #007AFF
      primaryColor: theme?.primaryColor ?? "#007AFF",
      // Màu nền lịch: Mặc định là trắng #FFFFFF
      backgroundColor: theme?.backgroundColor ?? "#FFFFFF",
      // Màu chữ ngày thường: Mặc định là đen #000000
      textColor: theme?.textColor ?? "#000000",
      // Màu chữ khi được chọn: Mặc định là trắng #FFFFFF
      selectedTextColor: theme?.selectedTextColor ?? "#FFFFFF",
      // Màu nền dải nối khoảng chọn: Mặc định không truyền (Native sẽ lấy primaryColor mờ 18%)
      rangeColor: theme?.rangeColor,
      // Màu ngày đặc biệt (mùng 1 & rằm): Mặc định là đỏ #FF3B30
      specialDayColor: theme?.specialDayColor ?? "#FF3B30",
    };
  }, [theme]);

  // Phân giải asset hoặc URL icon Close sang chuỗi String URI tương thích với Nitro
  const resolvedCloseIconUri = useMemo(() => {
    return getNitroImageUri(closeIcon);
  }, [closeIcon]);

  // Phân giải asset hoặc URL icon Confirm
  const resolvedConfirmIconUri = useMemo(() => {
    return getNitroImageUri(confirmIcon);
  }, [confirmIcon]);

  // Bọc callback bằng tiện ích callback() của Nitro Modules để đảm bảo an toàn luồng JSI
  const wrappedOnConfirm = useMemo(() => {
    return callback(handleInternalConfirm);
  }, [handleInternalConfirm]);

  const wrappedOnClose = useMemo(() => {
    return callback(onClose ?? (() => {}));
  }, [onClose]);

  const effectiveStartDate = startDate ?? internalStartDate;
  const effectiveEndDate = endDate ?? internalEndDate;

  // Phần tử Native Host Picker Component thực tế
  const pickerElement = (
    <NitroLunarRangePicker
      style={[isModal ? styles.modalPicker : styles.defaultStyle, style]}
      language={language}
      theme={mergedTheme}
      showLunarDate={showLunarDate}
      firstDayOfWeek={firstDayOfWeek}
      displayMode={displayMode}
      numberOfMonths={numberOfMonths}
      startDate={effectiveStartDate}
      endDate={effectiveEndDate}
      minDate={minDate}
      maxDate={maxDate}
      closeIconUri={resolvedCloseIconUri}
      confirmIconUri={resolvedConfirmIconUri}
      onConfirm={wrappedOnConfirm}
      onClose={wrappedOnClose}
    />
  );

  // =========================================================================
  // MARK: - Render Chế độ Modal (FormSheet iOS / BottomSheet Android)
  // =========================================================================

  if (isModal) {
    if (Platform.OS === "android") {
      // Giao diện BottomSheet chuẩn trên Android:
      return (
        <Modal
          visible={visible}
          transparent={true}
          animationType="slide"
          statusBarTranslucent={true}
          onRequestClose={onClose}
        >
          <View style={styles.androidBackdrop}>
            {/* Vùng nền mờ: Nhấn vào vùng này sẽ tự động đóng modal */}
            <Pressable
              style={styles.androidBackdropPressable}
              onPress={onClose}
            />
            {/* Container tấm thẻ BottomSheet trượt lên từ đáy */}
            <View
              style={[
                styles.androidSheetContainer,
                { backgroundColor: mergedTheme.backgroundColor },
              ]}
            >
              {/* Thanh kẹp vuốt (Grabber Handle) phía trên đầu tấm thẻ */}
              <View style={styles.grabberContainer}>
                <View style={styles.grabber} />
              </View>
              {pickerElement}
            </View>
          </View>
        </Modal>
      );
    }

    // Giao diện FormSheet / PageSheet chuẩn phong cách iOS
    return (
      <Modal
        visible={visible}
        animationType="slide"
        presentationStyle="pageSheet" // Kiểu trang dạng thẻ của iOS (có thể vuốt xuống để đóng)
        onRequestClose={onClose}
      >
        <SafeAreaView
          style={[
            styles.modalContainer,
            { backgroundColor: mergedTheme.backgroundColor },
          ]}
        >
          {/* Thanh kẹp vuốt chỉ báo trên iOS */}
          <View style={styles.grabberContainer}>
            <View style={styles.grabber} />
          </View>
          {pickerElement}
        </SafeAreaView>
      </Modal>
    );
  }

  // Chế độ hiển thị Inline thông thường
  return pickerElement;
};

// =========================================================================
// MARK: - Stylesheet & Hằng số Kích thước React Native (RN Layout Constants)
// =========================================================================

const styles = StyleSheet.create({
  /// Cấu hình mặc định cho chế độ hiển thị Inline (khi isModal = false)
  defaultStyle: {
    width: "100%",
    // - height: 480: Chiều cao mặc định của bộ lịch khi nhúng vào một màn hình
    // -> Muốn khung lịch cao hơn để xem được nhiều tháng hơn: tăng lên 540 hoặc 600
    // -> Muốn khung lịch gọn gàng: giảm về 400 hoặc 440
    height: 480,
    // - borderRadius: 16: Bo tròn 4 góc 16dp
    borderRadius: 16,
    overflow: "hidden",
  },
  
  modalContainer: {
    flex: 1,
  },
  
  modalPicker: {
    flex: 1,
    width: "100%",
  },
  
  /// Lớp phủ nền mờ cho BottomSheet Android
  androidBackdrop: {
    flex: 1,
    // - backgroundColor: "rgba(0, 0, 0, 0.45)": Độ tối của nền mờ phía sau là 45% đen
    // -> Muốn nền tối đậm hơn: tăng 0.45 lên 0.6
    // -> Muốn nền trong suốt hơn: giảm về 0.3
    backgroundColor: "rgba(0, 0, 0, 0.45)",
    justifyContent: "flex-end", // Đẩy nội dung thẻ xuống đáy màn hình
  },
  
  androidBackdropPressable: {
    flex: 1,
  },
  
  /// Tấm thẻ BottomSheet Android
  androidSheetContainer: {
    // - height: "82%": Chiều cao của BottomSheet chiếm 82% chiều cao màn hình điện thoại
    // -> Muốn mở rộng gần như full màn hình: đổi thành "90%" hoặc "95%"
    // -> Muốn nhỏ gọn dạng nửa màn hình: đổi thành "60%" hoặc "70%"
    height: "82%",
    // - borderTopLeftRadius & borderTopRightRadius: 20: Bo tròn 2 góc trên cùng 20dp
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    overflow: "hidden",
  },
  
  /// Vùng chứa thanh kẹp vuốt (Grabber Handle)
  grabberContainer: {
    alignItems: "center",
    // - paddingTop: 8, paddingBottom: 4: Khoảng đệm trên dưới thanh kẹp
    paddingTop: 8,
    paddingBottom: 4,
  },
  
  /// Thanh kẹp vuốt (Grabber Indicator)
  grabber: {
    // - width: 36: Chiều dài thanh kẹp 36dp (chuẩn Apple HIG)
    width: 36,
    // - height: 5: Độ dày thanh kẹp 5dp
    height: 5,
    // - borderRadius: 2.5: Bo tròn 2 đầu bán nguyệt (bằng height / 2 = 5 / 2 = 2.5dp)
    borderRadius: 2.5,
    // - backgroundColor: "#C5C5C7": Màu xám bạc chuẩn thanh kéo
    backgroundColor: "#C5C5C7",
  },
});
