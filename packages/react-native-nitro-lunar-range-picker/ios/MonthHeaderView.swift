import UIKit

/// MonthHeaderView: Header hiển thị tiêu đề từng tháng (ví dụ: "Tháng 9, 2026") trong UICollectionView
class MonthHeaderView: UICollectionReusableView {
    /// Label hiển thị văn bản tháng và năm
    let titleLabel = UILabel()
    
    override init(frame: CGRect) {
        super.init(frame: frame)
        
        // Cỡ chữ tiêu đề tháng: 16pt, kiểu chữ đậm (bold)
        // -> Muốn đổi độ to/nhỏ của chữ tiêu đề tháng: thay đổi 16
        // -> Muốn đổi độ đậm (ví dụ nhẹ hơn): đổi .bold thành .semibold hoặc .medium
        titleLabel.font = .systemFont(ofSize: 16, weight: .bold)
        
        // Màu chữ mặc định: đen (.black). Giá trị này có thể bị ghi đè bởi theme.textColor trong LunarRangePickerView
        titleLabel.textColor = .black
        
        // Thêm label vào view của header
        addSubview(titleLabel)
    }
    
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
    
    override func layoutSubviews() {
        super.layoutSubviews()
        
        // Căn lề và vị trí hiển thị của tiêu đề tháng:
        // - x = 16: Thụt lề trái 16pt (chuẩn padding của iOS, giúp tiêu đề thẳng hàng với lề lịch)
        // - y = 4: Cách mép trên của header 4pt (tạo khoảng cách nhẹ với tháng liền trước)
        // - width = bounds.width - 32: Chiều rộng khả dụng sau khi trừ 16pt lề trái và 16pt lề phải
        // - height = bounds.height - 4: Chiều cao của label (bounds.height do headerReferenceSize = 38pt quyết định)
        // -> Nếu muốn căn giữa tiêu đề tháng: Đổi x = 0, width = bounds.width và thêm titleLabel.textAlignment = .center
        titleLabel.frame = CGRect(
            x: 16,
            y: 4,
            width: bounds.width - 32,
            height: bounds.height - 4
        )
    }
}
