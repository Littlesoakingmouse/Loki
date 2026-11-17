/*
 * TỔNG QUAN FILE: ViewModel bạn bè, dùng chung cho Home và FriendsSheet.
 *
 * Thành phần chính:
 * - FriendsUiState: danh sách bạn, lời mời đang chờ, người đã chặn, người tìm thấy, thông báo.
 * - reload: làm mới ngầm khi Home được mở lại hoặc có push FRIEND_REQUEST, lỗi không hiện ra.
 * - search, openInvite, sendRequest, accept, decline, unfriend: các hành động trong sheet.
 * - block, unblock, report: chặn / bỏ chặn / báo cáo (từ sheet hoặc từ menu của một ảnh).
 * - perform: chạy một hành động và hiện lỗi (nếu có) trong sheet.
 *
 * Lưu ý:
 * - Mọi lệnh đi qua FriendRepository; ViewModel không gọi API trực tiếp.
 * - Danh sách bạn đọc từ cache Room nên hiện ngay khi mở app, kể cả offline.
 * - Chặn xong thì Home phải làm mới lịch sử (HomeScreen gọi FeedViewModel.refresh) để ảnh của người đó biến mất.
 */
package com.example.loki.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loki.data.remote.dto.FriendshipDto
import com.example.loki.data.remote.dto.UserDto
import com.example.loki.data.remote.safeCall
import com.example.loki.data.remote.toUserMessage
import com.example.loki.data.repository.FriendRepository
import com.example.loki.push.PushEvents
import com.example.loki.push.PushType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FriendsUiState(
    val friends: List<UserDto> = emptyList(),
    val pending: List<FriendshipDto> = emptyList(),
    val blocked: List<UserDto> = emptyList(),
    // From the email search or an opened invite link
    val found: UserDto? = null,
    val message: String? = null,
)

// Shared by Home (counter, filter, recipients) and the friends sheet
@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friends: FriendRepository,
    pushEvents: PushEvents,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FriendsUiState())
    val uiState: StateFlow<FriendsUiState> = _uiState.asStateFlow()

    init {
        // Cached friends show at once (counter, filter, recipient row); load() refreshes them
        viewModelScope.launch { friends.observeFriends().collect { list -> _uiState.update { it.copy(friends = list) } } }
        viewModelScope.launch { pushEvents.events.collect { if (it == PushType.FRIEND_REQUEST) reload() } }
    }

    // Background refresh on resume: offline just keeps the cached list, no error in the sheet
    fun reload() {
        viewModelScope.launch { safeCall { load() } }
    }

    fun search(email: String) = perform {
        val user = friends.searchByEmail(email)
        _uiState.update { it.copy(found = user, message = null) }
    }

    // An invite link was opened: show who sent it, with an Add button
    fun openInvite(userId: Long) = perform {
        val user = friends.byId(userId)
        val alreadyFriends = _uiState.value.friends.any { it.id == user.id }
        _uiState.update {
            if (alreadyFriends) it.copy(message = "${user.name} đã là bạn của bạn") else it.copy(found = user, message = null)
        }
    }

    fun sendRequest(user: UserDto) = perform {
        friends.sendRequest(user)
        _uiState.update { it.copy(found = null, message = "Đã gửi lời mời tới ${user.name}") }
    }

    fun accept(request: FriendshipDto) = perform {
        friends.accept(request)
        load()
    }

    fun decline(request: FriendshipDto) = perform {
        friends.decline(request)
        load()
    }

    fun unfriend(user: UserDto) = perform {
        friends.unfriend(user)
        load()
    }

    // onDone: e.g. Home refreshes the history so their photos disappear
    fun block(user: UserDto, onDone: () -> Unit = {}) = perform {
        friends.block(user)
        _uiState.update { it.copy(found = null, message = "Đã chặn ${user.name}") }
        load()
        onDone()
    }

    fun unblock(user: UserDto) = perform {
        friends.unblock(user)
        _uiState.update { it.copy(message = "Đã bỏ chặn ${user.name}") }
        load()
    }

    fun report(user: UserDto, photoId: Long?, reason: String?) = perform {
        friends.report(user, photoId, reason)
        _uiState.update { it.copy(message = "Đã gửi báo cáo. Cảm ơn bạn!") }
    }

    fun clearFound() = _uiState.update { it.copy(found = null, message = null) }

    fun messageShown() = _uiState.update { it.copy(message = null) }

    private suspend fun load() {
        friends.refreshFriends()
        val pending = friends.pendingRequests()
        val blocked = friends.blocked()
        _uiState.update { it.copy(pending = pending, blocked = blocked) }
    }

    // Runs one action and shows its error, if any, in the sheet
    private fun perform(action: suspend () -> Unit) {
        viewModelScope.launch {
            safeCall { action() }.onFailure { e -> _uiState.update { it.copy(message = e.toUserMessage()) } }
        }
    }
}
