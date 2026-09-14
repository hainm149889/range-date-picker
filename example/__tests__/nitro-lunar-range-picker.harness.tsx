import React from 'react'
import { StyleSheet } from 'react-native'
import { describe, it, expect, render } from 'react-native-harness'
import { screen } from '@react-native-harness/ui'
import { callback } from 'react-native-nitro-modules'
import { NitroLunarRangePicker } from 'react-native-nitro-lunar-range-picker'

describe('NitroLunarRangePicker', () => {
  it('renders the native view', async () => {
    await render(
      <NitroLunarRangePicker
        language="vi"
        showLunarDate={true}
        onConfirm={callback(() => {})}
        onClose={callback(() => {})}
        style={styles.view}
        testID="nitro-lunar-range-picker"
      />
    )

    const view = await screen.findByTestId('nitro-lunar-range-picker')

    expect(view.nativeId).toBeDefined()
  })
})

const styles = StyleSheet.create({
  view: {
    width: 200,
    height: 200,
  },
})
