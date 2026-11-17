# Loki (Android)

Ứng dụng chia sẻ ảnh tức thì với bạn bè (giống Locket). Kotlin + Jetpack Compose, MVVM, Hilt, Retrofit, Room, DataStore, WorkManager, Glance.

## Tính năng

- **Ảnh**: chụp bằng camera trong app (flash, đổi camera, zoom) hoặc chọn ảnh có sẵn từ thư viện; caption; gửi cho mọi người hoặc chọn từng bạn.
- **Lịch sử**: vuốt dọc qua từng ảnh, xem dạng lưới, lọc theo bạn; ảnh tải trước và xem được khi offline.
- **Tương tác**: thả 4 emoji nhanh hoặc **emoji bất kỳ** (nút ＋); trả lời ảnh sang chat; trên ảnh của mình, bấm "hoạt động" để xem **ai đã xem, lúc nào, thả gì**.
- **Lưu & chia sẻ**: lưu ảnh về thư viện máy (Pictures/Loki), chia sẻ ảnh sang app khác.
- **Chat**: hộp thư, tin nhắn; tin mới đến qua push, ngoài ra tải lại mỗi 15 giây khi đang mở chat.
- **Bạn bè**: tìm theo email, link mời `loki://add/<id>`, lời mời kết bạn, tối đa **20 bạn** (giống Locket).
- **An toàn**: **chặn / bỏ chặn**, **báo cáo** người dùng hoặc một ảnh. Người bị chặn không tìm thấy bạn, không nhắn tin, không gửi lời mời được, và ảnh của họ biến mất khỏi lịch sử.
- **Tài khoản**: email + mật khẩu hoặc Google; **đổi mật khẩu**; đổi tên, ảnh đại diện; xoá tài khoản.
- **Offline**: ảnh và tin gửi lúc mất mạng nằm trong outbox, WorkManager tự gửi khi có mạng; mỗi lần gửi có `clientId` nên gửi lại không bao giờ tạo bản trùng.
- **Thông báo & widget**: push FCM data-only, đăng nhập **nhiều máy** đều nhận push; mỗi cuộc trò chuyện một thông báo; widget màn hình chính hiện ảnh mới nhất.

## Cấu trúc

```
com.example.loki
├── LokiApp.kt, MainActivity.kt   # Application (Hilt, WorkManager, Coil) + single Activity
├── core/util/                    # Nén ảnh, thumbnail Cloudinary, kiểm tra emoji
├── data/
│   ├── local/
│   │   ├── datastore/            # SessionStore (JWT), SettingsStore (cài đặt)
│   │   ├── db/                   # Room: LokiDatabase (+ Migration), CacheConverters
│   │   │   ├── dao/              # PhotoDao, ChatDao, UserDao, OutboxDao
│   │   │   └── entity/           # Bảng cache, bảng outbox, mapper DTO <-> entity
│   │   └── LocalCache.kt         # Xoá dữ liệu của tài khoản (đăng xuất, token hết hạn, đổi tài khoản)
│   ├── remote/
│   │   ├── api/                  # Retrofit interfaces
│   │   ├── dto/                  # Request/response khớp với backend
│   │   ├── interceptor/          # Gắn Bearer token, xử lý 401
│   │   ├── auth/                 # Google Sign-In (Credential Manager)
│   │   ├── storage/              # Upload ảnh lên Cloudinary
│   │   └── ApiErrors.kt          # safeCall, toUserMessage
│   ├── repository/               # Session, Photo, Chat, User, Friend, Outbox (gửi + hàng đợi), PhotoExport
│   └── worker/                   # OutboxWorker
├── di/                           # Hilt modules (Network, Database)
├── push/                         # FCM: service, PushType, PushEvents
├── widget/                       # Widget màn hình chính (Glance)
└── ui/
    ├── components/, theme/, navigation/
    └── camera/, home/, chat/, friends/, login/, profile/   # mỗi màn: Screen + ViewModel
```

Luồng dữ liệu: `Screen (Compose) → ViewModel → Repository → Api / Dao`. ViewModel **không** gọi Retrofit API trực tiếp.

Mỗi file Kotlin mở đầu bằng khối chú thích `TỔNG QUAN FILE` (vai trò, thành phần chính, lưu ý). Sửa hành vi của file thì cập nhật khối này.

