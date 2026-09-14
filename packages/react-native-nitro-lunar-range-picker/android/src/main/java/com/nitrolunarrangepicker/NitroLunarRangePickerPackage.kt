package com.nitrolunarrangepicker;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.BaseReactPackage;
import com.facebook.react.uimanager.ViewManager;
import com.margelo.nitro.nitrolunarrangepicker.*;
import com.margelo.nitro.nitrolunarrangepicker.views.*;


public class NitroLunarRangePickerPackage : BaseReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? = null

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider = ReactModuleInfoProvider { emptyMap() }
  
  override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
    val viewManagers = ArrayList<ViewManager<*, *>>()
    viewManagers.add(HybridNitroLunarRangePickerManager())
    return viewManagers
  }

  companion object {
    init {
      NitroLunarRangePickerOnLoad.initializeNative()
    }
  }
}

