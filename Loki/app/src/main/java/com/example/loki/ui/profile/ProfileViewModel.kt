/*
 * TỔNG QUAN FILE: ViewModel màn hồ sơ.
 *
 * Thành phần chính:
 * - ProfileUiState: hồ sơ, đang lưu, đang tải avatar, đang đổi mật khẩu, thông báo.
 * - notificationsEnabled, setNotificationsEnabled: đọc và ghi SettingsStore.
 * - save: đổi tên. changeAvatar: nén ảnh 512 px rồi UserRepository.changeAvatar.
 * - changePassword: chỉ với tài khoản email + mật khẩu (UserDto.hasPassword).
 * - logout, deleteAccount: qua SessionRepository.
 *
 * Lưu ý:
 * - Mọi lệnh đi qua UserRepository / SessionRepository; repository tự lưu hồ sơ mới vào cache để mọi màn hình
 *   khác thấy tên hoặc avatar mới.
 * - Lỗi mạng khi làm mới hồ sơ được bỏ qua vì hồ sơ cache vẫn đang hiện.
 */
package com.example.loki.ui.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loki.core.util.compressForUpload
import com.example.loki.data.local.datastore.SettingsStore
import com.example.loki.data.remote.dto.UserDto
import com.example.loki.data.remote.safeCall
import com.example.loki.data.remote.toUserMessage
import com.example.loki.data.repository.SessionRepository
import com.example.loki.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

// Same rule as the backend's ChangePasswordRequest / RegisterRequest
const val MIN_PASSWORD_LENGTH = 6

data class ProfileUiState(
    val user: UserDto? = null,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isChangingPassword: Boolean = false,
    // Bumped after a successful change, so the dialog can close
    val passwordChangedCount: Int = 0,
    val message: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionRepository,
    private val settings: SettingsStore,
    private val users: UserRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val notificationsEnabled: StateFlow<Boolean> =
        settings.notificationsEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
    }

    init {
        // The cached profile shows at once, offline too; the refresh brings it up to date
        viewModelScope.launch { users.observeMe().collect { me -> if (me != null) _uiState.update { it.copy(user = me) } } }
        viewModelScope.launch {
            safeCall { users.refreshMe() }
                .onFailure { e -> if (e !is IOException) _uiState.update { it.copy(message = e.toUserMessage()) } }
        }
    }

    fun save(displayName: String) {
        if (displayName.isBlank() || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            safeCall { users.updateDisplayName(displayName) }
                .onSuccess { user -> _uiState.update { it.copy(user = user, message = "Đã lưu") } }
                .onFailure { e -> _uiState.update { it.copy(message = e.toUserMessage()) } }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    // Picked from the gallery: square-crop, upload, then save the URL on the profile
    fun changeAvatar(image: Uri) {
        if (_uiState.value.isUploadingAvatar) return
        _uiState.update { it.copy(isUploadingAvatar = true, message = null) }
        viewModelScope.launch {
            safeCall {
                val jpeg = withContext(Dispatchers.IO) { compressForUpload(context, image, maxSide = 512) }
                users.changeAvatar(jpeg)
            }
                .onSuccess { user -> _uiState.update { it.copy(user = user, message = "Đã đổi ảnh đại diện") } }
                .onFailure { e -> _uiState.update { it.copy(message = e.toUserMessage()) } }
            _uiState.update { it.copy(isUploadingAvatar = false) }
        }
    }

    fun changePassword(current: String, new: String) {
        if (_uiState.value.isChangingPassword) return
        if (new.length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(message = "Mật khẩu mới phải có tối thiểu $MIN_PASSWORD_LENGTH ký tự") }
            return
        }
        _uiState.update { it.copy(isChangingPassword = true, message = null) }
        viewModelScope.launch {
            safeCall { users.changePassword(current, new) }
                .onSuccess { _uiState.update { it.copy(message = "Đã đổi mật khẩu", passwordChangedCount = it.passwordChangedCount + 1) } }
                .onFailure { e -> _uiState.update { it.copy(message = e.toUserMessage()) } }
            _uiState.update { it.copy(isChangingPassword = false) }
        }
    }

    // Clearing the token switches the app back to Login
    fun logout() {
        viewModelScope.launch { session.logout() }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            safeCall { session.deleteAccount() }.onFailure { e -> _uiState.update { it.copy(message = e.toUserMessage()) } }
        }
    }
}
