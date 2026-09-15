import UIKit
import NitroModules

/// LunarRangePickerView: Giao diện chính của bộ chọn ngày âm dương trên nền tảng iOS.
/// Kế thừa từ UIView và cài đặt UICollectionViewDataSource, UICollectionViewDelegateFlowLayout
/// để hiển thị lịch cuộn dọc với cấu trúc 7 cột (tương ứng 7 ngày trong tuần) và phân chia theo từng tháng.
class LunarRangePickerView: UIView, UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {
    
    // =========================================================================
    // MARK: - Properties từ Nitro Hybrid View (Giao tiếp trực tiếp với React Native)
    // =========================================================================
    
    /// Ngôn ngữ hiển thị (mặc định: .vi - Tiếng Việt). Khi thay đổi:
    /// - Cập nhật lại thanh thứ trong tuần (T2..CN hoặc Mon..Sun)
    /// - Tải lại tiêu đề tháng và nhãn ngày âm
    var language: PickerLanguage = .vi {
        didSet {
            setupWeekdayHeader()
            rebuildCalendarData()
        }
    }
    
    /// Bộ màu giao diện (Theme) từ JS truyền xuống (primaryColor, backgroundColor, textColor...)
    var theme: PickerTheme? {
        didSet {
            applyTheme()
        }
    }
    
    /// Cờ bật/tắt hiển thị dòng chữ ngày âm lịch bên dưới ngày dương
    var showLunarDate: Bool = true {
        didSet {
            collectionView?.reloadData()
        }
    }
    
    /// Ngày bắt đầu tuần (.monday: Thứ 2 đầu tuần, .sunday: Chủ nhật đầu tuần)
    var firstDayOfWeek: FirstDayOfWeek = .monday {
        didSet {
            setupWeekdayHeader()
            rebuildCalendarData()
        }
    }
    
    /// Chế độ hiển thị:
    /// - .multi: Hiển thị danh sách cuộn dọc nhiều tháng (numberOfMonths)
    /// - .single: Chỉ hiển thị 1 tháng duy nhất chứa ngày được chọn
    var displayMode: DisplayMode = .multi {
        didSet {
            rebuildCalendarData()
        }
    }
    
    /// Số lượng tháng hiển thị trong chế độ .multi (Mặc định: 12 tháng)
    /// -> Muốn hiển thị nhiều tháng hơn (ví dụ 24 tháng): truyền từ RN prop numberOfMonths={24}
    var numberOfMonths: Double = 12 {
        didSet {
            rebuildCalendarData()
        }
    }
    
