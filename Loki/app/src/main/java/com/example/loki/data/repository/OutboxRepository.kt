/*
 * TỔNG QUAN FILE: Gửi ảnh và tin nhắn, kèm hàng đợi offline giữ trong Room cho tới khi OutboxWorker gửi được.
 *
 * Thành phần chính:
 * - sendPhoto, sendMessage: gửi ngay; mất mạng thì tự xếp vào hàng đợi. Mỗi lần gửi có một clientId (UUID)
 *   dùng lại khi gửi lại, để backend không tạo bản trùng nếu lần đầu thực ra đã tới nơi.
 * - outboxDir: thư mục filesDir/outbox chứa ảnh chờ gửi.
 * - pendingPhotoCount, observePendingMessagesTo, hasPendingMessagesTo: cho UI hiện trạng thái "đang chờ".
 * - flush: gửi lần lượt theo thứ tự tạo; dùng uploadedUrl để không tải ảnh lên Cloudinary hai lần.
 * - sendOrDrop + isRetryable: lỗi tạm thời (mất mạng, 401, 408, 429, 5xx) thì ném lên để Worker thử lại; lỗi
 *   khác (backend/Cloudinary từ chối, dữ liệu hỏng) thì bỏ mục đó và ghi log.
 *
 * Lưu ý:
 * - Ảnh lưu trong filesDir chứ không phải cacheDir, vì Android có thể tự xoá cacheDir khi máy đầy.
 * - Không bỏ qua lỗi không thử lại được thì một mục hỏng sẽ chặn mãi mọi thứ xếp phía sau.
 * - Tin mới tới một người phải xếp sau tin còn chờ tới người đó, để giữ đúng thứ tự.
 */
package com.example.loki.data.repository

import android.content.Context
import android.util.Log
import com.example.loki.data.local.db.dao.OutboxDao
import com.example.loki.data.local.db.entity.PendingMessage
import com.example.loki.data.local.db.entity.PendingPhoto
import com.example.loki.data.remote.api.PhotoApi
import com.example.loki.data.remote.dto.MessageDto
import com.example.loki.data.remote.dto.SendPhotoRequest
import com.example.loki.data.remote.storage.PhotoStorage
import com.example.loki.data.worker.OutboxWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Outbox"

// Where queued photos wait; cleared with the rest of the account's data in LocalCache
fun outboxDir(context: Context) = File(context.filesDir, "outbox")

// Worth sending again later: no connection, logged out for now, or the server is busy or broken.
// Anything else (400, 403 no longer friends, 404 photo deleted, Cloudinary refusing the file) won't change on a retry
fun isRetryable(e: Throwable): Boolean = when (e) {
    is IOException -> true
    is HttpException -> e.code() == 401 || e.code() == 408 || e.code() == 429 || e.code() >= 500
    else -> false
}

@Singleton
class OutboxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val outboxDao: OutboxDao,
    private val photoApi: PhotoApi,
    private val photoStorage: PhotoStorage,
    private val chat: ChatRepository,
) {
    val pendingPhotoCount: Flow<Int> = outboxDao.observePhotoCount()

    fun observePendingMessagesTo(userId: Long): Flow<List<PendingMessage>> = outboxDao.observeMessagesTo(userId)

    // A new message must wait behind the queued ones, or it would arrive before them
    suspend fun hasPendingMessagesTo(userId: Long) = outboxDao.hasMessagesTo(userId)

    // Upload -> tell the backend. Returns true when it was queued for later because the connection dropped
    suspend fun sendPhoto(jpeg: ByteArray, caption: String?, recipientIds: List<Long>): Boolean {
        val clientId = newClientId()
        val url = try {
            photoStorage.upload(jpeg)
        } catch (e: IOException) {
            queuePhoto(jpeg, caption, recipientIds, clientId, uploadedUrl = null)
            return true
        }
        try {
            photoApi.send(SendPhotoRequest(url, caption, recipientIds, clientId))
        } catch (e: IOException) {
            // Cloudinary has it already; the retry reuses clientId, so a send that did arrive isn't repeated
            queuePhoto(jpeg, caption, recipientIds, clientId, uploadedUrl = url)
            return true
        }
        return false
    }

    // Returns the sent message, or null when it was queued (offline, or earlier messages to this person still wait)
    suspend fun sendMessage(recipientId: Long, content: String, photoId: Long? = null): MessageDto? {
        val clientId = newClientId()
        if (outboxDao.hasMessagesTo(recipientId)) {
            queueMessage(recipientId, content, photoId, clientId)
            return null
        }
        return try {
            chat.send(recipientId, content, photoId, clientId)
        } catch (e: IOException) {
            queueMessage(recipientId, content, photoId, clientId)
            null
        }
    }

    private suspend fun queuePhoto(jpeg: ByteArray, caption: String?, recipientIds: List<Long>, clientId: String, uploadedUrl: String?) {
        val file = withContext(Dispatchers.IO) {
            File(outboxDir(context).apply { mkdirs() }, "${UUID.randomUUID()}.jpg").apply { writeBytes(jpeg) }
        }
        outboxDao.insertPhoto(
            PendingPhoto(
                filePath = file.path,
                caption = caption,
                recipientIds = recipientIds,
                uploadedUrl = uploadedUrl,
                createdAt = now(),
                clientId = clientId,
            ),
        )
        OutboxWorker.schedule(context)
    }

    private suspend fun queueMessage(recipientId: Long, content: String, photoId: Long?, clientId: String) {
        outboxDao.insertMessage(
            PendingMessage(recipientId = recipientId, content = content, photoId = photoId, createdAt = now(), clientId = clientId),
        )
        OutboxWorker.schedule(context)
    }

    // Run by OutboxWorker. Throws on a retryable error so the worker tries again; the rest stays queued
    suspend fun flush() {
        for (photo in outboxDao.photos()) {
            sendOrDrop("photo ${photo.id}") {
                val file = File(photo.filePath)
                if (file.exists()) {
                    val url = photo.uploadedUrl
                        ?: photoStorage.upload(withContext(Dispatchers.IO) { file.readBytes() })
                            .also { outboxDao.setUploadedUrl(photo.id, it) }
                    photoApi.send(SendPhotoRequest(url, photo.caption, photo.recipientIds, photo.clientId.ifEmpty { null }))
                }
            }
            outboxDao.deletePhoto(photo.id)
            withContext(Dispatchers.IO) { File(photo.filePath).delete() }
        }
        for (message in outboxDao.messages()) {
            sendOrDrop("message ${message.id}") {
                chat.send(message.recipientId, message.content, message.photoId, message.clientId.ifEmpty { null })
            }
            outboxDao.deleteMessage(message.id)
        }
    }

    private suspend fun sendOrDrop(what: String, send: suspend () -> Unit) {
        try {
            send()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (isRetryable(e)) throw e
            Log.w(TAG, "Dropped $what: it was refused and a retry won't change that", e)
        }
    }

    private fun newClientId() = UUID.randomUUID().toString()

    private fun now() = OffsetDateTime.now().toString()
}
