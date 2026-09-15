import UIKit

/// DayCell: Ô hiển thị một ngày trong lưới lịch (Collection View Cell).
/// Quản lý việc vẽ: Dải màu nền kết nối (range highlight), Vòng tròn ngày được chọn (badge tròn), Số ngày dương và Chữ ngày âm.
class DayCell: UICollectionViewCell {
    /// 1. Dải màu nền chữ nhật chạy ngang dùng để kết nối các ngày nằm trong khoảng được chọn
    private let rangeHighlightView = UIView()
    
    /// 2. Hình tròn màu nổi bật dùng để bọc ngày Bắt đầu (Start), Kết thúc (End) hoặc ngày Chọn đơn (Single)
    private let circleBackgroundView = UIView()
    
    /// 3. Nhãn hiển thị số ngày dương lịch (1, 2, 3... 31)
    private let solarLabel = UILabel()
    
    /// 4. Nhãn hiển thị ngày âm lịch bên dưới (ví dụ: "15", "1/8", "Tết", "Vu Lan")
    private let lunarLabel = UILabel()
    
    /// Vị trí của ngày này trong khoảng chọn hiện tại (.none, .single, .start, .middle, .end)
    private var currentPosition: RangePosition = .none
    
    /// Cờ bật/tắt hiển thị dòng ngày âm lịch
    private var isShowLunar: Bool = true
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupViews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    /// Khởi tạo và thiết lập thuộc tính ban đầu cho các View thành phần
    private func setupViews() {
        // contentView không cắt tràn (clipsToBounds = false) để dải màu nối các ô ngày không bị gãy mép
        contentView.clipsToBounds = false
        
        // --- 1. Dải màu nối khoảng chọn (Range Highlight) ---
        rangeHighlightView.isHidden = true
        contentView.addSubview(rangeHighlightView)
        
        // --- 2. Vòng tròn nền ngày được chọn (Circle Badge) ---
        circleBackgroundView.isHidden = true
        contentView.addSubview(circleBackgroundView)
        
        // --- 3. Nhãn ngày dương lịch ---
        // - fontSize = 15: Cỡ chữ ngày dương (chuẩn 14-16pt để dễ nhìn trên điện thoại)
        // - weight = .semibold: Chữ hơi đậm để nổi bật hơn chữ âm lịch
        // -> Muốn chữ to hơn: tăng lên 16 hoặc 17
        // -> Muốn chữ mỏng hơn: đổi .semibold thành .regular hoặc .medium
        solarLabel.font = .systemFont(ofSize: 15, weight: .semibold)
        solarLabel.textAlignment = .center
        solarLabel.adjustsFontSizeToFitWidth = false
        contentView.addSubview(solarLabel)
        
        // --- 4. Nhãn ngày âm lịch ---
        // - fontSize = 9: Cỡ chữ nhỏ (subtext 8-10pt) đặt phía dưới ngày dương
        // - weight = .regular: Kiểu chữ nét mảnh
        // -> Muốn chữ âm to hơn: tăng lên 10 hoặc 11 (lưu ý: nếu to quá sẽ dễ chạm vào chữ dương lịch)
        lunarLabel.font = .systemFont(ofSize: 9, weight: .regular)
        lunarLabel.textAlignment = .center
        lunarLabel.lineBreakMode = .byClipping
        contentView.addSubview(lunarLabel)
    }
    
    /// Chuẩn bị tái sử dụng ô khi cuộn màn hình
    override func prepareForReuse() {
        super.prepareForReuse()
        clear()
    }
    
    /// Xoá trắng ô (dành cho các ô trống ở đầu/cuối tháng hoặc khi tái sử dụng cell)
    func clear() {
        currentPosition = .none
        solarLabel.text = nil
        lunarLabel.text = nil
        rangeHighlightView.isHidden = true
        circleBackgroundView.isHidden = true
        isUserInteractionEnabled = false
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        updateLayout()
    }
    
