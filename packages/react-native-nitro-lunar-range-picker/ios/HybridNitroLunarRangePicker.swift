//
//  HybridNitroLunarRangePicker.swift
//  NitroLunarRangePicker
//

import Foundation
import UIKit
import NitroModules

class HybridNitroLunarRangePicker: HybridNitroLunarRangePickerSpec {
  private let picker = LunarRangePickerView()

  var view: UIView {
    return picker
  }

  var language: PickerLanguage {
    get { picker.language }
    set { picker.language = newValue }
  }

  var theme: PickerTheme? {
    get { picker.theme }
    set { picker.theme = newValue }
  }

  var showLunarDate: Bool {
    get { picker.showLunarDate }
    set { picker.showLunarDate = newValue }
  }

  var firstDayOfWeek: FirstDayOfWeek? {
    get { picker.firstDayOfWeek }
    set {
      if let val = newValue {
        picker.firstDayOfWeek = val
      }
    }
  }

  var displayMode: DisplayMode? {
    get { picker.displayMode }
    set {
      if let val = newValue {
        picker.displayMode = val
      }
    }
  }

  var numberOfMonths: Double? {
    get { picker.numberOfMonths }
    set {
      if let val = newValue {
        picker.numberOfMonths = val
      }
    }
  }

  var startDate: String? {
    get { picker.startDate }
    set { picker.startDate = newValue }
  }

  var endDate: String? {
    get { picker.endDate }
    set { picker.endDate = newValue }
  }

  var minDate: String? {
    get { picker.minDate }
    set { picker.minDate = newValue }
  }

  var maxDate: String? {
    get { picker.maxDate }
    set { picker.maxDate = newValue }
  }

  var closeIconUri: String? {
    get { picker.closeIconUri }
    set { picker.closeIconUri = newValue }
  }

  var confirmIconUri: String? {
    get { picker.confirmIconUri }
    set { picker.confirmIconUri = newValue }
  }

  var onConfirm: (_ result: DateRangeResult) -> Void {
    get { picker.onConfirm ?? { _ in } }
    set { picker.onConfirm = newValue }
  }

  var onClose: () -> Void {
    get { picker.onClose ?? {} }
    set { picker.onClose = newValue }
  }
}
