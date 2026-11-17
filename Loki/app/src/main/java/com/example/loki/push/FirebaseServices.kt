/*
 * TỔNG QUAN FILE: Bọc Firebase Cloud Messaging để lấy FCM token một cách an toàn.
 *
 * Thành phần chính:
 * - isConfigured: false khi chưa có app/google-services.json.
 * - fcmToken(): token để backend gửi push tới máy này; trả null khi không lấy được.
 *
 * Lưu ý:
 * - Push là tính năng phụ: thiếu Firebase hay Google Play services thì đăng nhập vẫn chạy, chỉ không có thông
 *   báo.
 */
package com.example.loki.push

import android.content.Context
import com.example.loki.data.remote.safeCall
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseServices @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    // False until app/google-services.json is added and the app is rebuilt
    val isConfigured get() = FirebaseApp.getApps(context).isNotEmpty()

    // Lets the backend send this phone push notifications; null when unavailable
    suspend fun fcmToken(): String? {
        if (!isConfigured) return null
        // No Google Play services, offline...: login still works, just without push
        return safeCall { FirebaseMessaging.getInstance().token.await() }.getOrNull()
    }
}
