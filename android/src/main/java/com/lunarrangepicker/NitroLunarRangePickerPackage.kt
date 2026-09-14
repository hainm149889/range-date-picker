package com.lunarrangepicker

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import com.margelo.nitro.HybridObjectRegistry

class NitroLunarRangePickerPackage : ReactPackage {
    
    // Đăng ký Nitro Hybrid View khi Package được load
    init {
        HybridObjectRegistry.register("NitroLunarRangePickerView") { context ->
            NitroLunarRangePickerViewManager(context)
        }
    }

    override func createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return emptyList() // Không dùng Bridge Module cũ
    }

    override func createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList() // Nitro tự quản lý View via JSI, không cần ViewManager cũ của RN
    }
}