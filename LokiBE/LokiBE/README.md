# LokiBE (Backend)

REST API cho app Loki: Spring Boot 4, Java 21, MySQL, Spring Security + JWT, Firebase Admin (push).

## Cấu hình

`application.properties` không chứa bí mật. Giá trị bí mật lấy từ biến môi trường:

| Biến | Ý nghĩa |
|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL (mặc định `localhost:3306/Loki`, user `root`) |
| `JWT_SECRET` | Khoá ký JWT (bắt buộc) |
| `GOOGLE_CLIENT_ID` | OAuth Web Client ID, dùng để kiểm tra idToken của Google |
| `FIREBASE_CONFIG_PATH` | File khoá Firebase Admin SDK (mặc định `secrets/...json`, thư mục `secrets/` không commit) |

Trên máy dev: không đặt profile nào thì Spring dùng profile `dev`, đọc thêm `src/main/resources/application-dev.properties` (đã nằm trong `.gitignore`, chứa mật khẩu DB và `jwt.secret` cho máy dev). Production đặt biến môi trường thay cho file này.

Database tạo bảng/cột tự động (`ddl-auto=update`). Các thay đổi mới: bảng `device_tokens`, `reports`; cột `client_id` trên `photos` và `messages`. Cột `users.fcm_token` không còn dùng (token chuyển sang `device_tokens`; app tự gửi lại token khi mở).

## Quy ước

- Controller → Service (interface + Impl) → Repository. Luôn trả DTO, không trả Entity.
- `UserMapper.toProfile` dùng cho người khác (**không có email**); `toSelf` chỉ cho chính mình (`/me`, đăng nhập).
- Thời gian trả về luôn kèm múi giờ của server, VD `2026-11-18T10:00:00+07:00` (`Config/JacksonConfig`).
- Gửi ảnh / tin nhắn nhận `clientId` (UUID của app): gửi lại cùng `clientId` thì trả lại bản đã lưu, không tạo bản trùng, không push lại.
- Quan hệ `BLOCKED`: `requesterId` luôn là người chặn. Chỉ người chặn mới bỏ chặn được.
- Giới hạn bạn bè: `AppConstants.MAX_FRIENDS = 20`, kiểm tra cả hai phía khi kết bạn.
- Push: data-only, gửi tới mọi thiết bị của người nhận (`DeviceTokenService`); token FCM báo không hợp lệ thì tự xoá.

## API

Tất cả cần header `Authorization: Bearer <jwt>`, trừ `/api/auth/**`.

| Method | Path | Mô tả |
|---|---|---|
| POST | `/api/auth/register`, `/login`, `/google` | Đăng ký / đăng nhập (kèm `fcmToken` của máy) |
| POST | `/api/auth/refresh` | Đổi token còn hạn lấy token mới |
| GET, PUT, DELETE | `/api/users/me` | Hồ sơ của mình / sửa (tên, avatar, fcmToken) / xoá tài khoản |
| PUT | `/api/users/me/password` | Đổi mật khẩu (`currentPassword`, `newPassword`), chỉ tài khoản email |
| POST | `/api/users/logout` | Body `{fcmToken}`: ngừng push tới máy đó (bỏ trống = mọi máy) |
| GET | `/api/users/search?email=`, `/api/users/{id}` | Tìm người (người đã chặn mình trả 404) |
| GET | `/api/friends`, `/api/friends/requests/pending`, `/api/friends/blocked` | Bạn bè, lời mời, người đã chặn |
| POST | `/api/friends/requests`, `/api/friends/requests/respond` | Gửi / trả lời lời mời |
| DELETE | `/api/friends/{friendshipId}`, `/api/friends/users/{friendId}` | Từ chối lời mời / huỷ kết bạn |
| POST, DELETE | `/api/friends/users/{userId}/block` | Chặn / bỏ chặn |
| POST | `/api/reports` | Báo cáo `{userId, photoId?, reason?}` |
| POST | `/api/photos` | Gửi ảnh `{imageUrl, caption, recipientIds, clientId}` |
| GET | `/api/photos/history?page=&friendId=` | Lịch sử (ảnh gửi + nhận), bỏ ảnh của người bị chặn |
| GET | `/api/photos/feed`, `/sent`, `/widget` | Ảnh nhận được / đã gửi / mới nhất cho widget |
| GET | `/api/photos/{id}/activity` | Chỉ người gửi: ai đã xem, thả emoji gì |
| PUT | `/api/photos/{id}/seen` | Đánh dấu đã xem |
| POST, DELETE | `/api/photos/reactions`, `/api/photos/{id}/reactions` | Thả / gỡ emoji |
| DELETE | `/api/photos/{id}` | Xoá ảnh của mình |
| GET, POST, PUT | `/api/conversations`, `/messages`, `/{id}/messages`, `/{id}/read` | Chat (tin nhắn có `clientId`) |

## Chạy & test

```bash
./mvnw spring-boot:run
./mvnw test -Dtest=JacksonConfigTest      # test không cần database
```

`LokiBeApplicationTests` khởi động toàn bộ ứng dụng nên cần MySQL đang chạy.
