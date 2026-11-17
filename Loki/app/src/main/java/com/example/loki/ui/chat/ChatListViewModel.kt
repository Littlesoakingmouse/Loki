/*
 * TỔNG QUAN FILE: ViewModel của màn hộp thư.
 *
 * Thành phần chính:
 * - ChatListUiState: danh sách cuộc trò chuyện, cờ offline, lỗi.
 * - init: lắng nghe hộp thư từ Room. load: gọi ChatRepository.refreshConversations.
 *
 * Lưu ý:
 * - Mất mạng chỉ bật nhãn offline, không hiện lỗi; danh sách cũ vẫn giữ trên màn hình.
 */
package com.example.loki.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loki.data.remote.dto.ConversationDto
import com.example.loki.data.remote.safeCall
import com.example.loki.data.remote.toUserMessage
import com.example.loki.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

data class ChatListUiState(
    val conversations: List<ConversationDto> = emptyList(),
    val isOffline: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val chat: ChatRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { chat.observeConversations().collect { list -> _uiState.update { it.copy(conversations = list) } } }
    }

    // The cached inbox is already on screen; this brings it up to date
    fun load() {
        viewModelScope.launch {
            safeCall { chat.refreshConversations() }
                .onSuccess { _uiState.update { it.copy(isOffline = false, error = null) } }
                .onFailure { e ->
                    if (e is IOException) _uiState.update { it.copy(isOffline = true) }
                    else _uiState.update { it.copy(error = e.toUserMessage()) }
                }
        }
    }
}
