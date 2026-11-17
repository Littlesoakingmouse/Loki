/*
 * TỔNG QUAN FILE: ViewModel lịch sử ảnh của Home: danh sách ảnh, bộ lọc, phân trang, đã xem, cảm xúc, trả lời,
 * xoá, hoạt động trên ảnh của mình, lưu và chia sẻ ảnh.
 *
 * Thành phần chính:
 * - FeedUiState: ảnh, myId, bộ lọc, đang tải, offline, thông báo snackbar, danh sách hoạt động đang mở,
 *   Intent chia sẻ chờ màn hình mở.
 * - init: lắng nghe userId, lịch sử từ Room theo bộ lọc (flatMapLatest) và sự kiện push.
 * - refresh, checkForNew, loadMore: tải trang đầu hoặc trang tiếp, quản lý bằng loadJob.
 * - setFilter, markSeen, toggleReaction, reply, delete, messageShown.
 * - showActivity / closeActivity: ai đã xem, thả emoji gì trên ảnh của mình.
 * - saveToGallery, share / shareHandled: qua PhotoExport.
 *
 * Lưu ý:
 * - refresh huỷ việc tải cũ (đổi bộ lọc không bị trang cũ ghi đè); checkForNew và loadMore bỏ qua nếu đang
 *   tải.
 * - Mất mạng chỉ bật nhãn offline, không hiện snackbar mỗi lần làm mới thất bại.
 * - Trả lời ảnh đi qua OutboxRepository.sendMessage nên lúc offline tự vào hàng đợi.
 * - markSeen chỉ áp dụng cho ảnh người khác gửi và chỉ gọi một lần cho mỗi ảnh.
 */
package com.example.loki.ui.home

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loki.data.remote.api.PAGE_SIZE
import com.example.loki.data.remote.dto.PhotoActivityDto
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.data.remote.safeCall
import com.example.loki.data.remote.toUserMessage
import com.example.loki.data.repository.OutboxRepository
import com.example.loki.data.repository.PhotoExport
import com.example.loki.data.repository.PhotoRepository
import com.example.loki.data.repository.SessionRepository
import com.example.loki.push.PushEvents
import com.example.loki.push.PushType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class FeedUiState(
    val photos: List<PhotoDto> = emptyList(),
    val myId: Long? = null,
    // null = everyone; my own id = only my photos
    val friendFilter: Long? = null,
    val isRefreshing: Boolean = false,
    // The last refresh couldn't reach the backend: the cached history is shown as is
    val isOffline: Boolean = false,
    // One-off text for the snackbar ("Đã gửi", errors)
    val message: String? = null,
    // Who saw / reacted to one of my photos; null = the sheet is closed
    val activity: List<PhotoActivityDto>? = null,
    val isActivityLoading: Boolean = false,
    // A ready share chooser; the screen starts it, then calls shareHandled()
    val share: Intent? = null,
)