    /// Tính toán kích thước, vị trí tọa độ (geometry math) của badge tròn, dải nối và chữ
    private func updateLayout() {
        let cellW = bounds.width   // Chiều rộng ô ngày (thường = bounds.width / 7, khoảng 44-55pt tùy màn hình)
        let cellH = bounds.height  // Chiều cao ô ngày (được định nghĩa cố định là 48pt ở LunarRangePickerView)
        guard cellW > 0 && cellH > 0 else { return }
        
        // --- CẤU HÌNH VÒNG TRÒN CHỌN NGÀY (CIRCLE BADGE) ---
        // - badgeSize = min(cellW - 4, 38): Đường kính vòng tròn chọn ngày.
        //   + Trừ 4: Chừa 2pt đệm mỗi bên mép trái/phải để vòng tròn không bị dính sát vào mép viền của ô lân cận.
        //   + 38pt: Giới hạn tối đa đường kính là 38pt (với cellH = 48pt, 38pt tạo ra khoảng đệm trên/dưới 5pt cực kỳ cân đối).
        // -> Muốn vòng tròn to hơn: Đổi 38 thành 40 hoặc 42.
        // -> Muốn vòng tròn nhỏ hơn: Đổi 38 thành 32 hoặc 34.
        let badgeSize: CGFloat = min(cellW - 4, 38)
        
        // Công thức căn giữa vòng tròn theo trục Y:
        // (48 - 38) / 2 = 5pt (vòng tròn cách mép trên 5pt và cách mép dưới 5pt)
        let badgeY = (cellH - badgeSize) / 2
        
        // Công thức căn giữa vòng tròn theo trục X:
        let badgeX = (cellW - badgeSize) / 2
        
        // Thiết lập tọa độ và bo tròn thành hình tròn hoàn hảo:
        // cornerRadius = badgeSize / 2 (ví dụ: 38 / 2 = 19pt)
        circleBackgroundView.frame = CGRect(x: badgeX, y: badgeY, width: badgeSize, height: badgeSize)
        circleBackgroundView.layer.cornerRadius = badgeSize / 2
        circleBackgroundView.layer.masksToBounds = true
        
        // --- CẤU HÌNH DẢI MÀU NỐI KHOẢNG CHỌN (RANGE HIGHLIGHT VIEW) ---
        // Chiều cao của dải nối luôn bằng badgeSize (38pt) và tọa độ Y khớp chính xác với badgeY để tạo thành 1 dải đồng nhất.
        switch currentPosition {
        case .none, .single:
            // Không chọn hoặc chỉ chọn 1 ngày duy nhất: Ẩn dải nối
            rangeHighlightView.isHidden = true
            
        case .start:
            // Ngày Bắt đầu của khoảng chọn:
            // Dải màu chỉ vẽ từ TÂM ô (x = cellW / 2) chạy về bên PHẢI (width = cellW / 2).
            // Nửa bên trái để trống vì phía trước không có ngày được chọn.
            rangeHighlightView.isHidden = false
            rangeHighlightView.frame = CGRect(
                x: cellW / 2,
                y: badgeY,
                width: cellW / 2,
                height: badgeSize
            )
            
        case .middle:
            // Các ngày Nằm giữa khoảng chọn:
            // Dải màu phủ kín toàn bộ ô từ mép TRÁI sang mép PHẢI (x = 0, width = cellW)
            // để nối liền liên tục giữa các cột và các tuần.
            rangeHighlightView.isHidden = false
            rangeHighlightView.frame = CGRect(
                x: 0,
                y: badgeY,
                width: cellW,
                height: badgeSize
            )
            
        case .end:
            // Ngày Kết thúc của khoảng chọn:
            // Dải màu chỉ vẽ từ mép TRÁI (x = 0) đến TÂM ô (width = cellW / 2).
            // Nửa bên phải để trống vì phía sau không nằm trong khoảng chọn.
            rangeHighlightView.isHidden = false
            rangeHighlightView.frame = CGRect(
                x: 0,
                y: badgeY,
                width: cellW / 2,
                height: badgeSize
            )
        }
        
        // --- CẤU HÌNH TỌA ĐỘ VÀ KHOẢNG CÁCH CHỮ (SOLAR & LUNAR LABELS) ---
        let hasLunar = isShowLunar && !(lunarLabel.text?.isEmpty ?? true)
        
        // - solarH = 18pt: Chiều cao hộp chữ số ngày dương
        // -> Sửa 18 nếu đổi font chữ ngày dương to hơn (ví dụ fontSize 17 cần solarH khoảng 20)
        let solarH: CGFloat = 18
        
        // - lunarH = 11pt: Chiều cao hộp chữ ngày âm
        // -> Sửa 11 nếu đổi font chữ ngày âm to hơn
        let lunarH: CGFloat = 11
        
        // - spacing = 1.0pt: Khoảng cách dọc giữa đáy chữ ngày dương và đỉnh chữ ngày âm
        // -> Muốn 2 dòng chữ xa nhau hơn: tăng lên 2.0 hoặc 3.0
        // -> Muốn 2 dòng chữ sát nhau hơn: giảm về 0 hoặc 0.5
        let spacing: CGFloat = 1.0
        
        if hasLunar {
            // Trường hợp: Có hiển thị cả ngày Âm lịch:
            // Tổng chiều cao cả 2 dòng chữ: 18 + 1.0 + 11 = 30pt.
            let totalH = solarH + spacing + lunarH
            
            // Căn giữa cả cụm chữ theo chiều dọc ô ngày:
            // startY = (48 - 30) / 2 = 9pt. Cụm chữ bắt đầu cách đỉnh ô 9pt.
            let startY = (cellH - totalH) / 2
            
            // Ngày dương nằm ở nửa trên:
            solarLabel.frame = CGRect(x: 0, y: startY, width: cellW, height: solarH)
            
            // Ngày âm nằm ngay dưới ngày dương, cách một khoảng bằng spacing:
            lunarLabel.frame = CGRect(x: 0, y: startY + solarH + spacing, width: cellW, height: lunarH)
            lunarLabel.isHidden = false
        } else {
            // Trường hợp: Ẩn ngày âm lịch (chỉ hiển thị ngày dương):
            // Căn giữa duy nhất dòng ngày dương theo chiều dọc ô:
            // startY = (48 - 18) / 2 = 15pt.
            let startY = (cellH - solarH) / 2
            solarLabel.frame = CGRect(x: 0, y: startY, width: cellW, height: solarH)
            lunarLabel.isHidden = true
        }
    }
    
