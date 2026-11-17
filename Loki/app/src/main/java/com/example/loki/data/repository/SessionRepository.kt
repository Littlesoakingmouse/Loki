/*
 * TỔNG QUAN FILE: Repository phiên đăng nhập: kết hợp API auth với token lưu trên máy.
 *
 * Thành phần chính:
 * - isLoggedIn, userId: Flow lấy từ SessionStore; NavHost dựa vào isLoggedIn để chọn Login hay Home.
 * - login, register, loginWithGoogle: gọi API rồi bắt đầu phiên mới (xoá cache trước, lưu token sau).
 * - refresh: gia hạn token 30 ngày mỗi lần mở app và cập nhật FCM token; offline thì giữ token cũ.
 * - updateFcmToken, logout (chỉ gỡ push của máy này), deleteAccount.
 *
 * Lưu ý:
 * - startSession phải xoá cache trước khi lưu token, nếu không Home có thể hiện ảnh của tài khoản cũ trong
 *   chốc lát.
 * - deleteAccount chỉ xoá dữ liệu trên máy khi backend đồng ý; logout thì luôn xoá dù backend lỗi.
 * - refresh dùng replaceToken: nếu người dùng đã đăng xuất trong lúc chờ backend, token mới bị bỏ qua thay vì
 *   "hồi sinh" phiên cũ.
 * - Chuyển màn hình sau đăng nhập hay đăng xuất xảy ra tự động qua Flow token, không có lệnh điều hướng ở đây.
 */
package com.example.loki.data.repository

import com.example.loki.data.local.LocalCache
import com.example.loki.data.local.datastore.SessionStore
import com.example.loki.data.remote.api.AuthApi
import com.example.loki.data.remote.api.UserApi
import com.example.loki.data.remote.dto.AuthResponse
import com.example.loki.data.remote.dto.GoogleLoginRequest
import com.example.loki.data.remote.dto.LoginRequest
import com.example.loki.data.remote.dto.LogoutRequest
import com.example.loki.data.remote.dto.RegisterRequest
import com.example.loki.data.remote.dto.UpdateProfileRequest
import com.example.loki.push.FirebaseServices
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Combines the auth API with the locally stored token
@Singleton
class SessionRepository @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val sessionStore: SessionStore,
    private val firebase: FirebaseServices,
    private val localCache: LocalCache,
) {
    val isLoggedIn: Flow<Boolean> = sessionStore.token.map { it != null }.distinctUntilChanged()
    val userId: Flow<Long?> = sessionStore.userId

    // A new login never inherits the previous account's cached photos and chats
    private suspend fun startSession(auth: AuthResponse) {
        localCache.clear()
        sessionStore.save(auth)
    }

    suspend fun login(email: String, password: String) {
        startSession(authApi.login(LoginRequest(email.trim(), password, firebase.fcmToken())))
    }

    suspend fun register(email: String, password: String, displayName: String) {
        startSession(
            authApi.register(RegisterRequest(email.trim(), password, displayName.trim(), firebase.fcmToken())),
        )
    }

    suspend fun loginWithGoogle(idToken: String) {
        startSession(authApi.loginWithGoogle(GoogleLoginRequest(idToken, firebase.fcmToken())))
    }

    // Called on app start to extend the 30-day token
    suspend fun refresh() {
        val current = sessionStore.token.first() ?: return
        try {
            // Logged out (or another account logged in) while this was in flight: keep that, drop the refresh
            if (!sessionStore.replaceToken(current, authApi.refresh())) return
            // Keep the backend's push token current: login may have happened before Firebase had one
            firebase.fcmToken()?.let { updateFcmToken(it) }
        } catch (e: IOException) {
            // Offline: keep the old token
        } catch (e: HttpException) {
            // 401 was already handled by AuthInterceptor; other errors keep the old token
        }
    }

    suspend fun updateFcmToken(token: String) {
        if (sessionStore.token.first() != null) userApi.updateMe(UpdateProfileRequest(fcmToken = token))
    }

    // Throws if the backend refuses, so the screen can show why; the local session is only cleared on success
    suspend fun deleteAccount() {
        userApi.deleteMe()
        sessionStore.clear()
        localCache.clear()
    }

    suspend fun logout() {
        try {
            userApi.logout(LogoutRequest(firebase.fcmToken())) // stops push to this phone only
        } catch (e: IOException) {
        } catch (e: HttpException) {
        }
        sessionStore.clear()
        localCache.clear()
    }
}
