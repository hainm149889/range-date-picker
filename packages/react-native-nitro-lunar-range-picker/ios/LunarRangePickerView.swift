import UIKit
import NitroModules

class LunarRangePickerView: UIView, UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {
    // Properties từ Nitro Hybrid View
    var language: PickerLanguage = .vi {
        didSet {
            setupWeekdayHeader()
            rebuildCalendarData()
        }
    }
    var theme: PickerTheme? {
        didSet {
            applyTheme()
        }
    }
    var showLunarDate: Bool = true {
        didSet {
            collectionView?.reloadData()
        }
    }
    var firstDayOfWeek: FirstDayOfWeek = .monday {
        didSet {
            setupWeekdayHeader()
            rebuildCalendarData()
        }
    }
    var displayMode: DisplayMode = .multi {
        didSet {
            rebuildCalendarData()
        }
    }
    var numberOfMonths: Double = 12 {
        didSet {
            rebuildCalendarData()
        }
    }
    var startDate: String? {
        didSet {
            if let s = startDate, !s.isEmpty {
                selectedStartDate = DateUtils.parseDateString(s)
            } else {
                selectedStartDate = nil
            }
            if displayMode == .single {
                rebuildCalendarData()
            } else {
                collectionView?.reloadData()
                scrollToSelectedDate(animated: false)
            }
        }
    }
    var endDate: String? {
        didSet {
            if let s = endDate, !s.isEmpty {
                selectedEndDate = DateUtils.parseDateString(s)
            } else {
                selectedEndDate = nil
            }
            collectionView?.reloadData()
        }
    }
    var minDate: String? {
        didSet {
            parsedMinDate = DateUtils.parseDateString(minDate)
            collectionView?.reloadData()
        }
    }
    var maxDate: String? {
        didSet {
            parsedMaxDate = DateUtils.parseDateString(maxDate)
            collectionView?.reloadData()
        }
    }
    var closeIconUri: String? {
        didSet {
            loadCloseIcon()
        }
    }
    var confirmIconUri: String?
    var onConfirm: ((DateRangeResult) -> Void)?
    var onClose: (() -> Void)?

    private var parsedMinDate: Date?
    private var parsedMaxDate: Date?
    private var monthSections: [MonthData] = []
    private var selectedStartDate: Date?
    private var selectedEndDate: Date?
    
    // UI Elements
    private let topBarView = UIView()
    private let titleLabel = UILabel()
    private let closeButton = UIButton(type: .custom)
    private let weekdayHeaderStack = UIStackView()
    private var collectionView: UICollectionView!

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func didMoveToWindow() {
        super.didMoveToWindow()
        if window != nil {
            if displayMode == .single {
                rebuildCalendarData()
            } else {
                collectionView?.reloadData()
                if selectedStartDate != nil {
                    scrollToSelectedDate(animated: false)
                }
            }
        }
    }