    /// Cấu hình dữ liệu và phong cách giao diện (Theme & State) cho ô ngày
    /// - Parameters:
    ///   - date: Ngày dương lịch đang hiển thị
    ///   - lunar: Kết quả tính toán ngày âm tương ứng
    ///   - showLunar: Có hiển thị ngày âm hay không
    ///   - position: Vị trí trong dải chọn (.none, .single, .start, .middle, .end)
    ///   - isDisabled: Ô có bị vô hiệu hóa do vượt ngoài minDate / maxDate không
    ///   - theme: Bộ màu tùy biến từ React Native truyền xuống
    func configure(
        date: Date,
        lunar: LunarDateResult,
        showLunar: Bool,
        position: RangePosition,
        isDisabled: Bool,
        theme: PickerTheme?
    ) {
        // Lấy số ngày (ví dụ: ngày 15 -> hiển thị "15")
        let day = Calendar.current.component(.day, from: date)
        solarLabel.text = "\(day)"
        
        // Lấy tên ngày âm (ví dụ: "15", "1/8", "Tết")
        lunarLabel.text = showLunar ? lunar.lunarDayName : nil
        self.isShowLunar = showLunar
        self.currentPosition = position
        
        // --- XỬ LÝ BẢNG MÀU SẮC (THEME COLORS) ---
        // Màu chủ đạo (Primary Color): Dùng cho badge chọn, mặc định là xanh iOS #007AFF
        let primaryColor = theme?.primaryColor.map { UIColor(hexString: $0) } ?? UIColor(red: 0/255, green: 122/255, blue: 255/255, alpha: 1)
        
        // Màu chữ khi được chọn (Selected Text Color): Mặc định là trắng (.white)
        let selectedTextColor = theme?.selectedTextColor.map { UIColor(hexString: $0) } ?? .white
        
        // Màu chữ thường (Text Color): Mặc định là đen (.black)
        let textColor = theme?.textColor.map { UIColor(hexString: $0) } ?? .black
        
        // Màu ngày đặc biệt (mùng 1, rằm): Mặc định là đỏ (.systemRed)
        let specialColor = theme?.specialDayColor.map { UIColor(hexString: $0) } ?? UIColor.systemRed
        
        // Màu nền dải nối khoảng chọn (Range Color):
        // Nếu user truyền màu riêng qua theme -> dùng màu đó
        // Nếu không -> lấy primaryColor phủ mờ 18% (alpha = 0.18) để dải màu nền nhẹ nhàng, không che mất chữ
        // -> Muốn dải nối đậm hơn: tăng 0.18 lên 0.25 hoặc 0.3
        let rangeBgColor = theme?.rangeColor.map { UIColor(hexString: $0) } ?? primaryColor.withAlphaComponent(0.18)
        
        // Gán màu nền cho badge tròn và dải nối
        circleBackgroundView.backgroundColor = primaryColor
        rangeHighlightView.backgroundColor = rangeBgColor
        
        // --- TRƯỜNG HỢP Ô BỊ VÔ HIỆU HÓA (DISABLED DO < minDate HOẶC > maxDate) ---
        if isDisabled {
            isUserInteractionEnabled = false // Khóa không cho bấm
            circleBackgroundView.isHidden = true
            rangeHighlightView.isHidden = true
            
            // Làm mờ cả chữ ngày dương và ngày âm sang màu xám nhạt (systemGray4)
            solarLabel.textColor = UIColor.systemGray4
            lunarLabel.textColor = UIColor.systemGray4
            lunarLabel.font = .systemFont(ofSize: 9, weight: .regular)
            updateLayout()
            return
        }
        
        // Mở quyền tương tác click chọn
        isUserInteractionEnabled = true
        
        // --- THIẾT LẬP GIAO DIỆN THEO TRẠNG THÁI VỊ TRÍ CHỌN ---
        switch position {
        case .none:
            // 1. Ngày bình thường (không được chọn, không nằm trong dải)
            circleBackgroundView.isHidden = true
            solarLabel.textColor = textColor // Chữ ngày dương màu tiêu chuẩn
            
            // Nếu là ngày rằm (15) hoặc mùng 1 âm lịch: Tô chữ ngày âm màu đỏ đặc biệt và in đậm
            if lunar.isSpecialDay {
                lunarLabel.textColor = specialColor
                lunarLabel.font = .systemFont(ofSize: 9, weight: .bold)
            } else {
                // Ngày thường: Tô chữ ngày âm màu xám tiêu chuẩn
                lunarLabel.textColor = UIColor.systemGray
                lunarLabel.font = .systemFont(ofSize: 9, weight: .regular)
            }
            
        case .single:
            // 2. Chọn một ngày duy nhất
            circleBackgroundView.isHidden = false // Hiện badge tròn
            solarLabel.textColor = selectedTextColor // Chữ trắng
            lunarLabel.textColor = selectedTextColor.withAlphaComponent(0.9) // Chữ âm mờ nhẹ 90%
            lunarLabel.font = .systemFont(ofSize: 9, weight: .medium)
            
        case .start:
            // 3. Ngày bắt đầu của khoảng chọn (Start Date)
            circleBackgroundView.isHidden = false // Hiện badge tròn
            solarLabel.textColor = selectedTextColor // Chữ trắng
            lunarLabel.textColor = selectedTextColor.withAlphaComponent(0.9)
            lunarLabel.font = .systemFont(ofSize: 9, weight: .medium)
            
        case .middle:
            // 4. Các ngày nằm GIỮA khoảng chọn
            circleBackgroundView.isHidden = true // Không hiện badge tròn, chỉ có dải nối rangeHighlightView
            // Chữ ngày dương và âm đổi sang màu primaryColor để tương phản nổi bật trên nền mờ 18%
            solarLabel.textColor = primaryColor
            lunarLabel.textColor = primaryColor.withAlphaComponent(0.85)
            lunarLabel.font = .systemFont(ofSize: 9, weight: .medium)
            
        case .end:
            // 5. Ngày kết thúc của khoảng chọn (End Date)
            circleBackgroundView.isHidden = false // Hiện badge tròn
            solarLabel.textColor = selectedTextColor // Chữ trắng
            lunarLabel.textColor = selectedTextColor.withAlphaComponent(0.9)
            lunarLabel.font = .systemFont(ofSize: 9, weight: .medium)
        }
        
        updateLayout()
    }
}
