import { getHostComponent, type HybridRef } from 'react-native-nitro-modules'
import NitroLunarRangePickerConfig from '../nitrogen/generated/shared/json/NitroLunarRangePickerConfig.json'
import type {
  NitroLunarRangePickerProps,
  NitroLunarRangePickerMethods,
} from './specs/nitro-lunar-range-picker.nitro'


export const NitroLunarRangePicker = getHostComponent<NitroLunarRangePickerProps, NitroLunarRangePickerMethods>(
  'NitroLunarRangePicker',
  () => NitroLunarRangePickerConfig
)

export type NitroLunarRangePickerRef = HybridRef<NitroLunarRangePickerProps, NitroLunarRangePickerMethods>
