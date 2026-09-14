//
//  HybridNitroLunarRangePicker.swift
//  Pods
//
//  Created by Hainm14 on 9/14/2026.
//

import Foundation
import UIKit

class HybridNitroLunarRangePicker : HybridNitroLunarRangePickerSpec {
  // UIView
  var view: UIView = UIView()

  // Props
  var isRed: Bool = false {
    didSet {
      view.backgroundColor = isRed ? .red : .black
    }
  }
}
