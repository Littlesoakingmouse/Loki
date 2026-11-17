/*
 * TỔNG QUAN FILE: Service nhận push từ Firebase: hiện thông báo, báo màn hình đang mở và cập nhật widget.
 *
 * Thành phần chính:
 * - onNewToken: Firebase đổi token thì báo cho backend.
 * - onMessageReceived: đọc data message (type, photoId, senderId...), tạo Intent mở đúng màn hình, hiện thông
 *   báo nếu người dùng không tắt, phát sự kiện qua PushEvents, cập nhật widget khi có ảnh mới.
 * - show: tạo notification channel và PendingIntent rồi hiện thông báo.
 * - NotificationKey / cancelChatNotification: mỗi cuộc trò chuyện một thông báo (tin mới thay tin cũ thay vì
 *   chồng thêm), mỗi ảnh một thông báo; mở chat thì gỡ thông báo của người đó.
 *
 * Lưu ý:
 * - Backend gửi data message nên hàm này chạy cả khi app ở nền.
 * - Mỗi thông báo dùng request code riêng cho PendingIntent, nếu không các thông báo sẽ mở nhầm ảnh của nhau.
 * - runBlocking dùng có chủ ý: Service có thể bị huỷ ngay sau khi hàm trả về, launch sẽ bị huỷ giữa chừng.
 * - Từ Android 13 cần quyền POST_NOTIFICATIONS; quyền có thể bị thu hồi bất cứ lúc nào nên notify được bọc
 *   try-catch.
 * - Icon nhỏ phải là ic_notification (một màu), icon launcher có màu sẽ hiện thành khối trắng.
 */
package com.example.loki.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.glance.appwidget.updateAll
import com.example.loki.R
import com.example.loki.data.local.datastore.SettingsStore
import com.example.loki.data.remote.safeCall
import com.example.loki.data.repository.SessionRepository
import com.example.loki.ui.navigation.chatIntent
import com.example.loki.ui.navigation.photoIntent
import com.example.loki.widget.LokiWidget
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val CHANNEL_ID = "loki_updates"

// tag + id of a notification: a new one with the same key replaces the old one
data class NotificationKey(val tag: String, val id: Int) {
    companion object {
        fun chat(otherUserId: Long) = NotificationKey("chat", otherUserId.toInt())
        fun photo(photoId: Long?) = NotificationKey("photo", photoId?.toInt() ?: 0)
        fun friends() = NotificationKey("friends", 0)
    }
}

// The chat with this person is open: its notification has been read
fun cancelChatNotification(context: Context, otherUserId: Long) {
    val key = NotificationKey.chat(otherUserId)
    NotificationManagerCompat.from(context).cancel(key.tag, key.id)
}

@AndroidEntryPoint
class LokiMessagingService : FirebaseMessagingService() {
    @Inject lateinit var session: SessionRepository
    @Inject lateinit var pushEvents: PushEvents
    @Inject lateinit var settings: SettingsStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Firebase rotated this phone's push token: tell the backend (login sends it too)
    override fun onNewToken(token: String) {
        scope.launch { safeCall { session.updateFcmToken(token) } }
    }

    // The backend sends data-only messages, so this runs even when the app is in the background:
    // we build the notification ourselves and refresh the widget right away
    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = PushType.from(data["type"])
        val title = data["title"] ?: message.notification?.title
        val body = data["body"] ?: message.notification?.body
        val photoId = data["photoId"]?.toLongOrNull()
        val senderId = data["senderId"]?.toLongOrNull()

        val (open, key) = when {
            type == PushType.NEW_MESSAGE && senderId != null ->
                chatIntent(this, senderId, title.orEmpty(), data["conversationId"]?.toLongOrNull()) to NotificationKey.chat(senderId)
            type == PushType.FRIEND_REQUEST -> photoIntent(this, null) to NotificationKey.friends()
            else -> photoIntent(this, photoId) to NotificationKey.photo(photoId)
        }
        // The user can mute notifications in Profile; the widget and open screens still update below
        if (runBlocking { settings.notificationsEnabled.first() }) show(title, body, open, key)
        type?.let(pushEvents::emit)
        // Runs on Firebase's background thread; block until done, since the service (and scope) may be
        // destroyed right after this returns, which would cancel a launched update halfway
        if (type == PushType.NEW_PHOTO) runBlocking { safeCall { LokiWidget().updateAll(this@LokiMessagingService) } }
    }

    private fun show(title: String?, body: String?, open: Intent, key: NotificationKey) {
        val notifications = NotificationManagerCompat.from(this)
        if (!notifications.areNotificationsEnabled()) return
        notifications.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Ảnh và tin nhắn", NotificationManager.IMPORTANCE_HIGH),
        )
        // A distinct request code per notification, so each one keeps its own extras
        val contentIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(getColor(R.color.loki_yellow))
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        try {
            notifications.notify(key.tag, key.id, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and now
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
