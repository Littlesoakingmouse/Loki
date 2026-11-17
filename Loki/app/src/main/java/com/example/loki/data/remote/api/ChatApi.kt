/*
 * TỔNG QUAN FILE: Retrofit interface cho hộp thư và tin nhắn (api/conversations/...).
 *
 * Thành phần chính:
 * - MAX_PAGE_SIZE = 50: giới hạn kích thước trang mà backend cho phép.
 * - conversations, messages: đọc danh sách cuộc trò chuyện và tin nhắn (mới nhất trước).
 * - send: gửi tin; backend tự tạo cuộc trò chuyện ở tin đầu tiên. markRead: đánh dấu đã đọc.
 *
 * Lưu ý:
 * - Chat chỉ lấy 50 tin gần nhất và được làm mới bằng polling 3 giây (ChatViewModel); chưa có phân trang hay
 *   WebSocket.
 */
package com.example.loki.data.remote.api

import com.example.loki.data.remote.dto.ConversationDto
import com.example.loki.data.remote.dto.MessageDto
import com.example.loki.data.remote.dto.SendMessageRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// The backend caps page size at 50
const val MAX_PAGE_SIZE = 50

interface ChatApi {
    @GET("api/conversations")
    suspend fun conversations(@Query("size") size: Int = MAX_PAGE_SIZE): List<ConversationDto>

    // Creates the conversation on the first message
    @POST("api/conversations/messages")
    suspend fun send(@Body request: SendMessageRequest): MessageDto

    // Newest first
    @GET("api/conversations/{conversationId}/messages")
    suspend fun messages(
        @Path("conversationId") conversationId: Long,
        @Query("size") size: Int = MAX_PAGE_SIZE,
    ): List<MessageDto>

    @PUT("api/conversations/{conversationId}/read")
    suspend fun markRead(@Path("conversationId") conversationId: Long)
}
