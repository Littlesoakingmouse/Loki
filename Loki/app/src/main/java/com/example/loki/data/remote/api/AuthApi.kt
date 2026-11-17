/*
 * TỔNG QUAN FILE: Retrofit interface cho các API đăng nhập, đăng ký và gia hạn token (api/auth/...).
 *
 * Thành phần chính:
 * - login, register, loginWithGoogle: trả AuthResponse chứa JWT và thông tin người dùng.
 * - refresh: đổi token còn hạn lấy token mới, gọi mỗi lần mở app.
 *
 * Lưu ý:
 * - Các API này nằm trong permitAll của SecurityConfig bên backend nên không cần token, riêng refresh thì cần.
 * - Đường dẫn và tên trường phải khớp AuthController và các DTO Auth bên backend.
 */
package com.example.loki.data.remote.api

import com.example.loki.data.remote.dto.AuthResponse
import com.example.loki.data.remote.dto.GoogleLoginRequest
import com.example.loki.data.remote.dto.LoginRequest
import com.example.loki.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    // Creates the account on first use
    @POST("api/auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): AuthResponse

    // Needs a still-valid token; returns a fresh one
    @POST("api/auth/refresh")
    suspend fun refresh(): AuthResponse
}
