#include <jni.h>
#include <fbjni/fbjni.h>
#include "NitroLunarRangePickerOnLoad.hpp"

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
  return facebook::jni::initialize(vm, []() {
    margelo::nitro::nitrolunarrangepicker::registerAllNatives();
  });
}