## Quy ước quan trọng

- **Offline-first**: màn hình đọc Room; mọi kết quả API ghi vào Room trước khi lên UI.
- **Gửi ảnh / tin** luôn qua `OutboxRepository.sendPhoto` / `sendMessage`. Lỗi tạm thời (mất mạng, 401, 408, 429, 5xx) → giữ trong hàng đợi để thử lại; lỗi bị từ chối (4xx khác, Cloudinary từ chối) → bỏ mục đó để không chặn hàng đợi (`isRetryable`).
- **Thời gian** từ backend có múi giờ (`2026-11-18T10:00:00+07:00`); hiển thị bằng `timeAgo` / `clockTime` theo giờ của máy.
- **Phiên đăng nhập**: 401 chỉ đăng xuất nếu đúng là token hiện tại (`clearIfToken`), kèm xoá cache tài khoản; refresh token không ghi đè nếu người dùng đã đăng xuất (`replaceToken`).
- **Database**: đổi entity thì tăng `version`, viết `Migration` trong `LokiDatabase.kt` và commit file schema trong `app/schemas/`.

## Cấu hình

Các giá trị theo máy đọc từ `local.properties` (không commit), có mặc định cho emulator:

```properties
loki.apiUrl=http://10.0.2.2:8080/          # debug; dùng IP LAN của máy nếu chạy trên điện thoại thật
loki.apiUrlRelease=https://api.your-domain.com/
loki.googleWebClientId=xxx.apps.googleusercontent.com
loki.cloudinaryCloudName=your-cloud-name
loki.cloudinaryUploadPreset=loki_unsigned
```

- Bản **debug** cho phép HTTP (src/debug/res/xml/network_security_config.xml); bản **release** chỉ gọi HTTPS.
- Firebase: đặt `app/google-services.json` (tải từ Firebase Console). Khoá Admin SDK là của backend, không đặt trong repo này.
- Sao lưu: token đăng nhập, database cache và outbox bị loại khỏi Auto Backup / chuyển máy (`res/xml/backup_rules.xml`, `data_extraction_rules.xml`).

## Build & test

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew assembleRelease      # R8 bật: rút gọn + làm rối code (keep rule trong app/proguard-rules.pro)
```

Unit test hiện có: `ToUserMessageTest`, `HistorySyncTest`, `OutboxRetryTest`, `PushTypeTest`, `ServerTimeTest`, `EmojiTest`.

Sau khi đổi thư viện hoặc DTO, nên chạy thử bản release trên máy thật: R8 có thể làm hỏng code dùng reflection.

## Trước khi phát hành

- Đổi `applicationId` (Play Store không nhận `com.example.*`), rồi đăng ký lại package mới ở Firebase và Google OAuth (SHA-1).
- Thêm signing config cho release, tăng `versionCode` mỗi lần phát hành.
- Cloudinary đang dùng upload preset **unsigned**: ai biết tên preset đều upload được. Nên để backend ký từng lần upload.
- Link mời đang là `loki://add/<id>`; khi có domain nên chuyển sang App Link `https://`.

## Chưa làm (so với Locket)

| Tính năng | Lý do chưa làm |
|---|---|
| Video ngắn | Cần CameraX VideoCapture, upload video, trình phát (media3), widget ảnh tĩnh. Khối lượng lớn |
| Đăng ký bằng số điện thoại, đồng bộ danh bạ | Cần dịch vụ SMS/OTP (tốn phí) và chính sách quyền riêng tư cho danh bạ |
| Username, link mời https | Cần domain và App Links |
| Quên mật khẩu | Cần dịch vụ gửi email |
| Widget theo từng bạn, nhiều widget | Cần màn cấu hình widget và lưu cấu hình theo GlanceId |
| Caption kiểu Locket (màu, nhạc, vị trí, thời tiết), streak, recap | Tính năng phụ |
| Chat realtime (WebSocket), phân trang tin cũ, "đã xem" trong chat | Hiện dùng push + polling 15 giây, 50 tin gần nhất |
| Gói trả phí, tài khoản người nổi tiếng | Mảng kinh doanh |
| Đa ngôn ngữ | Chữ giao diện đang viết thẳng trong code (tiếng Việt); cần chuyển vào `strings.xml`, và backend cũng trả câu lỗi tiếng Việt |
