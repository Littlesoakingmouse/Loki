/*
 * TỔNG QUAN FILE: Lưu JWT và id người dùng bằng DataStore, giữ lại qua các lần mở app.
 *
 * Thành phần chính:
 * - token, userId: Flow đọc từ DataStore; token là nguồn của trạng thái đăng nhập.
 * - save(auth): ghi token và userId trong cùng một giao dịch. clear(): xoá cả hai (đăng xuất).
 * - replaceToken / clearIfToken: chỉ ghi hoặc xoá khi token hiện tại vẫn là token mà người gọi đã dùng. Tránh
 *   race: request cũ trả 401 sau khi đã đăng nhập lại, hoặc refresh xong sau khi người dùng đã đăng xuất.
 *
 * Lưu ý:
 * - Mỗi file DataStore chỉ được có một instance, vì vậy lớp này là @Singleton và dùng property delegate cấp
 *   file.
 * - Token lưu dạng văn bản thường trong thư mục riêng của app và bị loại khỏi bản sao lưu (backup_rules,
 *   data_extraction_rules). Với dữ liệu nhạy cảm hơn nên mã hoá bằng Android Keystore.
 * - Mọi chỗ cần biết đã đăng nhập hay chưa đều phải đọc từ đây (qua SessionRepository), không tự lưu thêm cờ
 *   riêng.
 */
package com.example.loki.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.loki.data.remote.dto.AuthResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "session")

// The logged-in user's JWT and id, kept across app restarts
@Singleton
class SessionStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.sessionDataStore

    val token: Flow<String?> = dataStore.data.map { it[TOKEN] }
    val userId: Flow<Long?> = dataStore.data.map { it[USER_ID] }

    suspend fun save(auth: AuthResponse) {
        dataStore.edit {
            it[TOKEN] = auth.accessToken
            it[USER_ID] = auth.user.id
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    // A refreshed token, unless the session changed meanwhile (logged out, or another account logged in)
    suspend fun replaceToken(expected: String, auth: AuthResponse): Boolean {
        var replaced = false
        dataStore.edit {
            if (it[TOKEN] == expected) {
                it[TOKEN] = auth.accessToken
                it[USER_ID] = auth.user.id
                replaced = true
            }
        }
        return replaced
    }

    // A 401 only ends the session whose token got it, never a newer one
    suspend fun clearIfToken(expected: String): Boolean {
        var cleared = false
        dataStore.edit {
            if (it[TOKEN] == expected) {
                it.clear()
                cleared = true
            }
        }
        return cleared
    }

    private companion object {
        val TOKEN = stringPreferencesKey("access_token")
        val USER_ID = longPreferencesKey("user_id")
    }
}
