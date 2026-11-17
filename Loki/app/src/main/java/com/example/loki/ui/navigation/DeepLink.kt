/*
 * TỔNG QUAN FILE: Mô tả nơi mà thông báo, widget hoặc link mời muốn mở, và cách đọc chúng từ Intent.
 *
 * Thành phần chính:
 * - DeepLink (sealed interface): Photo, Chat, AddFriend.
 * - photoIntent, chatIntent: tạo Intent mở MainActivity kèm extras.
 * - inviteLink: tạo link loki://add/<id>.
 * - Intent.toDeepLink(): đọc link mời từ data URI hoặc đích đến từ extras.
 *
 * Lưu ý:
 * - Intent không lưu được Long?, nên dùng -1 làm "không có" rồi đổi lại thành null bằng takeIf.
 * - Dữ liệu đến từ bên ngoài app, luôn parse an toàn (toLongOrNull) thay vì để crash.
 * - Shortcut: scheme loki:// không bấm được trong mọi ứng dụng chat; nên chuyển sang App Link https.
 */
package com.example.loki.ui.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.loki.MainActivity

// Where a notification, the widget or an invite link wants the app to go
sealed interface DeepLink {
    // photoId null = just open the history at the newest photo
    data class Photo(val photoId: Long?) : DeepLink
    data class Chat(val otherUserId: Long, val otherUserName: String, val conversationId: Long?) : DeepLink
    data class AddFriend(val userId: Long) : DeepLink
}

private const val EXTRA_OPEN = "open"
private const val EXTRA_PHOTO_ID = "photoId"
private const val EXTRA_USER_ID = "userId"
private const val EXTRA_USER_NAME = "userName"
private const val EXTRA_CONVERSATION_ID = "conversationId"

fun photoIntent(context: Context, photoId: Long?): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_OPEN, "photo")
        photoId?.let { putExtra(EXTRA_PHOTO_ID, it) }
    }

fun chatIntent(context: Context, otherUserId: Long, otherUserName: String, conversationId: Long?): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra(EXTRA_OPEN, "chat")
        putExtra(EXTRA_USER_ID, otherUserId)
        putExtra(EXTRA_USER_NAME, otherUserName)
        conversationId?.let { putExtra(EXTRA_CONVERSATION_ID, it) }
    }

// shortcut: a custom scheme isn't clickable in every chat app; switch to an https App Link once there is a domain
fun inviteLink(myId: Long): String = "loki://add/$myId"

fun Intent.toDeepLink(): DeepLink? {
    val uri: Uri? = data
    if (uri?.scheme == "loki" && uri.host == "add") {
        return uri.lastPathSegment?.toLongOrNull()?.let { DeepLink.AddFriend(it) }
    }
    return when (getStringExtra(EXTRA_OPEN)) {
        "photo" -> DeepLink.Photo(getLongExtra(EXTRA_PHOTO_ID, -1).takeIf { it > 0 })
        "chat" -> {
            val userId = getLongExtra(EXTRA_USER_ID, -1)
            if (userId <= 0) null
            else DeepLink.Chat(
                otherUserId = userId,
                otherUserName = getStringExtra(EXTRA_USER_NAME).orEmpty(),
                conversationId = getLongExtra(EXTRA_CONVERSATION_ID, -1).takeIf { it > 0 },
            )
        }
        else -> null
    }
}