    /// Ngày bắt đầu được chọn (định dạng chuỗi "YYYY-MM-DD" hoặc ISO)
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
                // Tự động cuộn đến tháng chứa ngày bắt đầu khi khởi tạo
                scrollToSelectedDate(animated: false)
            }
        }
    }
    
    /// Ngày kết thúc được chọn (định dạng chuỗi "YYYY-MM-DD" hoặc ISO)
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
    
    /// Ngày tối thiểu được phép chọn (các ngày trước minDate sẽ bị mờ và không bấm được)
    var minDate: String? {
        didSet {
            parsedMinDate = DateUtils.parseDateString(minDate)
            collectionView?.reloadData()
        }
    }
    
    /// Ngày tối đa được phép chọn (các ngày sau maxDate sẽ bị mờ và không bấm được)
    var maxDate: String? {
        didSet {
            parsedMaxDate = DateUtils.parseDateString(maxDate)
            collectionView?.reloadData()
        }
    }
    
    /// Đường dẫn URI hình ảnh icon Close tùy chỉnh từ React Native (local asset hoặc remote URL)
    var closeIconUri: String? {
        didSet {
            loadCloseIcon()
        }
    }
    
    /// Đường dẫn URI icon Confirm tùy chỉnh
    var confirmIconUri: String?
    
    /// Callback gọi về React Native khi người dùng hoàn tất chọn khoảng ngày (trả về DateRangeResult)
    var onConfirm: ((DateRangeResult) -> Void)?
    
    /// Callback gọi về React Native khi người dùng bấm nút Close (đóng modal/view)
    var onClose: (() -> Void)?

    // =========================================================================
    // MARK: - State nội bộ (Internal State)
    // =========================================================================
    
    /// Đối tượng Date đã chuẩn hoá của minDate (00:00:00)
    private var parsedMinDate: Date?
    
    /// Đối tượng Date đã chuẩn hoá của maxDate (00:00:00)
    private var parsedMaxDate: Date?
    
    /// Danh sách dữ liệu các tháng hiển thị (mỗi phần tử chứa thông tin năm, tháng, mảng ngày)
    private var monthSections: [MonthData] = []
    
    /// Ngày bắt đầu được chọn đang lưu trong bộ nhớ Native
    private var selectedStartDate: Date?
    
    /// Ngày kết thúc được chọn đang lưu trong bộ nhớ Native
    private var selectedEndDate: Date?
    
    // =========================================================================
    // MARK: - UI Components
    // =========================================================================
    
    /// Thanh tiêu đề phía trên cùng (chứa Title và nút Close)
    private let topBarView = UIView()
    
    /// Nhãn hiển thị tiêu đề (ví dụ: "Chọn ngày" / "Select Dates")
    private let titleLabel = UILabel()
    
    /// Nút bấm đóng picker (Close Button)
    private let closeButton = UIButton(type: .custom)
    
    /// Thanh ngang chứa 7 nhãn thứ trong tuần (T2, T3... CN)
    private let weekdayHeaderStack = UIStackView()
    
    /// Lưới hiển thị các tháng và ngày (Grid 7 cột)
    private var collectionView: UICollectionView!

    // =========================================================================
    // MARK: - Khởi tạo View (Initialization)
    // =========================================================================
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupUI()
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    /// Khi View được gắn vào Window (mở modal hoặc xuất hiện trên màn hình):
    /// Tiến hành cuộn danh sách đến vị trí tháng có ngày bắt đầu được chọn
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

    // =========================================================================
    // MARK: - Cài đặt giao diện (Setup UI & Geometry Constants)
    // =========================================================================
    
    private func setupUI() {
        // Nền mặc định của picker là màu trắng (sẽ bị ghi đè nếu theme.backgroundColor được truyền)
        backgroundColor = .white
        
        // --- 1. THIẾT LẬP THANH TOP BAR ---
        // - fontSize = 16: Cỡ chữ tiêu đề thanh công cụ
        // - weight = .semibold: Độ đậm vừa phải, tạo cảm giác sang trọng chuẩn iOS
        // -> Muốn đổi chữ to/nhỏ: sửa 16
        titleLabel.font = .systemFont(ofSize: 16, weight: .semibold)
        titleLabel.textAlignment = .center
        titleLabel.textColor = .black
        titleLabel.text = getLocalizedTitle()
        
        // - setTitle("✕"): Ký tự mặc định của nút đóng
        // - fontSize = 18: Kích cỡ ký tự nút đóng (18pt giúp dễ nhìn)
        // - weight = .medium: Nét chữ trung bình
        // - setTitleColor(.systemGray): Màu xám mờ tinh tế
        // -> Muốn đổi ký tự đóng (ví dụ chữ "Đóng" hoặc icon khác): sửa setTitle("✕")
        // -> Muốn đổi màu nút đóng: sửa .systemGray
        closeButton.setTitle("✕", for: .normal)
        closeButton.setTitleColor(.systemGray, for: .normal)
        closeButton.titleLabel?.font = .systemFont(ofSize: 18, weight: .medium)
        closeButton.addTarget(self, action: #selector(handleClose), for: .touchUpInside)
        
        topBarView.addSubview(titleLabel)
        topBarView.addSubview(closeButton)
        addSubview(topBarView)
        
        // --- 2. THIẾT LẬP THANH THỨ TRONG TUẦN (WEEKDAY HEADER STACK) ---
        // UIStackView nằm ngang, tự động chia đều chiều rộng cho 7 cột (fillEqually)
        weekdayHeaderStack.axis = .horizontal
        weekdayHeaderStack.distribution = .fillEqually
        weekdayHeaderStack.alignment = .center
        addSubview(weekdayHeaderStack)
        setupWeekdayHeader()

        // --- 3. THIẾT LẬP LƯỚI LỊCH (COLLECTION VIEW LAYOUT) ---
        let layout = UICollectionViewFlowLayout()
        
        // - minimumInteritemSpacing = 0: Khoảng cách giữa các cột trong 1 hàng = 0pt
        // (Do ta chia đều cellWidth = bounds.width / 7 nên không cần spacing thừa giữa các ô)
        layout.minimumInteritemSpacing = 0
        
        // - minimumLineSpacing = 8: Khoảng cách dọc giữa các hàng (tuần) trong cùng một tháng = 8pt
        // -> Muốn các tuần giãn cách xa hơn: tăng lên 10 hoặc 12pt
        // -> Muốn các tuần xếp khít nhau: giảm về 4 hoặc 6pt
        layout.minimumLineSpacing = 8
        
        // - sectionInset: Khoảng đệm bao quanh mỗi tháng:
        //   + top = 8pt: Khoảng cách từ tiêu đề tháng đến hàng ngày đầu tiên
        //   + bottom = 20pt: Khoảng cách đệm cuối tháng giúp tách biệt với tháng tiếp theo
        // -> Muốn các tháng tách xa nhau hơn: tăng bottom lên 24 hoặc 30pt
        layout.sectionInset = UIEdgeInsets(top: 8, left: 0, bottom: 20, right: 0)
        
        // - headerReferenceSize: Kích thước header của mỗi tháng
        //   + height = 38pt: Chiều cao tiêu đề tháng ("Tháng 9, 2026")
        // -> Muốn tiêu đề tháng cao hơn: sửa 38pt (lưu ý: sửa cả trong heightForSection)
        layout.headerReferenceSize = CGSize(width: bounds.width, height: 38)
        
        collectionView = UICollectionView(frame: .zero, collectionViewLayout: layout)
        collectionView.backgroundColor = .clear
        collectionView.dataSource = self
        collectionView.delegate = self
        collectionView.showsVerticalScrollIndicator = true
        
        // Đăng ký Cell hiển thị ngày và Header hiển thị tháng
        collectionView.register(DayCell.self, forCellWithReuseIdentifier: "DayCell")
        collectionView.register(MonthHeaderView.self, forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader, withReuseIdentifier: "MonthHeader")
        
        addSubview(collectionView)
        
        // Tạo cấu trúc dữ liệu các tháng ban đầu
        rebuildCalendarData()
    }

    /// Trả về tiêu đề tương ứng theo ngôn ngữ được cấu hình
    private func getLocalizedTitle() -> String {
        switch language {
        case .vi: return "Chọn ngày"
        case .zh: return "选择日期"
        case .en: return "Select Dates"
        }
    }

    /// Xây dựng các nhãn hiển thị thứ trong tuần (T2..CN hoặc CN..T7 tùy firstDayOfWeek)
    private func setupWeekdayHeader() {
        // Xóa các nhãn cũ khi đổi ngôn ngữ hoặc đổi ngày đầu tuần
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
            
            // - fontSize = 12: Cỡ chữ nhãn thứ trong tuần (11-13pt nhỏ gọn phía trên lưới)
            // -> Muốn đổi cỡ chữ thứ: sửa 12
            label.font = .systemFont(ofSize: 12, weight: .medium)
            
            // Phân biệt màu ngày Chủ Nhật:
            // Chủ Nhật được tô màu đỏ nhẹ (systemRed mờ 80%), các ngày khác màu xám (.systemGray)
            let isSunday = isMonFirst ? (index == 6) : (index == 0)
            label.textColor = isSunday ? UIColor.systemRed.withAlphaComponent(0.8) : UIColor.systemGray
            weekdayHeaderStack.addArrangedSubview(label)
        }
    }

    // =========================================================================
    // MARK: - Tính toán Layout & Tọa độ hình học (Geometry Math)
    // =========================================================================
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        // --- CÁC HẰNG SỐ KÍCH THƯỚC CỐ ĐỊNH (LAYOUT CONSTANTS) ---
        
        // 1. topBarHeight = 44pt: Chiều cao chuẩn của thanh điều hướng iOS (Apple HIG)
        // -> Muốn thanh bar trên dày hơn: tăng lên 48 hoặc 52pt
        let topBarHeight: CGFloat = 44
        
        // 2. weekdayHeight = 30pt: Chiều cao dải thứ trong tuần (T2..CN)
        // -> Muốn thanh thứ gọn hơn: giảm về 24 hoặc 26pt
        let weekdayHeight: CGFloat = 30
        
        // Tọa độ TopBar: phủ kín toàn bộ chiều rộng từ đỉnh Y = 0
        topBarView.frame = CGRect(x: 0, y: 0, width: bounds.width, height: topBarHeight)
        
        // Nút Close: hình vuông 44x44pt đặt sát mép phải (x = bounds.width - 44)
        // Đảm bảo diện tích chạm tối thiểu 44pt để ngón tay dễ bấm
        closeButton.frame = CGRect(x: bounds.width - 44, y: 0, width: 44, height: topBarHeight)
        
        // Title Label: chừa 44pt bên trái và 44pt bên phải (bounds.width - 88)
        // để tiêu đề luôn nằm chính giữa màn hình và không đè lên nút Close
        titleLabel.frame = CGRect(x: 44, y: 0, width: bounds.width - 88, height: topBarHeight)
        
        // --- TÍNH TOÁN 7 CỘT LỊCH VÀ CĂN GIỮA MÀN HÌNH ---
        // - cellWidth = floor(bounds.width / 7): Lấy chiều rộng màn hình chia cho 7 ngày.
        //   Dùng hàm floor() làm tròn xuống để không bao giờ bị dư phần thập phân gây tràn hàng.
        let cellWidth = floor(bounds.width / 7)
        
        // Tổng chiều rộng của 7 cột lịch kết hợp lại
        let totalGridWidth = cellWidth * 7
        
        // - sideInset: Số pixel dư thừa chia đều ra 2 bên lề trái và phải.
        // Giúp toàn bộ lưới lịch luôn được căn giữa màn hình một cách đối xứng hoàn hảo.
        let sideInset = max(0, (bounds.width - totalGridWidth) / 2)
        
        // Tọa độ thanh thứ: đặt ngay dưới TopBar (y = topBarHeight), thụt lề bằng sideInset
        weekdayHeaderStack.frame = CGRect(x: sideInset, y: topBarHeight, width: totalGridWidth, height: weekdayHeight)
        
        // Tọa độ lưới lịch: bắt đầu ngay dưới thanh thứ (cvY = topBarHeight + weekdayHeight = 74pt)
        let cvY = topBarHeight + weekdayHeight
        collectionView.frame = CGRect(x: 0, y: cvY, width: bounds.width, height: max(0, bounds.height - cvY))
        
        // Cập nhật lại insets của layout CollectionView để áp dụng sideInset
        if let layout = collectionView.collectionViewLayout as? UICollectionViewFlowLayout {
            layout.headerReferenceSize = CGSize(width: bounds.width, height: 38)
            layout.sectionInset = UIEdgeInsets(top: 8, left: sideInset, bottom: 20, right: sideInset)
            layout.minimumInteritemSpacing = 0
            layout.minimumLineSpacing = 8
        }
        
        // Nếu đã có ngày chọn sẵn và ở chế độ cuộn nhiều tháng (.multi): tự động cuộn đến tháng đó
        if bounds.width > 0 && bounds.height > 0 && selectedStartDate != nil && displayMode == .multi {
            scrollToSelectedDate(animated: false)
        }
    }

    /// Áp dụng màu sắc giao diện (Theme) khi nhận được từ React Native
    private func applyTheme() {
        if let theme = theme {
            // Đổi màu nền của picker nếu có truyền backgroundColor
            if let bgHex = theme.backgroundColor {
                self.backgroundColor = UIColor(hexString: bgHex)
            }
            // Đổi màu chữ tiêu đề nếu có truyền textColor
            if let textHex = theme.textColor {
                titleLabel.textColor = UIColor(hexString: textHex)
            }
        }
        titleLabel.text = getLocalizedTitle()
        collectionView?.reloadData()
    }

    /// Tải hình ảnh icon tùy chỉnh cho nút Close từ URL / Image URI
    private func loadCloseIcon() {
        guard let closeUri = closeIconUri, let url = URL(string: closeUri) else { return }
        DispatchQueue.global().async {
            if let data = try? Data(contentsOf: url), let img = UIImage(data: data) {
                DispatchQueue.main.async {
                    // Gán ảnh mới và xoá bỏ text "✕" mặc định
                    self.closeButton.setImage(img, for: .normal)
                    self.closeButton.setTitle(nil, for: .normal)
                }
            }
        }
    }

    // =========================================================================
    // MARK: - Logic Tính Chiều Cao Section & Cuộn Ngày (Scrolling Calculation)
    // =========================================================================
    
    /// Tính toán trước chiều cao thực tế của một Section (Tháng) trong CollectionView.
    /// Hàm này cực kỳ quan trọng giúp tính chính xác độ lệch Y (Offset) khi cuộn đến tháng mong muốn mà không bị giật.
    private func heightForSection(_ section: Int) -> CGFloat {
        guard section < monthSections.count else { return 0 }
        
        // Tổng số ô trong tháng (bao gồm cả các ô trống đầu tháng để căn đúng thứ)
        let daysCount = monthSections[section].days.count
        
        // Số hàng tuần trong tháng: chia 7 và làm tròn lên ceil (thường là 5 hoặc 6 hàng)
        let rows = CGFloat(ceil(Double(daysCount) / 7.0))
        
        // - headerH = 38pt: Chiều cao tiêu đề tháng
        let headerH: CGFloat = 38
        
        // - topInset = 8pt: Khoảng cách từ tiêu đề tháng đến hàng ngày đầu tiên
        let topInset: CGFloat = 8
        
        // - bottomInset = 20pt: Khoảng cách đệm cuối tháng
        let bottomInset: CGFloat = 20
        
        // - rowH = 48pt: Chiều cao mỗi hàng ô ngày (khớp với height 48pt của DayCell)
        let rowH: CGFloat = 48
        
        // - lineSpacing = 8pt: Khoảng cách giữa các hàng tuần kề nhau
        let lineSpacing: CGFloat = 8
        
        // Tổng chiều cao của tất cả các hàng tuần:
        // rows * rowH + (rows - 1) khoảng cách giữa các hàng
        let itemsH = rows * rowH + max(0, rows - 1) * lineSpacing
        
        // Tổng chiều cao của toàn bộ tháng = Header + Đệm trên + Các hàng ngày + Đệm dưới
        return headerH + topInset + itemsH + bottomInset
    }

    /// Cuộn CollectionView đến đúng tháng chứa ngày bắt đầu (selectedStartDate)
    func scrollToSelectedDate(animated: Bool = false) {
        guard displayMode == .multi, let start = selectedStartDate else { return }
        let calendar = Calendar.current
        let comp = calendar.dateComponents([.year, .month], from: start)
        guard let targetYear = comp.year, let targetMonth = comp.month else { return }
        
        // Tìm chỉ số Section của tháng cần cuộn tới
        guard let sectionIndex = monthSections.firstIndex(where: { $0.year == targetYear && $0.month == targetMonth }), sectionIndex > 0 else { return }
        
        DispatchQueue.main.async { [weak self] in
            guard let self = self, self.collectionView != nil, self.bounds.width > 0, self.bounds.height > 0 else { return }
            
            // Tính tổng chiều cao của tất cả các Section nằm phía trên Section mục tiêu
            var targetY: CGFloat = 0
            for i in 0..<sectionIndex {
                targetY += self.heightForSection(i)
            }
            
            // Giới hạn targetY không vượt quá độ cuộn tối đa của CollectionView để tránh bị overscroll
            let maxOffsetY = max(0, self.collectionView.contentSize.height - self.collectionView.bounds.height)
            let finalY = min(targetY, maxOffsetY)
            self.collectionView.setContentOffset(CGPoint(x: 0, y: finalY), animated: animated)
        }
    }

    // =========================================================================
    // MARK: - Xây dựng dữ liệu lịch (Data Generation)
    // =========================================================================
    
    /// Tạo cấu trúc danh sách các tháng và các ngày trong từng tháng
    private func rebuildCalendarData() {
        monthSections.removeAll()
        let calendar = Calendar.current
        let today = Date()
        
        // Nếu ở chế độ .multi thì tạo đủ numberOfMonths (mặc định 12), nếu .single thì chỉ tạo 1 tháng
        let totalMonths = (displayMode == .multi) ? max(1, Int(numberOfMonths)) : 1
        
        let baseDate: Date
        if displayMode == .single, let start = selectedStartDate {
            baseDate = start
        } else {
            baseDate = today
        }
        
        // Danh sách các tháng: Tháng hiện tại (baseDate) ở trên cùng, sau đó là các tháng trước đó (-1, -2, ...)
        // -> Nếu muốn hiển thị các tháng tương lai (sau này): đổi -i thành +i
        var monthsList: [Date] = []
        for i in 0..<totalMonths {
            if let mDate = calendar.date(byAdding: .month, value: -i, to: baseDate) {
                monthsList.append(mDate)
            }
        }
        
        for mDate in monthsList {
            let comp = calendar.dateComponents([.year, .month], from: mDate)
            guard let year = comp.year, let month = comp.month else { continue }
            
            // Lấy ngày mùng 1 của tháng để xác định thứ trong tuần
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
            
            // Tính số ô trống đầu tháng cần chèn vào trước ngày mùng 1:
            // - Nếu Thứ 2 đầu tuần: Chủ Nhật (weekday = 1) sẽ là cột cuối cùng (index 6)
            // - Nếu Chủ Nhật đầu tuần: Chủ Nhật sẽ là cột đầu tiên (index 0)
            let isMonFirst = (firstDayOfWeek == .monday)
            let leadingEmptyDays = isMonFirst
                ? (weekdayOfFirstDay - 2 + 7) % 7
                : (weekdayOfFirstDay - 1) % 7
            
            // Mảng chứa các ngày trong tháng (bắt đầu bằng các phần tử nil tương ứng số ô trống)
            var days: [Date?] = Array(repeating: nil, count: leadingEmptyDays)
            for d in 1...numberOfDays {
                var dayComp = DateComponents()
                dayComp.year = year
                dayComp.month = month
                dayComp.day = d
                if let dayDate = calendar.date(from: dayComp) {
                    // Chuẩn hoá về 00:00:00 để so sánh không bị lệch giờ phút giây
                    days.append(DateUtils.cleanToStartOfDay(dayDate))
                }
            }
            
            // Tiêu đề tháng theo ngôn ngữ
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

    /// Xử lý sự kiện bấm nút đóng Close
    @objc private func handleClose() {
        onClose?()
    }

    // =========================================================================
    // MARK: - UICollectionView DataSource
    // =========================================================================
    
    /// Số lượng Section: bằng số lượng tháng được tạo (mặc định 12)
    func numberOfSections(in collectionView: UICollectionView) -> Int {
        return monthSections.count
    }

    /// Số lượng ô trong mỗi tháng: bằng tổng số ngày + số ô trống đệm đầu tháng
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return monthSections[section].days.count
    }

    /// Cung cấp Header tiêu đề tháng cho mỗi Section
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

    /// Cung cấp Cell hiển thị cho từng ngày trong tháng
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: "DayCell", for: indexPath) as! DayCell
        let date = monthSections[indexPath.section].days[indexPath.item]
        
        if let date = date {
            let cleanDate = DateUtils.cleanToStartOfDay(date)
            let langStr = language.stringValue
            // Tính toán âm lịch từ ngày dương
            let lunar = LunarCalendarHelper.convertSolarToLunar(date: cleanDate, language: langStr)
            
            // --- KIỂM TRA TRẠNG THÁI KHÓA (DISABLED) THEO minDate / maxDate ---
            var isDisabled = false
            if let max = parsedMaxDate, cleanDate > max {
                isDisabled = true // Vượt quá ngày tối đa cho phép
            }
            if let min = parsedMinDate, cleanDate < min {
                isDisabled = true // Nhỏ hơn ngày tối thiểu cho phép
            }
            
            // Xác định vị trí của ngày trong khoảng chọn (.none, .single, .start, .middle, .end)
            let position = getRangePosition(for: cleanDate)
            
            // Cấu hình hiển thị ô ngày
            cell.configure(
                date: cleanDate,
                lunar: lunar,
                showLunar: showLunarDate,
                position: position,
                isDisabled: isDisabled,
                theme: theme
            )
        } else {
            // Ô trống ở đầu tháng: Xoá trắng nội dung
            cell.clear()
        }
        
        return cell
    }

    // =========================================================================
    // MARK: - Logic Xác Định Vị Trí Khoảng Chọn (Range Position Logic)
    // =========================================================================
    
    /// Xác định xem một ngày bất kỳ có vị trí nào trong khoảng chọn:
    /// - .none: Nằm ngoài khoảng chọn
    /// - .single: Mới chỉ chọn 1 ngày (hoặc ngày bắt đầu và kết thúc trùng nhau)
    /// - .start: Là ngày bắt đầu của khoảng
    /// - .middle: Là các ngày nằm ở giữa
    /// - .end: Là ngày kết thúc của khoảng
    private func getRangePosition(for date: Date) -> RangePosition {
        let target = DateUtils.cleanToStartOfDay(date)
        
        // Chưa chọn ngày bắt đầu nào -> .none
        guard let start = selectedStartDate else { return .none }
        let cleanStart = DateUtils.cleanToStartOfDay(start)
        
        // Đã chọn ngày bắt đầu nhưng chưa chọn ngày kết thúc -> Nếu đúng ngày bắt đầu thì là .single
        guard let end = selectedEndDate else {
            return (target == cleanStart) ? .single : .none
        }
        let cleanEnd = DateUtils.cleanToStartOfDay(end)
        
        let minD = min(cleanStart, cleanEnd)
        let maxD = max(cleanStart, cleanEnd)
        
        // Ngày bắt đầu và kết thúc trùng một ngày
        if minD == maxD {
            return (target == minD) ? .single : .none
        }
        
        // Phân loại vị trí
        if target == minD {
            return .start
        } else if target == maxD {
            return .end
        } else if target > minD && target < maxD {
            return .middle
        }
        
        return .none
    }

    // =========================================================================
    // MARK: - UICollectionView DelegateFlowLayout & Click Logic
    // =========================================================================
    
    /// Quy định kích thước mỗi ô ngày (Size per item):
    /// - width = floor(bounds.width / 7): Chiều rộng bằng 1/7 màn hình
    /// - height = 48pt: Chiều cao chuẩn của mỗi ô ngày trong lịch
    /// -> Muốn ô ngày cao/thấp hơn: sửa 48pt (lưu ý: sửa cả trong heightForSection và DayCell)
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let width = floor(bounds.width / 7)
        return CGSize(width: width, height: 48)
    }

    /// Xử lý sự kiện click chọn ngày của người dùng:
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        // Bỏ qua nếu click vào ô trống đầu tháng
        guard let date = monthSections[indexPath.section].days[indexPath.item] else { return }
        let cleanDate = DateUtils.cleanToStartOfDay(date)
        
        // Bỏ qua nếu click vào các ngày bị vô hiệu hóa (disabled)
        if let max = parsedMaxDate, cleanDate > max { return }
        if let min = parsedMinDate, cleanDate < min { return }
        
        // --- THUẬT TOÁN CHỌN KHOẢNG NGÀY (DATE RANGE SELECTION LOGIC) ---
        // 1. Chưa chọn gì HOẶC đã có đủ cả start & end -> Bắt đầu một lượt chọn mới:
        if selectedStartDate == nil || (selectedStartDate != nil && selectedEndDate != nil) {
            selectedStartDate = cleanDate
            selectedEndDate = nil
        } 
        // 2. Đang có ngày bắt đầu, người dùng chọn tiếp ngày thứ hai:
        else if let start = selectedStartDate {
            // Nếu người dùng chọn lùi (ngày mới chọn nhỏ hơn ngày start):
            // Tự động hoán đổi ngày mới chọn làm ngày bắt đầu mới, chờ người dùng chọn tiếp ngày kết thúc
            if cleanDate < start {
                selectedStartDate = cleanDate
                selectedEndDate = nil
            } else {
                // Chọn tiến (ngày mới chọn >= start): Hoàn tất chọn khoảng ngày!
                selectedEndDate = cleanDate
                // Bắn kết quả về React Native thông qua callback onConfirm
                dispatchConfirmResult(start: start, end: cleanDate)
            }
        }
        
        // Cập nhật lại giao diện để vẽ lại màu sắc dải chọn
        collectionView.reloadData()
    }

    /// Đóng gói dữ liệu kết quả (DateRangeResult) gồm ngày dương và âm lịch để gửi về React Native
    private func dispatchConfirmResult(start: Date, end: Date) {
        let langStr = language.stringValue
        let startLunar = LunarCalendarHelper.convertSolarToLunar(date: start, language: langStr)
        let endLunar = LunarCalendarHelper.convertSolarToLunar(date: end, language: langStr)

        let calendar = Calendar.current
        let startComp = calendar.dateComponents([.year, .month, .day], from: start)
        let endComp = calendar.dateComponents([.year, .month, .day], from: end)

        // Thông tin ngày bắt đầu
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

        // Thông tin ngày kết thúc
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

        // Kích hoạt callback onConfirm gửi về React Native
        onConfirm?(DateRangeResult(startDate: startDateInfo, endDate: endDateInfo))
    }
}