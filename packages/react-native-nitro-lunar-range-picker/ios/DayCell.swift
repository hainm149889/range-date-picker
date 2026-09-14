import UIKit

class DayCell: UICollectionViewCell {
    private let rangeHighlightView = UIView()
    private let circleBackgroundView = UIView()
    private let solarLabel = UILabel()
    private let lunarLabel = UILabel()
    
    private var currentPosition: RangePosition = .none
    private var isShowLunar: Bool = true
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        setupViews()
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    private func setupViews() {
        contentView.clipsToBounds = false
        
        // 1. Dải màu nối giữa hai ngày
        rangeHighlightView.isHidden = true
        contentView.addSubview(rangeHighlightView)
        
        // 2. Vòng tròn/nền nổi bật cho ngày start & end & single
        circleBackgroundView.isHidden = true
        contentView.addSubview(circleBackgroundView)
        
        // 3. Ngày dương lịch
        solarLabel.font = .systemFont(ofSize: 15, weight: .semibold)
        solarLabel.textAlignment = .center
        solarLabel.adjustsFontSizeToFitWidth = false
        contentView.addSubview(solarLabel)
        
        // 4. Ngày âm lịch
        lunarLabel.font = .systemFont(ofSize: 9, weight: .regular)
        lunarLabel.textAlignment = .center
        lunarLabel.lineBreakMode = .byClipping
        contentView.addSubview(lunarLabel)
    }
    
    override func prepareForReuse() {
        super.prepareForReuse()
        clear()
    }
    
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
    
    private func updateLayout() {
        let cellW = bounds.width
        let cellH = bounds.height
        guard cellW > 0 && cellH > 0 else { return }
        
        let badgeSize: CGFloat = min(cellW - 4, 38)
        let badgeY = (cellH - badgeSize) / 2
        let badgeX = (cellW - badgeSize) / 2
        
        circleBackgroundView.frame = CGRect(x: badgeX, y: badgeY, width: badgeSize, height: badgeSize)
        circleBackgroundView.layer.cornerRadius = badgeSize / 2
        circleBackgroundView.layer.masksToBounds = true
        
        switch currentPosition {
        case .none, .single:
            rangeHighlightView.isHidden = true
        case .start:
            rangeHighlightView.isHidden = false
            rangeHighlightView.frame = CGRect(
                x: cellW / 2,
                y: badgeY,
                width: cellW / 2,
                height: badgeSize
            )
        case .middle:
            rangeHighlightView.isHidden = false
            rangeHighlightView.frame = CGRect(
                x: 0,
                y: badgeY,
                width: cellW,
                height: badgeSize
            )
        case .end:
            rangeHighlightView.isHidden = false
            rangeHighlightView.frame = CGRect(
                x: 0,
                y: badgeY,
                width: cellW / 2,
                height: badgeSize
            )
        }
        
        let hasLunar = isShowLunar && !(lunarLabel.text?.isEmpty ?? true)
        let solarH: CGFloat = 18
        let lunarH: CGFloat = 11
        let spacing: CGFloat = 1.0
        
        if hasLunar {
            let totalH = solarH + spacing + lunarH // 30pt
            let startY = (cellH - totalH) / 2
            solarLabel.frame = CGRect(x: 0, y: startY, width: cellW, height: solarH)
            lunarLabel.frame = CGRect(x: 0, y: startY + solarH + spacing, width: cellW, height: lunarH)
            lunarLabel.isHidden = false
        } else {
            let startY = (cellH - solarH) / 2
            solarLabel.frame = CGRect(x: 0, y: startY, width: cellW, height: solarH)
            lunarLabel.isHidden = true
        }
    }
    
    func configure(
        date: Date,
        lunar: LunarDateResult,
        showLunar: Bool,
        position: RangePosition,
        isDisabled: Bool,
        theme: PickerTheme?
    ) {
        let day = Calendar.current.component(.day, from: date)
        solarLabel.text = "\(day)"
        lunarLabel.text = showLunar ? lunar.lunarDayName : nil
        self.isShowLunar = showLunar
        self.currentPosition = position
        
        // Colors
        let primaryColor = theme?.primaryColor.map { UIColor(hexString: $0) } ?? UIColor(red: 0/255, green: 122/255, blue: 255/255, alpha: 1)
        let selectedTextColor = theme?.selectedTextColor.map { UIColor(hexString: $0) } ?? .white
        let textColor = theme?.textColor.map { UIColor(hexString: $0) } ?? .black
        let specialColor = theme?.specialDayColor.map { UIColor(hexString: $0) } ?? UIColor.systemRed
        let rangeBgColor = theme?.rangeColor.map { UIColor(hexString: $0) } ?? primaryColor.withAlphaComponent(0.18)
        
        circleBackgroundView.backgroundColor = primaryColor
        rangeHighlightView.backgroundColor = rangeBgColor
        
        if isDisabled {
            isUserInteractionEnabled = false
            circleBackgroundView.isHidden = true
            rangeHighlightView.isHidden = true
            solarLabel.textColor = UIColor.systemGray4
            lunarLabel.textColor = UIColor.systemGray4
            lunarLabel.font = .systemFont(ofSize: 9, weight: .regular)
            updateLayout()
            return
        }
        
        isUserInteractionEnabled = true
        
        switch position {
        case .none:
            circleBackgroundView.isHidden = true
            solarLabel.textColor = textColor
            
            // Ngày đặc biệt (mùng 1 & rằm) có màu đỏ
            if lunar.isSpecialDay {
                lunarLabel.textColor = specialColor
                lunarLabel.font = .systemFont(ofSize: 9, weight: .bold)
            } else {
                lunarLabel.textColor = UIColor.systemGray
                lunarLabel.font = .systemFont(ofSize: 9, weight: .regular)
            }
            
        case .single:
            circleBackgroundView.isHidden = false
            solarLabel.textColor = selectedTextColor
            lunarLabel.textColor = selectedTextColor.withAlphaComponent(0.9)
            lunarLabel.font = .systemFont(ofSize: 9, weight: .medium)
            
        case .start:
            circleBackgroundView.isHidden = false
            solarLabel.textColor = selectedTextColor
            lunarLabel.textColor = selectedTextColor.withAlphaComponent(0.9)
            lunarLabel.font = .systemFont(ofSize: 9, weight: .medium)
            
        case .middle:
            circleBackgroundView.isHidden = true
            solarLabel.textColor = primaryColor
            lunarLabel.textColor = primaryColor.withAlphaComponent(0.85)
            lunarLabel.font = .systemFont(ofSize: 9, weight: .medium)
            
        case .end:
            circleBackgroundView.isHidden = false
            solarLabel.textColor = selectedTextColor
            lunarLabel.textColor = selectedTextColor.withAlphaComponent(0.9)
            lunarLabel.font = .systemFont(ofSize: 9, weight: .medium)
        }
        
        updateLayout()
    }
}
