import type {
  HybridView,
  HybridViewProps,
  HybridViewMethods,
} from 'react-native-nitro-modules'

export interface NitroLunarRangePickerProps extends HybridViewProps {
   isRed: boolean
}

export interface NitroLunarRangePickerMethods extends HybridViewMethods {}

export type NitroLunarRangePicker = HybridView<NitroLunarRangePickerProps, NitroLunarRangePickerMethods, { ios: 'swift', android: 'kotlin' }>