/*
 * TỔNG QUAN FILE: Repository của lịch sử ảnh, theo kiểu offline-first: UI đọc Room, mọi lần gọi backend đều
 * ghi vào Room.
 *
 * Thành phần chính:
 * - MAX_CACHED_PHOTOS = 300: số ảnh tối đa giữ trong cache.
 * - observeHistory: Flow lịch sử từ Room, lọc theo người gửi nếu có.
 * - loadPage: tải một trang, tính ảnh đã bị xoá bằng staleIds rồi sync vào Room; chỉ trả về số ảnh của trang.
 * - markSeen, toggleReaction, delete: gọi API rồi cập nhật dòng tương ứng trong cache.
 * - latestFromOthers: ảnh mới nhất cho widget, lấy từ server và dự phòng bằng cache.
 * - activity: với ảnh của mình, ai đã xem và thả emoji gì (không cache, luôn hỏi backend).
 *
 * Lưu ý:
 * - loadPage cố ý không trả danh sách ảnh, để mọi dữ liệu buộc phải đi qua Room.
 * - toggleReaction: bấm lại emoji đang chọn là gỡ, bấm emoji khác là thay.
 * - Xoá ảnh trên backend không xoá file trên Cloudinary.
 */
package com.example.loki.data.repository

import com.example.loki.data.local.db.dao.PhotoDao
import com.example.loki.data.local.db.entity.toCached
import com.example.loki.data.local.db.entity.toDto
import com.example.loki.data.remote.api.PAGE_SIZE
import com.example.loki.data.remote.api.PhotoApi
import com.example.loki.data.remote.dto.AddReactionRequest
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.data.remote.safeCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.example.loki.data.remote.dto.PhotoActivityDto
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

// Enough for a few weeks of photos; older ones are fetched again when scrolled to
private const val MAX_CACHED_PHOTOS = 300

// The photo history. Screens read the Room cache; every call to the backend writes into it
@Singleton
class PhotoRepository @Inject constructor(
    private val photoApi: PhotoApi,
    private val photoDao: PhotoDao,
) {
    // senderId null = everyone; my own id = only mine; a friend's id = what that friend sent me
    fun observeHistory(senderId: Long?): Flow<List<PhotoDto>> =
        (if (senderId == null) photoDao.observeAll() else photoDao.observeBySender(senderId))
            .map { rows -> rows.map { it.toDto() } }

    // Fetches one page of the history into the cache; returns how many photos it had
    suspend fun loadPage(page: Int, senderId: Long?): Int {
        val photos = photoApi.history(page = page, friendId = senderId)
        val stale = staleIds(
            cached = photoDao.inScope(senderId).map { CachedRef(it.id, it.createdAt) },
            page = photos.map { CachedRef(it.id, it.createdAt) },
            isFirstPage = page == 0,
            isLastPage = photos.size < PAGE_SIZE,
        )
        photoDao.sync(upserts = photos.map { it.toCached() }, deletes = stale, keep = MAX_CACHED_PHOTOS)
        return photos.size
    }

    suspend fun markSeen(photo: PhotoDto) {
        photoApi.markSeen(photo.id)
        update(photo.id) { it.copy(seenAt = OffsetDateTime.now().toString()) }
    }

    // Tapping my current emoji removes it; any other emoji replaces it
    suspend fun toggleReaction(photo: PhotoDto, emoji: String, myId: Long) {
        val mine = photo.reactions.find { it.user.id == myId }
        val reactions = if (mine?.emoji == emoji) {
            photoApi.removeReaction(photo.id)
            photo.reactions - mine
        } else {
            photo.reactions.filter { it.user.id != myId } + photoApi.react(AddReactionRequest(photo.id, emoji))
        }
        update(photo.id) { it.copy(reactions = reactions) }
    }

    suspend fun activity(photoId: Long): List<PhotoActivityDto> = photoApi.activity(photoId)

    suspend fun delete(photoId: Long) {
        photoApi.delete(photoId)
        photoDao.delete(listOf(photoId))
    }

    // For the widget: the backend's newest photo when reachable (cached on the way), else the cached one
    suspend fun latestFromOthers(myId: Long): PhotoDto? =
        safeCall { photoApi.latest().also { photoDao.upsert(listOf(it.toCached())) } }.getOrNull()
            ?: photoDao.latestFromOthers(myId)?.toDto()

    private suspend fun update(id: Long, change: (PhotoDto) -> PhotoDto) {
        val row = photoDao.byId(id) ?: return
        photoDao.upsert(listOf(change(row.toDto()).toCached()))
    }
}
