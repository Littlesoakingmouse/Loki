/*
 * TỔNG QUAN FILE: DTO cho hồ sơ và lời mời kết bạn.
 *
 * Thành phần chính:
 * - UpdateProfileRequest: chỉ điền trường cần đổi (tên, avatar hoặc fcmToken).
 * - SendFriendRequest, RespondFriendRequest: gửi và trả lời lời mời.
 * - FriendshipStatus: khớp enum Status của backend; giá trị lạ được đọc thành PENDING.
 * - FriendshipDto: một lời mời hoặc quan hệ bạn bè giữa requester và addressee.
 * - ChangePasswordRequest, LogoutRequest (fcmToken của máy này), ReportRequest (báo cáo người dùng / ảnh).
 * - MAX_FRIENDS: giới hạn bạn bè, giống AppConstants.MAX_FRIENDS của backend.
 *
 * Lưu ý:
 * - Đổi MAX_FRIENDS thì đổi cả backend; backend mới là nơi chặn thật, app chỉ hiện số.
 */
package com.example.loki.data.remote.dto

import kotlinx.serialization.Serializable

// Blank fields are ignored by the backend, so send only what changes
@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val fcmToken: String? = null,
)

@Serializable
data class SendFriendRequest(val addresseeId: Long)

// Same as the backend's AppConstants.MAX_FRIENDS, which is what actually enforces it
const val MAX_FRIENDS = 20

@Serializable
enum class FriendshipStatus { PENDING, ACCEPTED, BLOCKED }

@Serializable
data class RespondFriendRequest(val friendshipId: Long, val status: FriendshipStatus)

@Serializable
data class FriendshipDto(
    val id: Long,
    val requester: UserDto,
    val addressee: UserDto,
    val status: FriendshipStatus = FriendshipStatus.PENDING,
)

@Serializable
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)

// This phone's push token, so only this phone stops getting notifications
@Serializable
data class LogoutRequest(val fcmToken: String? = null)

@Serializable
data class ReportRequest(val userId: Long, val photoId: Long? = null, val reason: String? = null)
