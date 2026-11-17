/*
 * TỔNG QUAN FILE: DTO cho ảnh và cảm xúc.
 *
 * Thành phần chính:
 * - SendPhotoRequest: URL ảnh, caption và danh sách người nhận.
 * - AddReactionRequest: thả hoặc đổi emoji trên một ảnh.
 * - PhotoDto: một ảnh trong lịch sử; seenAt chỉ có ý nghĩa với ảnh người khác gửi cho mình.
 * - ReactionDto: một cảm xúc của một người.
 * - PhotoActivityDto: với ảnh của mình, từng người nhận đã xem lúc nào và thả emoji gì.
 *
 * Lưu ý:
 * - PhotoDto là kiểu mà UI dùng; Room lưu nó dưới dạng CachedPhoto (xem CacheEntities).
 */
package com.example.loki.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendPhotoRequest(
    val imageUrl: String,
    val caption: String? = null,
    val recipientIds: List<Long>,
    // Same id on a retry = the backend returns the stored photo instead of a duplicate
    val clientId: String? = null,
)

@Serializable
data class AddReactionRequest(val photoId: Long, val emoji: String)

@Serializable
data class PhotoDto(
    val id: Long,
    val sender: UserDto,
    val imageUrl: String,
    val caption: String? = null,
    val createdAt: String? = null,
    val seenAt: String? = null,
    val reactions: List<ReactionDto> = emptyList(),
)

// One recipient of my photo: seen when (null = not yet) and which emoji (null = none)
@Serializable
data class PhotoActivityDto(
    val user: UserDto,
    val seenAt: String? = null,
    val emoji: String? = null,
)

@Serializable
data class ReactionDto(
    val id: Long,
    val user: UserDto,
    val emoji: String,
)
