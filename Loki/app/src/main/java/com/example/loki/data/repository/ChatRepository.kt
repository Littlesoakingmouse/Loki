/*
 * TỔNG QUAN FILE: Repository của chat: hộp thư và tin nhắn, theo kiểu offline-first.
 *
 * Thành phần chính:
 * - observeConversations, observeUnreadTotal, observeMessages: đọc từ Room, UI chỉ lắng nghe các Flow này.
 * - refreshConversations, refreshMessages: gọi API rồi ghi vào Room.
 * - conversationWith: tìm cuộc trò chuyện với một người, xem cache trước rồi mới hỏi backend.
 * - send: gửi tin, lưu tin vào cache và cập nhật dòng hộp thư (hoặc tải lại hộp thư nếu là cuộc trò chuyện
 *   mới).
 * - markRead: báo backend và đặt số chưa đọc trong cache về 0.
 *
 * Lưu ý:
 * - Mọi dữ liệu trả về từ API phải đi qua Room; không đưa thẳng kết quả API lên UI.
 * - send ném IOException khi mất mạng. Màn hình gửi qua OutboxRepository.sendMessage, nơi tự đưa tin vào
 *   hàng đợi và giữ nguyên clientId khi gửi lại.
 */
package com.example.loki.data.repository

import com.example.loki.data.local.db.dao.ChatDao
import com.example.loki.data.local.db.entity.toCached
import com.example.loki.data.local.db.entity.toDto
import com.example.loki.data.remote.api.ChatApi
import com.example.loki.data.remote.dto.ConversationDto
import com.example.loki.data.remote.dto.MessageDto
import com.example.loki.data.remote.dto.SendMessageRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Inbox and messages. Screens read the Room cache; every call to the backend writes into it
@Singleton
class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val chatDao: ChatDao,
) {
    fun observeConversations(): Flow<List<ConversationDto>> =
        chatDao.observeConversations().map { rows -> rows.map { it.toDto() } }

    fun observeUnreadTotal(): Flow<Long> = chatDao.observeUnreadTotal()

    // Newest first
    fun observeMessages(conversationId: Long): Flow<List<MessageDto>> =
        chatDao.observeMessages(conversationId).map { rows -> rows.map { it.toDto() } }

    suspend fun refreshConversations() {
        chatDao.replaceConversations(chatApi.conversations().map { it.toCached() })
    }

    // Returns the newest messages, so the caller can tell whether something new arrived
    suspend fun refreshMessages(conversationId: Long): List<MessageDto> =
        chatApi.messages(conversationId).also { messages -> chatDao.upsertMessages(messages.map { it.toCached() }) }

    // Opened from a friend or a photo reply: the conversation may already exist (cache first, then the backend)
    suspend fun conversationWith(otherUserId: Long): Long? =
        chatDao.conversationWith(otherUserId) ?: run {
            refreshConversations()
            chatDao.conversationWith(otherUserId)
        }

    suspend fun markRead(conversationId: Long) {
        chatApi.markRead(conversationId)
        chatDao.markRead(conversationId)
    }

    // photoId set = a reply to that photo. The backend creates the conversation on the first message
    suspend fun send(recipientId: Long, content: String, photoId: Long? = null, clientId: String? = null): MessageDto {
        val message = chatApi.send(SendMessageRequest(recipientId, content, photoId, clientId))
        chatDao.upsertMessages(listOf(message.toCached()))
        val conversation = chatDao.conversationById(message.conversationId)
        if (conversation != null) {
            chatDao.upsertConversations(
                listOf(conversation.copy(lastMessageContent = message.content, lastMessageAt = message.createdAt)),
            )
        } else {
            refreshConversations() // a brand-new conversation: let the backend describe it
        }
        return message
    }
}
