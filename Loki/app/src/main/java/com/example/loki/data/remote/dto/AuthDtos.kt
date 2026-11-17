/*
 * TỔNG QUAN FILE: DTO (object JSON) cho đăng nhập và người dùng.
 *
 * Thành phần chính:
 * - LoginRequest, RegisterRequest, GoogleLoginRequest: body gửi lên, kèm fcmToken để backend gửi push.
 * - AuthResponse: accessToken (JWT) và user trả về sau khi đăng nhập.
 * - UserDto: thông tin người dùng; thuộc tính name chọn displayName, rồi email, rồi "User <id>".
 *   email chỉ có với hồ sơ của chính mình (backend ẩn email người khác). authProvider "LOCAL" = tài khoản có
 *   mật khẩu (đổi được mật khẩu), "GOOGLE" = đăng nhập Google.
 *
 * Lưu ý:
 * - Chỉ khai báo trường app dùng; trường lạ bị bỏ qua nhờ ignoreUnknownKeys trong NetworkModule.
 * - Trường có thể thiếu trong JSON phải có giá trị mặc định, nếu không việc parse sẽ lỗi.
 * - UserDto còn được lưu dạng JSON trong Room (CacheConverters), đổi tên trường sẽ làm hỏng cache cũ.
 */
package com.example.loki.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors the backend's Dto classes; only the fields the app uses are declared
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val fcmToken: String? = null,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
    val fcmToken: String? = null,
)

@Serializable
data class GoogleLoginRequest(
    val idToken: String,
    val fcmToken: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Long,
    val email: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val authProvider: String? = null,
) {
    val name get() = displayName ?: email ?: "User $id"

    // Email + password account: the only kind that can change its password
    val hasPassword get() = authProvider == "LOCAL"
}
