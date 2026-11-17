/*
 * TỔNG QUAN FILE: ViewModel gửi ảnh (chụp hoặc chọn từ thư viện): chọn người nhận, nén rồi gửi qua outbox.
 *
 * Thành phần chính:
 * - SendPhotoUiState: người được chọn (rỗng = mọi người), đang gửi, số lần gửi thành công, số ảnh chờ gửi,
 *   lỗi.
 * - selectEveryone, toggle: chọn người nhận.
 * - send: nén ảnh rồi gọi OutboxRepository.sendPhoto. Mất mạng ở bước nào thì ảnh nằm trong outbox và coi như
 *   đã gửi.
 *
 * Lưu ý:
 * - Logic tải lên / báo backend / xếp hàng nằm ở OutboxRepository, ViewModel không gọi API trực tiếp.
 * - sentCount tăng sau mỗi lần gửi thành công; CameraPage dựa vào nó để reset màn hình và chạy hiệu ứng.
 * - Gửi thất bại vì lý do khác thì ảnh vẫn ở trên màn hình, bấm gửi lại là thử lại.
 */
package com.example.loki.ui.camera

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loki.core.util.compressForUpload
import com.example.loki.data.remote.safeCall
import com.example.loki.data.remote.toUserMessage
import com.example.loki.data.repository.OutboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SendPhotoUiState(
    // Empty = everyone, which is what Locket selects by default
    val selectedIds: Set<Long> = emptySet(),
    val isSending: Boolean = false,
    // Bumped after each successful send, so the screen can reset and play its "sent" animation
    val sentCount: Int = 0,
    // Photos sent offline, waiting for a connection
    val pendingCount: Int = 0,
    val error: String? = null,
)

@HiltViewModel
class SendPhotoViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val outbox: OutboxRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SendPhotoUiState())
    val uiState: StateFlow<SendPhotoUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { outbox.pendingPhotoCount.collect { n -> _uiState.update { it.copy(pendingCount = n) } } }
    }

    fun selectEveryone() = _uiState.update { it.copy(selectedIds = emptySet()) }

    fun toggle(friendId: Long) {
        _uiState.update {
            it.copy(selectedIds = if (friendId in it.selectedIds) it.selectedIds - friendId else it.selectedIds + friendId)
        }
    }

    fun errorShown() = _uiState.update { it.copy(error = null) }

    // compress -> upload -> tell the backend who gets it. No connection: queued, counts as sent
    fun send(photo: Uri, caption: String, allFriendIds: List<Long>) {
        val recipients = _uiState.value.selectedIds.ifEmpty { allFriendIds.toSet() }
        if (_uiState.value.isSending) return
        if (recipients.isEmpty()) {
            _uiState.update { it.copy(error = "Hãy thêm bạn bè trước khi gửi ảnh") }
            return
        }
        _uiState.update { it.copy(isSending = true, error = null) }

        val text = caption.trim().ifBlank { null }
        viewModelScope.launch {
            safeCall {
                val jpeg = withContext(Dispatchers.IO) { compressForUpload(context, photo) }
                outbox.sendPhoto(jpeg, text, recipients.toList())
            }
                .onSuccess { _uiState.update { it.copy(isSending = false, sentCount = it.sentCount + 1, selectedIds = emptySet()) } }
                .onFailure { e ->
                    // The shot stays on screen, so the send button simply retries
                    _uiState.update { it.copy(isSending = false, error = e.toUserMessage()) }
                }
        }
    }
}
