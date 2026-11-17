/*
 * TỔNG QUAN FILE: ViewModel thanh trên của Home: hồ sơ của mình và số tin chưa đọc.
 *
 * Thành phần chính:
 * - HomeUiState: me, unreadChats.
 * - init: lắng nghe hồ sơ và tổng chưa đọc từ Room; có push NEW_MESSAGE thì làm mới hộp thư.
 * - reload: làm mới hồ sơ và hộp thư khi Home được mở lại.
 *
 * Lưu ý:
 * - Offline thì giữ giá trị cũ trong cache, không báo lỗi.
 */
package com.example.loki.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loki.data.remote.dto.UserDto
import com.example.loki.data.remote.safeCall
import com.example.loki.data.repository.ChatRepository
import com.example.loki.data.repository.UserRepository
import com.example.loki.push.PushEvents
import com.example.loki.push.PushType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val me: UserDto? = null,
    val unreadChats: Long = 0,
)

// The top bar: my avatar and the unread-chat badge, both read from the cache
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val users: UserRepository,
    private val chat: ChatRepository,
    pushEvents: PushEvents,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { users.observeMe().collect { me -> _uiState.update { it.copy(me = me) } } }
        viewModelScope.launch { chat.observeUnreadTotal().collect { n -> _uiState.update { it.copy(unreadChats = n) } } }
        // A new message while Home is open bumps the chat badge
        viewModelScope.launch {
            pushEvents.events.collect { if (it == PushType.NEW_MESSAGE) safeCall { chat.refreshConversations() } }
        }
    }

    // Called whenever Home comes back, e.g. after editing the profile or reading a chat.
    // Offline = the cached values stay on screen
    fun reload() {
        viewModelScope.launch { safeCall { users.refreshMe() } }
        viewModelScope.launch { safeCall { chat.refreshConversations() } }
    }
}
