/*
 * TỔNG QUAN FILE: Các bảng Room làm cache dữ liệu backend, và hàm chuyển đổi giữa entity và DTO.
 *
 * Thành phần chính:
 * - CachedPhoto (photos): có thêm cột senderId để lọc bằng SQL; index theo senderId và createdAt.
 * - CachedConversation (conversations), CachedMessage (messages), CachedUser (users).
 * - toCached() / toDto(): hàm mở rộng chuyển qua lại giữa DTO và entity.
 *
 * Lưu ý:
 * - Khoá chính là id của server, nên tải lại cùng một dòng sẽ ghi đè chứ không tạo bản trùng.
 * - createdAt lưu dạng chuỗi ISO; so sánh chuỗi cho ra đúng thứ tự thời gian.
 * - Thêm hoặc bớt cột phải tăng version trong LokiDatabase.
 */
package com.example.loki.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.loki.data.remote.dto.ConversationDto
import com.example.loki.data.remote.dto.MessageDto
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.data.remote.dto.ReactionDto
import com.example.loki.data.remote.dto.UserDto

// Local copies of what the backend returned, so screens can show the last known state instantly and offline.
// Nested objects (sender, reactions, a replied photo) are stored as JSON: they are only ever read whole

@Entity(tableName = "photos", indices = [Index("senderId"), Index("createdAt")])
data class CachedPhoto(
    @PrimaryKey val id: Long,
    val senderId: Long, // own column so the history filter runs as a query
    val sender: UserDto,
    val imageUrl: String,
    val caption: String?,
    val createdAt: String?, // ISO local time from the backend; sorts correctly as text
    val seenAt: String?,
    val reactions: List<ReactionDto>,
)

@Entity(tableName = "conversations")
data class CachedConversation(
    @PrimaryKey val id: Long,
    val otherUserId: Long,
    val otherUser: UserDto,
    val lastMessageContent: String?,
    val lastMessageAt: String?,
    val unreadCount: Long,
)

@Entity(tableName = "messages", indices = [Index("conversationId")])
data class CachedMessage(
    @PrimaryKey val id: Long,
    val conversationId: Long,
    val sender: UserDto,
    val content: String?,
    val photo: PhotoDto?, // the photo a reply points at, kept so its thumbnail shows offline
    val createdAt: String?,
)

// `isMe` = my own profile row; the others are my friends, in the order the backend listed them
@Entity(tableName = "users")
data class CachedUser(
    @PrimaryKey val id: Long,
    val user: UserDto,
    val isMe: Boolean,
    val position: Int,
)

fun PhotoDto.toCached() = CachedPhoto(id, sender.id, sender, imageUrl, caption, createdAt, seenAt, reactions)
fun CachedPhoto.toDto() = PhotoDto(id, sender, imageUrl, caption, createdAt, seenAt, reactions)

fun ConversationDto.toCached() = CachedConversation(id, otherUser.id, otherUser, lastMessageContent, lastMessageAt, unreadCount)
fun CachedConversation.toDto() = ConversationDto(id, otherUser, lastMessageContent, lastMessageAt, unreadCount)

fun MessageDto.toCached() = CachedMessage(id, conversationId, sender, content, photo, createdAt)
fun CachedMessage.toDto() = MessageDto(id, conversationId, sender, content, photo, createdAt)