// The photo history below the camera: what I sent and what friends sent me.
// Photos come from the Room cache (instant, offline too); refreshing only writes into it
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val photos: PhotoRepository,
    private val outbox: OutboxRepository,
    private val export: PhotoExport,
    session: SessionRepository,
    pushEvents: PushEvents,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val filter = MutableStateFlow<Long?>(null)
    private var nextPage = 0 // 0 = the first page hasn't loaded yet, so there is nothing to page after
    private var endReached = false
    private var loadJob: Job? = null
    private val markedSeen = mutableSetOf<Long>()

    init {
        viewModelScope.launch { session.userId.collect { id -> _uiState.update { it.copy(myId = id) } } }
        viewModelScope.launch {
            filter.flatMapLatest { photos.observeHistory(it) }.collect { list -> _uiState.update { it.copy(photos = list) } }
        }
        // A friend sent a photo or reacted while the app is open: pick it up right away
        viewModelScope.launch {
            pushEvents.events.collect { type -> if (type == PushType.NEW_PHOTO || type == PushType.PHOTO_REACTION) checkForNew() }
        }
        refresh()
    }

    // The cached photos for the new filter show at once; the refresh then fills in the rest
    fun setFilter(friendId: Long?) {
        if (friendId == filter.value) return
        filter.value = friendId
        _uiState.update { it.copy(friendFilter = friendId) }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel() // a filter change must not be overwritten by an older page
        loadJob = viewModelScope.launch { loadFirstPage() }
    }

    // Back in the app or a push arrived: the newest page is merged in, older pages already cached stay
    fun checkForNew() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch { loadFirstPage() }
    }

    // Called when the pager gets near the last loaded photo
    fun loadMore() {
        if (nextPage == 0 || endReached || loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            safeCall { photos.loadPage(nextPage, filter.value) }
                .onSuccess { size ->
                    nextPage++
                    endReached = size < PAGE_SIZE
                    _uiState.update { it.copy(isOffline = false) }
                }
                .onFailure(::showError)
        }
    }

    private suspend fun loadFirstPage() {
        val scope = filter.value
        _uiState.update { it.copy(isRefreshing = true) }
        safeCall { photos.loadPage(0, scope) }
            .onSuccess { size ->
                nextPage = 1
                endReached = size < PAGE_SIZE
                _uiState.update { it.copy(isOffline = false) }
                if (size == 0 && scope != null) {
                    // Nothing from this friend: back to everyone instead of an empty history
                    _uiState.update { it.copy(message = "Người này chưa gửi ảnh nào cho bạn") }
                    setFilter(null)
                }
            }
            .onFailure(::showError)
        _uiState.update { it.copy(isRefreshing = false) }
    }

    // Only photos sent to me have a "seen" state
    fun markSeen(photo: PhotoDto) {
        if (photo.sender.id == _uiState.value.myId || photo.seenAt != null || !markedSeen.add(photo.id)) return
        viewModelScope.launch { safeCall { photos.markSeen(photo) } }
    }

    fun toggleReaction(photo: PhotoDto, emoji: String) {
        val myId = _uiState.value.myId ?: return
        viewModelScope.launch { safeCall { photos.toggleReaction(photo, emoji, myId) }.onFailure(::showError) }
    }

    // A reply lands in the chat with the photo's sender, with the photo attached
    fun reply(photo: PhotoDto, text: String) {
        val content = text.trim()
        if (content.isEmpty()) return
        viewModelScope.launch {
            safeCall { outbox.sendMessage(photo.sender.id, content, photo.id) }
                .onSuccess { sent ->
                    _uiState.update {
                        if (sent != null) it.copy(message = "Đã gửi tới ${photo.sender.name}")
                        // No connection (or older replies still waiting): OutboxWorker sends it later
                        else it.copy(message = "Sẽ gửi tới ${photo.sender.name} khi có mạng")
                    }
                }
                .onFailure(::showError)
        }
    }

    fun delete(photo: PhotoDto) {
        viewModelScope.launch {
            safeCall { photos.delete(photo.id) }
                .onSuccess { _uiState.update { it.copy(message = "Đã xoá ảnh") } }
                .onFailure(::showError)
        }
    }

    fun showActivity(photo: PhotoDto) {
        _uiState.update { it.copy(activity = emptyList(), isActivityLoading = true) }
        viewModelScope.launch {
            safeCall { photos.activity(photo.id) }
                .onSuccess { list -> _uiState.update { it.copy(activity = list, isActivityLoading = false) } }
                .onFailure { e ->
                    _uiState.update { it.copy(activity = null, isActivityLoading = false) }
                    showError(e)
                    if (e is IOException) _uiState.update { it.copy(message = "Cần có mạng để xem ai đã xem ảnh") }
                }
        }
    }

    fun closeActivity() = _uiState.update { it.copy(activity = null, isActivityLoading = false) }

    fun saveToGallery(photo: PhotoDto) {
        viewModelScope.launch {
            safeCall { export.saveToGallery(photo.imageUrl) }
                .onSuccess { _uiState.update { it.copy(message = "Đã lưu vào thư viện ảnh") } }
                .onFailure { e -> _uiState.update { it.copy(message = e.toUserMessage()) } }
        }
    }

    fun share(photo: PhotoDto) {
        viewModelScope.launch {
            safeCall { export.shareIntent(photo.imageUrl, photo.caption) }
                .onSuccess { intent -> _uiState.update { it.copy(share = intent) } }
                .onFailure { e -> _uiState.update { it.copy(message = e.toUserMessage()) } }
        }
    }

    fun shareHandled() = _uiState.update { it.copy(share = null) }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    // Offline is shown as a quiet label, not a snackbar on every failed refresh
    private fun showError(e: Throwable) {
        if (e is IOException) _uiState.update { it.copy(isOffline = true) }
        else _uiState.update { it.copy(message = e.toUserMessage()) }
    }
}
