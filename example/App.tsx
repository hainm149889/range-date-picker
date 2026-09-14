import React, { useState } from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  TouchableOpacity,
} from 'react-native';
import {
  LunarRangePicker,
  type DateRangeResult,
} from 'react-native-nitro-lunar-range-picker';

export default function App() {
  const [selectedRange, setSelectedRange] = useState<string>(
    'Chưa chọn khoảng ngày',
  );
  const [savedResult, setSavedResult] = useState<DateRangeResult | null>(null);
  const [showPicker, setShowPicker] = useState<boolean>(true);

  const handleConfirm = (result: DateRangeResult) => {
    setSavedResult(result);
    const startStr = `${result.startDate.day}/${result.startDate.month}/${result.startDate.year} (Âm: ${result.startDate.lunarDayName})`;
    const endStr = `${result.endDate.day}/${result.endDate.month}/${result.endDate.year} (Âm: ${result.endDate.lunarDayName})`;

    setSelectedRange(`${startStr}\n⬇️\n${endStr}`);
    setShowPicker(false);
  };

  const startDateStr = savedResult
    ? `${savedResult.startDate.year}-${String(savedResult.startDate.month).padStart(2, '0')}-${String(savedResult.startDate.day).padStart(2, '0')}`
    : undefined;

  const endDateStr = savedResult
    ? `${savedResult.endDate.year}-${String(savedResult.endDate.month).padStart(2, '0')}-${String(savedResult.endDate.day).padStart(2, '0')}`
    : undefined;

  return (
    <SafeAreaView style={styles.container}>
      <Text style={styles.title}>Test Nitro Lunar Range Picker</Text>

      <View style={styles.card}>
        <Text style={styles.cardHeader}>Khoảng ngày đã chọn:</Text>
        <Text style={styles.resultText}>{selectedRange}</Text>
      </View>

      <TouchableOpacity
        style={styles.openButton}
        onPress={() => setShowPicker(true)}
      >
        <Text style={styles.openButtonText}>
          📅 Mở Lịch FormSheet (12 tháng)
        </Text>
      </TouchableOpacity>

      <LunarRangePicker
        isModal={true}
        visible={showPicker}
        language="vi"
        showLunarDate={true}
        firstDayOfWeek="monday"
        displayMode="multi"
        numberOfMonths={12}
        startDate={startDateStr}
        endDate={endDateStr}
        maxDate={new Date().toISOString()}
        // theme={{
        //   primaryColor: '#007AFF',
        //   backgroundColor: '#FFFFFF',
        //   textColor: '#1C1C1E',
        //   rangeColor: '#741cdfff',
        //   specialDayColor: '#FF3B30',
        // }}
        onConfirm={handleConfirm}
        onClose={() => setShowPicker(false)}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
    alignItems: 'center',
    paddingTop: 40,
    paddingHorizontal: 20,
  },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: '#000',
    marginBottom: 24,
  },
  card: {
    width: '100%',
    backgroundColor: '#FFF',
    borderRadius: 14,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 2,
    marginBottom: 24,
  },
  cardHeader: {
    fontSize: 13,
    fontWeight: '600',
    color: '#8E8E93',
    textTransform: 'uppercase',
    marginBottom: 8,
  },
  resultText: {
    fontSize: 15,
    fontWeight: '500',
    color: '#1C1C1E',
    lineHeight: 22,
  },
  openButton: {
    backgroundColor: '#007AFF',
    paddingVertical: 14,
    paddingHorizontal: 24,
    borderRadius: 12,
    shadowColor: '#007AFF',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.25,
    shadowRadius: 8,
  },
  openButtonText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '600',
  },
});
