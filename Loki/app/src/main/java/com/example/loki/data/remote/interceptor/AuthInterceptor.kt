/*
 * TỔNG QUAN FILE: OkHttp interceptor gắn JWT vào mọi request gửi tới backend và xử lý token hết hạn.
 *
 * Thành phần chính:
 * - intercept: đọc token từ SessionStore, thêm header "Authorization: Bearer <token>".
 * - Khi backend trả 401 cho đúng token đang dùng: xoá token và toàn bộ dữ liệu cục bộ của tài khoản
 *   (LocalCache), isLoggedIn thành false và app tự quay về màn Login.
 *
 * Lưu ý:
 * - runBlocking chỉ được phép ở đây vì OkHttp gọi intercept trên thread nền, không bao giờ trên main thread.
 *   DataStore giữ giá trị trong bộ nhớ sau lần đọc đầu nên mỗi lần đọc rất nhanh.
 * - clearIfToken: request cũ (của phiên trước) trả 401 muộn sẽ không đá người dùng khỏi phiên mới.
 * - Interceptor này chỉ gắn vào client của Retrofit. Không dùng client đó để gọi dịch vụ khác (ví dụ
 *   Cloudinary), nếu không JWT sẽ bị gửi ra ngoài.
 * - Logic đăng xuất dựa vào mã 401; backend phải trả 401 (không phải 403) khi token sai hoặc hết hạn.
 */
package com.example.loki.data.remote.interceptor

import com.example.loki.data.local.LocalCache
import com.example.loki.data.local.datastore.SessionStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

// Adds "Authorization: Bearer <token>" to every request
class AuthInterceptor @Inject constructor(
    private val sessionStore: SessionStore,
    // Provider: only needed on a 401, and keeps the database out of the network setup
    private val localCache: Provider<LocalCache>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // OkHttp runs this on a background thread, so blocking here is fine
        val token = runBlocking { sessionStore.token.first() }
        val request = if (token == null) chain.request() else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }

        val response = chain.proceed(request)
        // Expired or revoked token: back to Login, without this account's photos and chats left on the phone
        if (response.code == 401 && token != null) {
            runBlocking {
                if (sessionStore.clearIfToken(token)) localCache.get().clear()
            }
        }
        return response
    }
}