    private func setupUI() {
        backgroundColor = .white
        
        // 1. Top Bar
        titleLabel.font = .systemFont(ofSize: 16, weight: .semibold)
        titleLabel.textAlignment = .center
        titleLabel.textColor = .black
        titleLabel.text = getLocalizedTitle()
        
        closeButton.setTitle("✕", for: .normal)
        closeButton.setTitleColor(.systemGray, for: .normal)
        closeButton.titleLabel?.font = .systemFont(ofSize: 18, weight: .medium)
        closeButton.addTarget(self, action: #selector(handleClose), for: .touchUpInside)
        
        topBarView.addSubview(titleLabel)
        topBarView.addSubview(closeButton)
        addSubview(topBarView)
        
        // 2. Weekday Header Stack
        weekdayHeaderStack.axis = .horizontal
        weekdayHeaderStack.distribution = .fillEqually
        weekdayHeaderStack.alignment = .center
        addSubview(weekdayHeaderStack)
        setupWeekdayHeader()

        // 3. Grid Layout cho Calendar
        let layout = UICollectionViewFlowLayout()
        layout.minimumInteritemSpacing = 0
        layout.minimumLineSpacing = 8
        layout.sectionInset = UIEdgeInsets(top: 8, left: 0, bottom: 20, right: 0)
        layout.headerReferenceSize = CGSize(width: bounds.width, height: 38)
        
        collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        collectionView.backgroundColor = .clear
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.showsVerticalScrollIndicator = true
        collectionView.register(DayCell.self, forCellWithReuseIdentifier: "DayCell")
        collectionView.register(MonthHeaderView.self, forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader, withReuseIdentifier: "MonthHeader")
        
        addSubview(collectionView)
        
        rebuildCalendarData()
    }

    private func getLocalizedTitle() -> String {
        switch language {
        case .vi: return "Chọn ngày"
        case .zh: return "选择日期"
        case .en: return "Select Dates"
        }
    }

    private func setupWeekdayHeader() {
        weekdayHeaderStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
        
        let weekdayNames: [String]
        let isMonFirst = (firstDayOfWeek == .monday)
        
        switch language {
        case .vi:
            weekdayNames = isMonFirst
                ? ["T2", "T3", "T4", "T5", "T6", "T7", "CN"]
                : ["CN", "T2", "T3", "T4", "T5", "T6", "T7"]
        case .zh:
            weekdayNames = isMonFirst
                ? ["一", "二", "三", "四", "五", "六", "日"]
                : ["日", "一", "二", "三", "四", "五", "六"]
        case .en:
            weekdayNames = isMonFirst
                ? ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
                : ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
        }

        for (index, name) in weekdayNames.enumerated() {
            let label = UILabel()
            label.text = name
            label.textAlignment = .center
            label.font = .systemFont(ofSize: 12, weight: .medium)
            
            let isSunday = isMonFirst ? (index == 6) : (index == 0)
            label.textColor = isSunday ? UIColor.systemRed.withAlphaComponent(0.8) : UIColor.systemGray
            weekdayHeaderStack.addArrangedSubview(label)
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        let topBarHeight: CGFloat = 44
        let weekdayHeight: CGFloat = 30
        
        topBarView.frame = CGRect(x: 0, y: 0, width: bounds.width, height: topBarHeight)
        closeButton.frame = CGRect(x: bounds.width - 44, y: 0, width: 44, height: topBarHeight)
        titleLabel.frame = CGRect(x: 44, y: 0, width: bounds.width - 88, height: topBarHeight)
        
        let cellWidth = floor(bounds.width / 7)
        let totalGridWidth = cellWidth * 7
        let sideInset = max(0, (bounds.width - totalGridWidth) / 2)
        
        weekdayHeaderStack.frame = CGRect(x: sideInset, y: topBarHeight, width: totalGridWidth, height: weekdayHeight)
        
        let cvY = topBarHeight + weekdayHeight
        collectionView.frame = CGRect(x: 0, y: cvY, width: bounds.width, height: max(0, bounds.height - cvY))
        
        if let layout = collectionView.collectionViewLayout as? UICollectionViewFlowLayout {
            layout.headerReferenceSize = CGSize(width: bounds.width, height: 38)
            layout.sectionInset = UIEdgeInsets(top: 8, left: sideInset, bottom: 20, right: sideInset)
            layout.minimumInteritemSpacing = 0
            layout.minimumLineSpacing = 8
        }
        
        if bounds.width > 0 && bounds.height > 0 && selectedStartDate != nil && displayMode == .multi {
            scrollToSelectedDate(animated: false)
        }
    }

    private func applyTheme() {
        if let theme = theme {
            if let bgHex = theme.backgroundColor {
                self.backgroundColor = UIColor(hexString: bgHex)
            }
            if let textHex = theme.textColor {
                titleLabel.textColor = UIColor(hexString: textHex)
            }
        }
        titleLabel.text = getLocalizedTitle()
        collectionView?.reloadData()
    }

    private func loadCloseIcon() {
        guard let closeUri = closeIconUri, let url = URL(string: closeUri) else { return }
        DispatchQueue.global().async {
            if let data = try? Data(contentsOf: url), let img = UIImage(data: data) {
                DispatchQueue.main.async {
                    self.closeButton.setImage(img, for: .normal)
                    self.closeButton.setTitle(nil, for: .normal)
                }
            }
        }
    }

    private func heightForSection(_ section: Int) -> CGFloat {
        guard section < monthSections.count else { return 0 }
        let daysCount = monthSections[section].days.count
        let rows = CGFloat(ceil(Double(daysCount) / 7.0))
        let headerH: CGFloat = 38
        let topInset: CGFloat = 8
        let bottomInset: CGFloat = 20
        let rowH: CGFloat = 48
        let lineSpacing: CGFloat = 8
        let itemsH = rows * rowH + max(0, rows - 1) * lineSpacing
        return headerH + topInset + itemsH + bottomInset
    }

    func scrollToSelectedDate(animated: Bool = false) {
        guard displayMode == .multi, let start = selectedStartDate else { return }
        let calendar = Calendar.current
        let comp = calendar.dateComponents([.year, .month], from: start)
        guard let targetYear = comp.year, let targetMonth = comp.month else { return }
        
        guard let sectionIndex = monthSections.firstIndex(where: { $0.year == targetYear && $0.month == targetMonth }), sectionIndex > 0 else { return }
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self, self.collectionView != nil, self.bounds.width > 0, self.bounds.height > 0 else { return }
            
            var targetY: CGFloat = 0
            for i in 0..<sectionIndex {
                targetY += self.heightForSection(i)
            }
            
            let maxOffsetY = max(0, self.collectionView.contentSize.height - self.collectionView.bounds.height)
            let finalY = min(targetY, maxOffsetY)
            self.collectionView.setContentOffset(CGPoint(x: 0, y: finalY), animated: animated)
        }
    }

    private func rebuildCalendarData() {
        monthSections.removeAll()
        let calendar = Calendar.current
        let today = Date()
        let totalMonths = (displayMode == .multi) ? max(1, Int(numberOfMonths)) : 1
        
        let baseDate: Date
        if displayMode == .single, let start = selectedStartDate {
            baseDate = start
        } else {
            baseDate = today
        }
        
        // Tháng hiện tại (hoặc baseDate) ở trên cùng, sau đó là các tháng trước đó (-1, -2, ...)
        var monthsList: [Date] = []
        for i in 0..<totalMonths {
            if let mDate = calendar.date(byAdding: .month, value: -i, to: baseDate) {
                monthsList.append(mDate)
            }
        }
        
        for mDate in monthsList {
            let comp = calendar.dateComponents([.year, .month], from: mDate)
            guard let year = comp.year, let month = comp.month else { continue }
            
            var firstDayComp = DateComponents()
            firstDayComp.year = year
            firstDayComp.month = month
            firstDayComp.day = 1
            guard let firstDayOfMonth = calendar.date(from: firstDayComp),
                  let range = calendar.range(of: .day, in: .month, for: firstDayOfMonth) else {
                continue
            }
            
            let numberOfDays = range.count
            let weekdayOfFirstDay = calendar.component(.weekday, from: firstDayOfMonth)
            
            let isMonFirst = (firstDayOfWeek == .monday)
            let leadingEmptyDays = isMonFirst
                ? (weekdayOfFirstDay - 2 + 7) % 7
                : (weekdayOfFirstDay - 1) % 7
            
            var days: [Date?] = Array(repeating: nil, count: leadingEmptyDays)
            for d in 1...numberOfDays {
                var dayComp = DateComponents()
                dayComp.year = year
                dayComp.month = month
                dayComp.day = d
                if let dayDate = calendar.date(from: dayComp) {
                    days.append(DateUtils.cleanToStartOfDay(dayDate))
                }
            }
            
            let monthTitle: String
            switch language {
            case .vi:
                monthTitle = "Tháng \(month), \(year)"
            case .zh:
                monthTitle = "\(year)年 \(month)月"
            case .en:
                let monthNames = ["", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"]
                monthTitle = "\(monthNames[month]) \(year)"
            }
            
            monthSections.append(MonthData(year: year, month: month, title: monthTitle, days: days))
        }
        
        collectionView?.reloadData()
    }

    @objc private func handleClose() {
        onClose?()
    }

    // MARK: - UICollectionView DataSource
    func numberOfSections(in collectionView: UICollectionView) -> Int {
        return monthSections.count
    }

    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return monthSections[section].days.count
    }

    func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String, at indexPath: IndexPath) -> UICollectionReusableView {
        if kind == UICollectionView.elementKindSectionHeader {
            let header = collectionView.dequeueReusableSupplementaryView(ofKind: kind, withReuseIdentifier: "MonthHeader", for: indexPath) as! MonthHeaderView
            let sectionData = monthSections[indexPath.section]
            header.titleLabel.text = sectionData.title
            if let textHex = theme?.textColor {
                header.titleLabel.textColor = UIColor(hexString: textHex)
            }
            return header
        }
        return UICollectionReusableView()
    }

    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "DayCell", for: indexPath) as! DayCell
        let date = monthSections[indexPath.section].days[indexPath.item]
        
        if let date = date {
            let cleanDate = DateUtils.cleanToStartOfDay(date)
            let langStr = language.stringValue
            let lunar = LunarCalendarHelper.convertSolarToLunar(date: cleanDate, language: langStr)
            
            // Kiểm tra trạng thái disabled theo minDate / maxDate
            var isDisabled = false
            if let max = parsedMaxDate, cleanDate > max {
                isDisabled = true
            }
            if let min = parsedMinDate, cleanDate < min {
                isDisabled = true
            }
            
            let position = getRangePosition(for: cleanDate)
            cell.configure(
                date: cleanDate,
                lunar: lunar,
                showLunar: showLunarDate,
                position: position,
                isDisabled: isDisabled,
                theme: theme
            )
        } else {
            cell.clear()
        }
        
        return cell
    }

    private func getRangePosition(for date: Date) -> RangePosition {
        let target = DateUtils.cleanToStartOfDay(date)
        
        guard let start = selectedStartDate else { return .none }
        let cleanStart = DateUtils.cleanToStartOfDay(start)
        
        guard let end = selectedEndDate else {
            return (target == cleanStart) ? .single : .none
        }
        let cleanEnd = DateUtils.cleanToStartOfDay(end)
        
        let minD = min(cleanStart, cleanEnd)
        let maxD = max(cleanStart, cleanEnd)
        
        if minD == maxD {
            return (target == minD) ? .single : .none
        }
        
        if target == minD {
            return .start
        } else if target == maxD {
            return .end
        } else if target > minD && target < maxD {
            return .middle
        }
        
        return .none
    }

    // MARK: - UICollectionView DelegateFlowLayout
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let width = floor(bounds.width / 7)
        return CGSize(width: width, height: 48)
    }

    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        guard let date = monthSections[indexPath.section].days[indexPath.item] else { return }
        let cleanDate = DateUtils.cleanToStartOfDay(date)
        
        // Chặn chọn các ngày bị disable
        if let max = parsedMaxDate, cleanDate > max { return }
        if let min = parsedMinDate, cleanDate < min { return }
        
        if selectedStartDate == nil || (selectedStartDate != nil && selectedEndDate != nil) {
            selectedStartDate = cleanDate
            selectedEndDate = nil
        } else if let start = selectedStartDate {
            if cleanDate < start {
                selectedStartDate = cleanDate
                selectedEndDate = nil
            } else {
                selectedEndDate = cleanDate
                dispatchConfirmResult(start: start, end: cleanDate)
            }
        }
        
        collectionView.reloadData()
    }

    private func dispatchConfirmResult(start: Date, end: Date) {
        let langStr = language.stringValue
        let startLunar = LunarCalendarHelper.convertSolarToLunar(date: start, language: langStr)
        let endLunar = LunarCalendarHelper.convertSolarToLunar(date: end, language: langStr)

        let calendar = Calendar.current
        let startComp = calendar.dateComponents([.year, .month, .day], from: start)
        let endComp = calendar.dateComponents([.year, .month, .day], from: end)

        let startDateInfo = DateInfo(
            year: Double(startComp.year ?? 2026),
            month: Double(startComp.month ?? 1),
            day: Double(startComp.day ?? 1),
            timestamp: start.timeIntervalSince1970 * 1000,
            lunarDay: Double(startLunar.day),
            lunarMonth: Double(startLunar.month),
            lunarYear: Double(startLunar.year),
            isLeapMonth: startLunar.isLeap,
            lunarDayName: startLunar.lunarDayName
        )

        let endDateInfo = DateInfo(
            year: Double(endComp.year ?? 2026),
            month: Double(endComp.month ?? 1),
            day: Double(endComp.day ?? 1),
            timestamp: end.timeIntervalSince1970 * 1000,
            lunarDay: Double(endLunar.day),
            lunarMonth: Double(endLunar.month),
            lunarYear: Double(endLunar.year),
            isLeapMonth: endLunar.isLeap,
            lunarDayName: endLunar.lunarDayName
        )

        onConfirm?(DateRangeResult(startDate = startDateInfo, endDate = endDateInfo))
    }
}