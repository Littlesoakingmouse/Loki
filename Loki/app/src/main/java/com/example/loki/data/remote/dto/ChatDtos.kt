/*
 * TỔNG QUAN FILE: DTO cho chat.
 *
 * Thành phần chính:
 * - SendMessageRequest: gửi tin; photoId khác null nghĩa là trả lời một ảnh (backend lưu thành PHOTO_REPLY).
 * - ConversationDto: một dòng trong hộp thư, kèm số tin chưa đọc.
 * - MessageDto: một tin nhắn, có thể đính kèm PhotoDto của ảnh được trả lời.
 *
 * Lưu ý:
 * - Thời gian (createdAt, lastMessageAt) là chuỗi ISO có múi giờ, VD "2026-11-18T10:00:00+07:00". Mọi
 *   thời gian từ cùng một server có cùng múi giờ nên so sánh dạng chuỗi vẫn đúng thứ tự.
 * - clientId: UUID của lần gửi; gửi lại cùng clientId thì backend trả lại tin đã lưu thay vì tạo tin trùng.
 */
package com.example.loki.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val recipientId: Long,
    val content: String,
    // Set to reply to one of the photos; the backend then stores it as a PHOTO_REPLY
    val photoId: Long? = null,
    // Same id on a retry = the backend returns the stored message instead of a duplicate
    val clientId: String? = null,
)

@Serializable
data class ConversationDto(
    val id: Long,
    val otherUser: UserDto,
    val lastMessageContent: String? = null,
    val lastMessageAt: String? = null,
    val unreadCount: Long = 0,
)

@Serializable
data class MessageDto(
    val id: Long,
    val conversationId: Long,
    val sender: UserDto,
    val content: String? = null,
    val photo: PhotoDto? = null,
    val createdAt: String? = null,
)
