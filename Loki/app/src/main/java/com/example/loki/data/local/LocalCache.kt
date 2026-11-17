/*
 * TỔNG QUAN FILE: Xoá mọi dữ liệu của tài khoản trên máy (trừ token và cài đặt).
 *
 * Thành phần chính:
 * - clear(): huỷ OutboxWorker, xoá mọi bảng Room, xoá thư mục outbox và bộ nhớ đệm ảnh của Coil.
 *
 * Lưu ý:
 * - Được gọi khi đăng xuất, xoá tài khoản, khi token hết hạn (AuthInterceptor nhận 401) và trước khi đăng nhập
 *   tài khoản mới, để không lộ dữ liệu giữa hai tài khoản.
 * - Outbox cũng bị xoá: tin và ảnh chưa gửi không được gửi đi dưới tên tài khoản sau.
 * - Thêm nơi lưu dữ liệu mới (bảng, thư mục, DataStore) thì nhớ xoá nó ở đây.
 */
package com.example.loki.data.local

import android.content.Context
import coil3.SingletonImageLoader
import com.example.loki.data.local.db.LokiDatabase
import com.example.loki.data.repository.outboxDir
import com.example.loki.data.worker.OutboxWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Everything this phone keeps about the logged-in account besides the token: the Room cache and downloaded images
@Singleton
class LocalCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: LokiDatabase,
) {
    // On logout, account deletion and before a new login, so one account never sees another's photos or chats
    suspend fun clear() = withContext(Dispatchers.IO) {
        // Unsent photos and messages too: they must never go out under the next account
        OutboxWorker.cancel(context)
        database.clearAllTables()
        outboxDir(context).deleteRecursively()
        val images = SingletonImageLoader.get(context)
        images.memoryCache?.clear()
        images.diskCache?.clear()
    }
}
