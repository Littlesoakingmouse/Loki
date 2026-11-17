/*
 * TỔNG QUAN FILE: Room DAO cho cache hộp thư và tin nhắn.
 *
 * Thành phần chính:
 * - observeConversations, observeUnreadTotal, observeMessages: Flow mà UI lắng nghe.
 * - replaceConversations (@Transaction): thay toàn bộ hộp thư bằng danh sách mới từ backend.
 * - conversationWith: tìm cuộc trò chuyện với một người; markRead: đặt số chưa đọc về 0.
 * - upsertConversations, upsertMessages: ghi đè theo id của server.
 *
 * Lưu ý:
 * - replaceConversations giả định backend trả về cả hộp thư (tối đa 50). Nếu sau này có phân trang thì không
 *   được xoá hết.
 * - Tin nhắn chỉ được thêm hoặc cập nhật, không bao giờ bị xoá khỏi cache trừ khi đăng xuất.
 */
package com.example.loki.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.loki.data.local.db.entity.CachedConversation
import com.example.loki.data.local.db.entity.CachedMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC, id DESC")
    fun observeConversations(): Flow<List<CachedConversation>>

    @Query("SELECT COALESCE(SUM(unreadCount), 0) FROM conversations")
    fun observeUnreadTotal(): Flow<Long>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun conversationById(id: Long): CachedConversation?

    @Query("SELECT id FROM conversations WHERE otherUserId = :otherUserId LIMIT 1")
    suspend fun conversationWith(otherUserId: Long): Long?

    @Query("DELETE FROM conversations")
    suspend fun deleteConversations()

    @Upsert
    suspend fun upsertConversations(conversations: List<CachedConversation>)

    // The backend returns the whole inbox (up to 50), so it replaces the cached one
    @Transaction
    suspend fun replaceConversations(conversations: List<CachedConversation>) {
        deleteConversations()
        upsertConversations(conversations)
    }

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markRead(conversationId: Long)

    // Newest first, like the API
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC, id DESC")
    fun observeMessages(conversationId: Long): Flow<List<CachedMessage>>

    @Upsert
    suspend fun upsertMessages(messages: List<CachedMessage>)
}
