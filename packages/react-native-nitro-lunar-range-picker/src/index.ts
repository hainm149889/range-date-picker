import type { HybridRef } from 'react-native-nitro-modules'
import type {
  NitroLunarRangePickerProps,
  NitroLunarRangePickerMethods,
} from './specs/nitro-lunar-range-picker.nitro'

export {
  NitroLunarRangePicker,
  LunarRangePicker,
} from './LunarRangePickerComponent'
export type { LunarRangePickerComponentProps } from './LunarRangePickerComponent'

export type NitroLunarRangePickerRef = HybridRef<
  NitroLunarRangePickerProps,
  NitroLunarRangePickerMethods
>

export type {
  DateInfo,
  PickerTheme,
  DateRangeResult,
  PickerLanguage,
  FirstDayOfWeek,
  DisplayMode,
  NitroLunarRangePickerProps,
  NitroLunarRangePickerMethods,
} from './specs/nitro-lunar-range-picker.nitro'
