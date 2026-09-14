import React, { useMemo } from "react";
import { type StyleProp, type ViewStyle, StyleSheet } from "react-native";
import { getHostComponent, callback } from "react-native-nitro-modules";

import { getNitroImageUri } from "./utils/imageHelper";
import type {
  LunarRangePickerProps,
  PickerTheme,
} from "./specs/LunarRangePicker.nitro";

// 💡 CẬP NHẬT TẠI ĐÂY: Bổ sung bubblingEventTypes và directEventTypes vào ViewConfig
const NativeLunarRangePickerView = getHostComponent<LunarRangePickerProps, {}>(
  "NitroLunarRangePickerView",
  () => ({
    uiViewClassName: "NitroLunarRangePickerView",
    bubblingEventTypes: {},
    directEventTypes: {},
    validAttributes: {
      language: true,
      theme: true,
      showLunarDate: true,
      minDate: true,
      maxDate: true,
      closeIconUri: true,
      confirmIconUri: true,
      onConfirm: true,
      onClose: true,
    },
  }),
);

export interface LunarRangePickerComponentProps extends Omit<
  LunarRangePickerProps,
  "closeIconUri" | "confirmIconUri"
> {
  style?: StyleProp<ViewStyle>;
  closeIcon?: any;
  confirmIcon?: any;
}

export const LunarRangePicker: React.FC<LunarRangePickerComponentProps> = ({
  style,
  theme,
  language = "vi",
  showLunarDate = true,
  closeIcon,
  confirmIcon,
  onConfirm,
  onClose,
  minDate,
  maxDate,
}) => {
  const mergedTheme = useMemo<PickerTheme>(() => {
    return {
      primaryColor: theme?.primaryColor ?? "#007AFF",
      backgroundColor: theme?.backgroundColor ?? "#FFFFFF",
      textColor: theme?.textColor ?? "#000000",
      selectedTextColor: theme?.selectedTextColor ?? "#FFFFFF",
    };
  }, [theme]);

  const resolvedCloseIconUri = useMemo(() => {
    return getNitroImageUri(closeIcon);
  }, [closeIcon]);

  const resolvedConfirmIconUri = useMemo(() => {
    return getNitroImageUri(confirmIcon);
  }, [confirmIcon]);

  // 💡 2. Bọc callback bằng wrapCallback để thỏa mãn NitroViewWrappedCallback
  const wrappedOnConfirm = useMemo(() => {
    return callback(onConfirm);
  }, [onConfirm]);

  const wrappedOnClose = useMemo(() => {
    return callback(onClose);
  }, [onClose]);

  return (
    <NativeLunarRangePickerView
      style={[styles.defaultStyle, style]}
      language={language}
      theme={mergedTheme}
      showLunarDate={showLunarDate}
      minDate={minDate}
      maxDate={maxDate}
      closeIconUri={resolvedCloseIconUri}
      confirmIconUri={resolvedConfirmIconUri}
      onConfirm={wrappedOnConfirm}
      onClose={wrappedOnClose}
    />
  );
};

const styles = StyleSheet.create({
  defaultStyle: {
    width: "100%",
    height: 380,
  },
});
