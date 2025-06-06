# Ứng Dụng Thương Mại Điện Tử với Jetpack Compose

Một ứng dụng thương mại điện tử Android hiện đại được xây dựng bằng Jetpack Compose, với giao diện người dùng đẹp và tương tác thời gian thực.

## 📱 Tính Năng

- **Giao Diện Hiện Đại với Jetpack Compose**: 
  - Giao diện người dùng trực quan và đẹp mắt
  - Hoàn toàn được xây dựng bằng Jetpack Compose
- **Quản Lý Sản Phẩm**: 
  - Xem chi tiết sản phẩm với hình ảnh và mô tả
  - Lựa chọn kích thước và màu sắc
  - Quản lý tồn kho
  - Giá động với hỗ trợ giảm giá
- **Giỏ Hàng**: 
  - Chức năng giỏ hàng đầy đủ
  - Quản lý số lượng sản phẩm
- **Hệ Thống Đánh Giá**:
  - Đánh giá bằng sao
  - Viết nhận xét
  - Tải lên hình ảnh đánh giá
  - Cập nhật đánh giá thời gian thực
- **Xác Thực Người Dùng**: Bảo mật với Firebase Auth
- **Cập Nhật Thời Gian Thực**: Đồng bộ hóa dữ liệu với Firebase Firestore

## 🛠 Công Nghệ Sử Dụng

- **Jetpack Compose**: Công cụ hiện đại để xây dựng giao diện Android
- **Kotlin**: Mã nguồn 100% Kotlin
- **Firebase**:
  - Xác thực Firebase
  - Cloud Firestore
  - Firebase Storage
- **Thành Phần Kiến Trúc**:
  - ViewModel
  - Coroutines cho xử lý bất đồng bộ
  - StateFlow cho cập nhật UI
- **Thư Viện Khác**:
  - Coil để tải hình ảnh
  - Hilt để tiêm phụ thuộc
  - Material Design

## 🚀 Bắt Đầu

### Yêu Cầu Hệ Thống

- Android Studio Arctic Fox trở lên
- JDK 11 trở lên
- Android SDK với API level tối thiểu 21

### Cài Đặt

1. Clone dự án:
```bash
git clone https://github.com/yourusername/E-commerce-Complete-Jetpack-Compose-UI.git
```

2. Mở dự án trong Android Studio

3. Cấu hình Firebase:
   - Tạo dự án Firebase mới
   - Thêm ứng dụng Android vào dự án Firebase
   - Tải xuống `google-services.json` và đặt vào thư mục app
   - Kích hoạt Authentication và Firestore trong Firebase Console

4. Build và chạy dự án

## 📦 Cấu Trúc Dự Án

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/eritlab/jexmon/
│   │   │   ├── domain/           # Tầng domain với các model
│   │   │   ├── presentation/     # Tầng UI với màn hình và viewmodel
│   │   │   └── data/            # Tầng data với repositories
│   │   └── res/                 # Tài nguyên
└── build.gradle                 # Dependency của dự án
```

## 🖥 Màn Hình

1. Màn hình giới thiệu (Onboarding)
2. Đăng nhập
3. Quên mật khẩu
4. Đăng ký
5. Hoàn thiện hồ sơ
6. Xác thực OTP
7. Trang chủ
8. Chi tiết sản phẩm
9. Đơn hàng
10. Hồ sơ người dùng
11. Thanh điều hướng dưới

## 📚 Thư Viện Sử Dụng

1. [Dagger Hilt](https://github.com/google/dagger) - Tiêm phụ thuộc
2. [Coroutine](https://github.com/Kotlin/kotlinx.coroutines) - Xử lý bất đồng bộ
3. Firebase - Backend và xác thực
4. Jetpack Navigation - Điều hướng ứng dụng

## 🤝 Đóng Góp

Chúng tôi rất hoan nghênh mọi đóng góp! Hãy thoải mái gửi Pull Request.

## 📝 Giấy Phép

Dự án này được cấp phép theo Giấy phép MIT - xem file [LICENSE](LICENSE) để biết thêm chi tiết.

## 📧 Liên Hệ

Nếu bạn có bất kỳ câu hỏi hoặc góp ý nào, vui lòng liên hệ: [Trungnghia7115@gmail.com]

---
⭐️ Nếu bạn thấy dự án này hữu ích, hãy cho chúng tôi một sao nhé!
