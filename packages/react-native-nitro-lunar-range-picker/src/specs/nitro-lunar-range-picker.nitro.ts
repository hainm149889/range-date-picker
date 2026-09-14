import type {
  HybridView,
  HybridViewProps,
  HybridViewMethods,
} from 'react-native-nitro-modules'

export interface DateInfo {
  year: number
  month: number
  day: number
  timestamp: number
  lunarDay: number
  lunarMonth: number
  lunarYear: number
  isLeapMonth?: boolean
  lunarDayName?: string
}

export interface PickerTheme {
  primaryColor?: string
  backgroundColor?: string
  textColor?: string
  selectedTextColor?: string
  rangeColor?: string
  specialDayColor?: string
}

export interface DateRangeResult {
  startDate: DateInfo
  endDate: DateInfo
}

export type PickerLanguage = 'vi' | 'en' | 'zh'
export type FirstDayOfWeek = 'monday' | 'sunday'
export type DisplayMode = 'single' | 'multi'

export interface NitroLunarRangePickerProps extends HybridViewProps {
  language: PickerLanguage
  theme?: PickerTheme
  showLunarDate: boolean
  firstDayOfWeek?: FirstDayOfWeek
  displayMode?: DisplayMode
  numberOfMonths?: number
  startDate?: string
  endDate?: string
  minDate?: string
  maxDate?: string
  closeIconUri?: string
  confirmIconUri?: string
  onConfirm: (result: DateRangeResult) => void
  onClose: () => void
}

export interface NitroLunarRangePickerMethods extends HybridViewMethods {}

export type NitroLunarRangePicker = HybridView<
  NitroLunarRangePickerProps,
  NitroLunarRangePickerMethods,
  { ios: 'swift'; android: 'kotlin' }
>
