/*
 * TỔNG QUAN FILE: Khai báo database Room của app: danh sách bảng, phiên bản, DAO và Migration.
 *
 * Thành phần chính:
 * - Bảng cache: photos, conversations, messages, users (bản sao dữ liệu backend).
 * - Bảng outbox: pending_photos, pending_messages (bản duy nhất của thứ gửi lúc offline).
 * - CacheConverters: lưu object lồng nhau dạng JSON.
 * - MIGRATION_2_3: thêm clientId cho outbox (giữ nguyên ảnh/tin chưa gửi) và xoá cache, vì từ bản này backend
 *   trả thời gian có múi giờ; cache tự tải lại.
 *
 * Lưu ý:
 * - Đổi cấu trúc entity thì phải tăng version và viết Migration mới; schema mỗi version được xuất ra
 *   app/schemas (cần commit) để so sánh.
 * - DatabaseModule vẫn bật fallbackToDestructiveMigration cho các bản cũ hơn version 2 (chưa có người dùng
 *   thật); từ version 2 trở đi luôn có Migration để không mất outbox.
 */
package com.example.loki.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.loki.data.local.db.dao.ChatDao
import com.example.loki.data.local.db.dao.OutboxDao
import com.example.loki.data.local.db.dao.PhotoDao
import com.example.loki.data.local.db.dao.UserDao
import com.example.loki.data.local.db.entity.CachedConversation
import com.example.loki.data.local.db.entity.CachedMessage
import com.example.loki.data.local.db.entity.CachedPhoto
import com.example.loki.data.local.db.entity.CachedUser
import com.example.loki.data.local.db.entity.PendingMessage
import com.example.loki.data.local.db.entity.PendingPhoto

@Database(
    entities = [CachedPhoto::class, CachedConversation::class, CachedMessage::class, CachedUser::class, PendingPhoto::class, PendingMessage::class],
    version = 3,
    exportSchema = true,
)
@TypeConverters(CacheConverters::class)
abstract class LokiDatabase : RoomDatabase() {
    abstract fun photoDao(): PhotoDao
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao
    abstract fun outboxDao(): OutboxDao
}

// v3: outbox rows get a clientId (unsent items are kept); the cache is emptied because timestamps now carry a zone
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (table in listOf("pending_photos", "pending_messages")) {
            db.execSQL("ALTER TABLE $table ADD COLUMN clientId TEXT NOT NULL DEFAULT ''")
            db.execSQL("UPDATE $table SET clientId = lower(hex(randomblob(16)))")
        }
        for (table in listOf("photos", "conversations", "messages", "users")) {
            db.execSQL("DELETE FROM $table")
        }
    }
}
