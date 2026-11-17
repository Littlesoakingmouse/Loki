/*
 * TỔNG QUAN FILE: Lưu cài đặt của app trên máy bằng DataStore riêng (file "settings").
 *
 * Thành phần chính:
 * - notificationsEnabled: bật hay tắt hiện thông báo, mặc định bật.
 * - setNotificationsEnabled: đổi cài đặt từ màn Profile.
 *
 * Lưu ý:
 * - Khác SessionStore, cài đặt ở đây vẫn giữ sau khi đăng xuất.
 * - Tắt thông báo chỉ chặn việc hiện thông báo; push vẫn cập nhật widget và các màn hình đang mở.
 */
package com.example.loki.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

// App preferences kept on this phone; unlike the session they survive logging out
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext context: Context) {
    private val dataStore = context.settingsDataStore

    // Off = pushes still refresh the widget and the open screens, they just don't show a notification
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS] ?: true }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS] = enabled }
    }

    private companion object {
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
    }
}
