/*
 * TỔNG QUAN FILE: Room DAO cho hàng đợi gửi offline (outbox).
 *
 * Thành phần chính:
 * - insertPhoto, insertMessage: thêm mục vào hàng đợi.
 * - photos(), messages(): đọc theo thứ tự tạo (cũ trước) để gửi đúng thứ tự.
 * - observePhotoCount, observeMessagesTo: Flow cho nhãn "đang chờ" trên camera và trong chat.
 * - hasMessagesTo: tin mới phải xếp sau tin đang chờ cho cùng người nhận.
 * - setUploadedUrl: ghi nhớ ảnh đã lên Cloudinary để thử lại không tải lên lần nữa.
 *
 * Lưu ý:
 * - Dữ liệu ở đây là bản duy nhất, mất là mất hẳn. Cẩn thận khi xoá hoặc đổi schema.
 */
package com.example.loki.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.loki.data.local.db.entity.PendingMessage
import com.example.loki.data.local.db.entity.PendingPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Insert
    suspend fun insertPhoto(photo: PendingPhoto)

    @Insert
    suspend fun insertMessage(message: PendingMessage)

    // Oldest first: they are sent in the order they were made
    @Query("SELECT * FROM pending_photos ORDER BY id")
    suspend fun photos(): List<PendingPhoto>

    @Query("SELECT * FROM pending_messages ORDER BY id")
    suspend fun messages(): List<PendingMessage>

    @Query("SELECT COUNT(*) FROM pending_photos")
    fun observePhotoCount(): Flow<Int>

    // Newest first, like the chat
    @Query("SELECT * FROM pending_messages WHERE recipientId = :userId ORDER BY id DESC")
    fun observeMessagesTo(userId: Long): Flow<List<PendingMessage>>

    @Query("SELECT EXISTS(SELECT 1 FROM pending_messages WHERE recipientId = :userId)")
    suspend fun hasMessagesTo(userId: Long): Boolean

    @Query("UPDATE pending_photos SET uploadedUrl = :url WHERE id = :id")
    suspend fun setUploadedUrl(id: Long, url: String)

    @Query("DELETE FROM pending_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("DELETE FROM pending_messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)
}
