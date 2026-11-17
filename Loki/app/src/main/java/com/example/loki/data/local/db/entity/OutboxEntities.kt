/*
 * TỔNG QUAN FILE: Các bảng Room của outbox: ảnh và tin nhắn tạo lúc offline, chờ OutboxWorker gửi.
 *
 * Thành phần chính:
 * - PendingPhoto: đường dẫn file JPEG, caption, người nhận và uploadedUrl (nếu đã lên Cloudinary).
 * - PendingMessage: người nhận, nội dung, photoId nếu là trả lời ảnh.
 * - clientId: UUID tạo lúc người dùng bấm gửi. Gửi lại cùng clientId thì backend trả lại bản đã lưu, nên mất
 *   mạng giữa chừng không tạo ảnh/tin trùng.
 *
 * Lưu ý:
 * - Khác bảng cache, đây là bản duy nhất của dữ liệu người dùng vừa tạo.
 * - id tự tăng; giá trị mặc định 0 nghĩa là để Room tự cấp.
 * - Ảnh lưu thành file trong filesDir/outbox, database chỉ giữ đường dẫn.
 * - clientId có defaultValue '' để khớp cột mà MIGRATION_2_3 thêm vào (xem LokiDatabase).
 */
package com.example.loki.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Sends made without a connection, waiting for OutboxWorker. Unlike the cache tables these are the only copy

@Entity(tableName = "pending_photos")
data class PendingPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String, // the compressed JPEG in filesDir/outbox
    val caption: String?,
    val recipientIds: List<Long>,
    // Set once Cloudinary has the image, so a retry after a backend failure doesn't upload it twice
    val uploadedUrl: String? = null,
    val createdAt: String,
    @ColumnInfo(defaultValue = "''") val clientId: String,
)

@Entity(tableName = "pending_messages", indices = [Index("recipientId")])
data class PendingMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientId: Long,
    val content: String,
    val photoId: Long? = null, // a reply to that photo
    val createdAt: String,
    @ColumnInfo(defaultValue = "''") val clientId: String,
)
