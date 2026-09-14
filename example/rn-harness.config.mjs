import { execSync } from 'node:child_process'
import { androidEmulator, androidPlatform } from '@react-native-harness/platform-android'
import { applePlatform, appleSimulator } from '@react-native-harness/platform-apple'

function getAvailableSimulator() {
  if (process.env.DEVICE_MODEL && process.env.IOS_VERSION) {
    return { name: process.env.DEVICE_MODEL, version: process.env.IOS_VERSION }
  }
  try {
    const raw = execSync('xcrun simctl list devices available --json', { encoding: 'utf8' })
    const data = JSON.parse(raw)
    const candidates = Object.entries(data.devices).flatMap(([runtime, devices]) => {
      const version = runtime.match(/iOS-(\d+(?:-\d+)*)$/)?.[1]?.replaceAll('-', '.')
      if (!version) return []
      return devices
        .filter((d) => d.isAvailable && d.name.startsWith('iPhone'))
        .map((d) => ({ name: d.name, version }))
    })
    candidates.sort(
      (a, b) =>
        b.version.localeCompare(a.version, undefined, { numeric: true }) ||
        b.name.localeCompare(a.name, undefined, { numeric: true })
    )
    if (candidates.length > 0) {
      return {
        name: process.env.DEVICE_MODEL ?? candidates[0].name,
        version: process.env.IOS_VERSION ?? candidates[0].version,
      }
    }
  } catch (e) {
    // fallback if xcrun fails (e.g. non-mac environment)
  }
  return {
    name: process.env.DEVICE_MODEL ?? 'iPhone 16',
    version: process.env.IOS_VERSION ?? '18.0',
  }
}

const availableSimulator = getAvailableSimulator()

const config = {
  entryPoint: './index.js',
  appRegistryComponentName: 'NitroLunarRangePickerExample',
  runners: [
    androidPlatform({
      name: 'android',
      device: androidEmulator(process.env.AVD_NAME ?? 'Pixel_7_API_36', {
        apiLevel: Number(process.env.DEVICE_API_LEVEL ?? '36'),
        profile: process.env.DEVICE_PROFILE ?? 'pixel_7',
        diskSize: process.env.AVD_DISK_SIZE ?? '1G',
        heapSize: process.env.AVD_HEAP_SIZE ?? '1G',
        snapshot: {
          enabled: process.env.CI === 'true',
        },
      }),
      bundleId: 'com.nitrolunarrangepickerexample',
    }),
    applePlatform({
      name: 'ios',
      device: appleSimulator(availableSimulator.name, availableSimulator.version),
      bundleId: 'com.nitrolunarrangepickerexample',
    }),
  ],
  defaultRunner: 'android',
  bridgeTimeout: 300000,
}

export default config
