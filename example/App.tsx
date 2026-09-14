import React from 'react';
import { View, StyleSheet } from 'react-native';
import { NitroLunarRangePicker } from 'react-native-nitro-lunar-range-picker';

function App(): React.JSX.Element {
  return (
    <View style={styles.container}>
        <NitroLunarRangePicker isRed={true} style={styles.view} testID="nitro-lunar-range-picker" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  view: {
    width: 200,
    height: 200
  }});

export default App;