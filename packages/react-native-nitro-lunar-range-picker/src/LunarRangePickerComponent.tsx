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

export const NitroLunarRangePicker = getHostComponent<
  NitroLunarRangePickerProps,
  NitroLunarRangePickerMethods
>("NitroLunarRangePicker", () => NitroLunarRangePickerConfig);

export interface LunarRangePickerComponentProps
  extends Omit<
    NitroLunarRangePickerProps,
    "closeIconUri" | "confirmIconUri" | "onConfirm" | "onClose"
  > {
  style?: StyleProp<ViewStyle>;
  closeIcon?: any;
  confirmIcon?: any;
  onConfirm: (result: DateRangeResult) => void;
  onClose?: () => void;
  firstDayOfWeek?: FirstDayOfWeek;
  displayMode?: DisplayMode;
  numberOfMonths?: number;
  startDate?: string;
  endDate?: string;

  /**
   * Hiển thị dưới dạng Modal FormSheet (iOS PageSheet)
   * Có hỗ trợ vuốt xuống để đóng và giao diện thẻ chuẩn iOS.
   */
  isModal?: boolean;
  /**
   * Trạng thái hiển thị khi dùng chế độ `isModal`
   */
  visible?: boolean;
}

export const LunarRangePicker: React.FC<LunarRangePickerComponentProps> = ({
  style,
  theme,
  language = "vi",
  showLunarDate = true,
  firstDayOfWeek = "monday",
  displayMode = "multi",
  numberOfMonths = 12,
  startDate,
  endDate,
  closeIcon,
  confirmIcon,
  onConfirm,
  onClose,
  minDate,
  maxDate,
  isModal = false,
  visible = true,
}) => {
  const [internalStartDate, setInternalStartDate] = useState<string | undefined>(startDate);
  const [internalEndDate, setInternalEndDate] = useState<string | undefined>(endDate);

  useEffect(() => {
    if (startDate !== undefined) setInternalStartDate(startDate);
  }, [startDate]);

  useEffect(() => {
    if (endDate !== undefined) setInternalEndDate(endDate);
  }, [endDate]);

  const handleInternalConfirm = useCallback(
    (result: DateRangeResult) => {
      const s = `${result.startDate.year}-${String(result.startDate.month).padStart(2, "0")}-${String(result.startDate.day).padStart(2, "0")}`;
      const e = `${result.endDate.year}-${String(result.endDate.month).padStart(2, "0")}-${String(result.endDate.day).padStart(2, "0")}`;
      setInternalStartDate(s);
      setInternalEndDate(e);
      onConfirm(result);
    },
    [onConfirm]
  );

  const mergedTheme = useMemo<PickerTheme>(() => {
    return {
      primaryColor: theme?.primaryColor ?? "#007AFF",
      backgroundColor: theme?.backgroundColor ?? "#FFFFFF",
      textColor: theme?.textColor ?? "#000000",
      selectedTextColor: theme?.selectedTextColor ?? "#FFFFFF",
      rangeColor: theme?.rangeColor,
      specialDayColor: theme?.specialDayColor ?? "#FF3B30",
    };
  }, [theme]);

  const resolvedCloseIconUri = useMemo(() => {
    return getNitroImageUri(closeIcon);
  }, [closeIcon]);

  const resolvedConfirmIconUri = useMemo(() => {
    return getNitroImageUri(confirmIcon);
  }, [confirmIcon]);

  const wrappedOnConfirm = useMemo(() => {
    return callback(handleInternalConfirm);
  }, [handleInternalConfirm]);

  const wrappedOnClose = useMemo(() => {
    return callback(onClose ?? (() => {}));
  }, [onClose]);

  const effectiveStartDate = startDate ?? internalStartDate;
  const effectiveEndDate = endDate ?? internalEndDate;

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

  if (isModal) {
    if (Platform.OS === "android") {
      return (
        <Modal
          visible={visible}
          transparent={true}
          animationType="slide"
          statusBarTranslucent={true}
          onRequestClose={onClose}
        >
          <View style={styles.androidBackdrop}>
            <Pressable
              style={styles.androidBackdropPressable}
              onPress={onClose}
            />
            <View
              style={[
                styles.androidSheetContainer,
                { backgroundColor: mergedTheme.backgroundColor },
              ]}
            >
              {/* Grabber indicator cho BottomSheet Android */}
              <View style={styles.grabberContainer}>
                <View style={styles.grabber} />
              </View>
              {pickerElement}
            </View>
          </View>
        </Modal>
      );
    }

    return (
      <Modal
        visible={visible}
        animationType="slide"
        presentationStyle="pageSheet"
        onRequestClose={onClose}
      >
        <SafeAreaView
          style={[
            styles.modalContainer,
            { backgroundColor: mergedTheme.backgroundColor },
          ]}
        >
          {/* Grabber indicator cho FormSheet iOS */}
          <View style={styles.grabberContainer}>
            <View style={styles.grabber} />
          </View>
          {pickerElement}
        </SafeAreaView>
      </Modal>
    );
  }

  return pickerElement;
};

const styles = StyleSheet.create({
  defaultStyle: {
    width: "100%",
    height: 480,
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
  androidBackdrop: {
    flex: 1,
    backgroundColor: "rgba(0, 0, 0, 0.45)",
    justifyContent: "flex-end",
  },
  androidBackdropPressable: {
    flex: 1,
  },
  androidSheetContainer: {
    height: "82%",
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    overflow: "hidden",
  },
  grabberContainer: {
    alignItems: "center",
    paddingTop: 8,
    paddingBottom: 4,
  },
  grabber: {
    width: 36,
    height: 5,
    borderRadius: 2.5,
    backgroundColor: "#C5C5C7",
  },
});
