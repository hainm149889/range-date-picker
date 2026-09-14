import React from 'react'
import { StyleSheet } from 'react-native'
import { describe, it, expect, render } from 'react-native-harness'
import { screen } from '@react-native-harness/ui'
import { NitroLunarRangePicker } from 'react-native-nitro-lunar-range-picker'

describe('NitroLunarRangePicker', () => {
  it('renders the native view', async () => {
    await render(
      <NitroLunarRangePicker
        isRed={true}
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
