/*
 * TỔNG QUAN FILE: ViewModel của màn trò chuyện: tin nhắn, tin đang chờ, bản nháp và việc gửi.
 *
 * Thành phần chính:
 * - ChatUiState: tin nhắn (mới nhất trước), tin đang chờ, myId, bản nháp, đang gửi, offline, lỗi.
 * - route: tham số ChatRoute đọc từ SavedStateHandle.
 * - conversationId: null cho tới khi biết; dùng flatMapLatest để chuyển sang lắng nghe đúng cuộc trò chuyện.
 * - pollMessages: chỉ chạy khi màn hình đang hiện. Tải ngay khi có push NEW_MESSAGE, ngoài ra tải lại mỗi
 *   15 giây phòng khi push không tới; đánh dấu đã đọc khi có tin mới từ người kia.
 * - send: gửi qua OutboxRepository.sendMessage; mất mạng hoặc còn tin đang chờ thì tin nằm trong outbox.
 *
 * Lưu ý:
 * - Shortcut: chỉ 50 tin gần nhất, chưa phân trang; chưa có WebSocket (push + polling thưa thay thế).
 * - Tin mới phải xếp sau tin đang chờ cho cùng người nhận, để đến nơi đúng thứ tự (OutboxRepository lo việc
 *   này).
 * - Cuộc trò chuyện mở từ danh sách bạn hay từ trả lời ảnh có thể chưa tồn tại; backend tạo nó ở tin đầu tiên.
 */
package com.example.loki.ui.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.loki.data.local.db.entity.PendingMessage
import com.example.loki.data.remote.dto.MessageDto
import com.example.loki.data.remote.safeCall
import com.example.loki.data.remote.toUserMessage
import com.example.loki.data.repository.ChatRepository
import com.example.loki.data.repository.OutboxRepository
import com.example.loki.data.repository.SessionRepository
import com.example.loki.push.PushEvents
import com.example.loki.push.PushType
import com.example.loki.ui.navigation.ChatRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

// Push is the main signal; this only catches a push that never arrived
private const val FALLBACK_POLL_MS = 15_000L

data class ChatUiState(
    val messages: List<MessageDto> = emptyList(), // newest first
    // Sent offline, waiting for a connection; newer than every message above
    val pending: List<PendingMessage> = emptyList(),
    val myId: Long? = null,
    val draft: String = "",
    val isSending: Boolean = false,
    val isOffline: Boolean = false,
    val error: String? = null,
)

// Messages come from the Room cache, so a chat opens with what was there last time, offline too
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chat: ChatRepository,
    private val outbox: OutboxRepository,
    private val pushEvents: PushEvents,
    session: SessionRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<ChatRoute>()
    val title = route.otherUserName
    val otherUserId = route.otherUserId

    // null until known: a chat started from a friend or a photo reply may not exist yet
    private val conversationId = MutableStateFlow(route.conversationId)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { session.userId.collect { id -> _uiState.update { it.copy(myId = id) } } }
        viewModelScope.launch {
            conversationId.filterNotNull().flatMapLatest { chat.observeMessages(it) }
                .collect { messages -> _uiState.update { it.copy(messages = messages) } }
        }
        viewModelScope.launch {
            outbox.observePendingMessagesTo(route.otherUserId).collect { pending -> _uiState.update { it.copy(pending = pending) } }
        }
    }

    fun onDraftChange(text: String) = _uiState.update { it.copy(draft = text) }

    // Run while the screen is visible (repeatOnLifecycle), so "read" is only reported for messages actually on screen
    suspend fun pollMessages(): Unit = coroutineScope {
        launch { pushEvents.events.filter { it == PushType.NEW_MESSAGE }.collect { loadMessages() } }
        while (true) {
            loadMessages()
            delay(FALLBACK_POLL_MS)
        }
    }

    private suspend fun loadMessages() {
        safeCall {
            val id = conversationId.value ?: chat.conversationWith(route.otherUserId)?.also { conversationId.value = it }
                ?: return@safeCall
            val before = _uiState.value.messages.firstOrNull()?.id
            val newest = chat.refreshMessages(id).firstOrNull()
            // Something new from the other person while the chat is open: it's read
            if (newest != null && newest.id != before && newest.sender.id == route.otherUserId) chat.markRead(id)
        }
            .onSuccess { _uiState.update { it.copy(isOffline = false, error = null) } }
            .onFailure(::showError)
    }

    fun send() {
        val text = _uiState.value.draft.trim()
        if (text.isEmpty() || _uiState.value.isSending) return
        // Cleared right away: typing on while it sends must not be wiped afterwards
        _uiState.update { it.copy(isSending = true, draft = "") }

        viewModelScope.launch {
            safeCall { outbox.sendMessage(route.otherUserId, text) }
                .onSuccess { message ->
                    if (message != null) conversationId.value = message.conversationId
                    _uiState.update { it.copy(isSending = false, isOffline = message == null && it.isOffline, error = null) }
                }
                .onFailure { e ->
                    // Refused (e.g. no longer friends): give the text back so it isn't lost
                    _uiState.update { it.copy(isSending = false, draft = text + it.draft) }
                    showError(e)
                }
        }
    }

    private fun showError(e: Throwable) {
        if (e is IOException) _uiState.update { it.copy(isOffline = true) }
        else _uiState.update { it.copy(error = e.toUserMessage()) }
    }
}
