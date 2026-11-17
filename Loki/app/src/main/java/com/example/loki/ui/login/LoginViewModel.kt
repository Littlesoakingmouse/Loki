/*
 * TỔNG QUAN FILE: ViewModel màn đăng nhập.
 *
 * Thành phần chính:
 * - LoginUiState: isLoading, error.
 * - login, register, loginWithGoogle: đều đi qua submit.
 * - submit: chặn bấm lặp khi đang tải, chạy coroutine và hiện lỗi nếu thất bại.
 * - googleSignInFailed: màn hình báo lỗi khi bộ chọn tài khoản Google lỗi.
 *
 * Lưu ý:
 * - Bộ chọn tài khoản Google cần Activity nên LoginScreen tự mở nó rồi chỉ đưa idToken vào đây; ViewModel không
 *   giữ Context của Activity (xoay màn hình không làm rò rỉ Activity cũ).
 * - Người dùng huỷ bộ chọn Google thì LoginScreen không gọi gì, không hiện lỗi.
 */
package com.example.loki.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loki.data.remote.safeCall
import com.example.loki.data.remote.toUserMessage
import com.example.loki.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) = submit { session.login(email, password) }

    fun register(email: String, password: String, displayName: String) =
        submit { session.register(email, password, displayName) }

    // The idToken from Google's account picker (opened by the screen, which owns the Activity)
    fun loginWithGoogle(idToken: String) = submit { session.loginWithGoogle(idToken) }

    fun googleSignInFailed(e: Throwable) {
        _uiState.value = LoginUiState(error = e.toUserMessage())
    }

    private fun submit(call: suspend () -> Unit) {
        if (_uiState.value.isLoading) return
        _uiState.value = LoginUiState(isLoading = true)

        viewModelScope.launch {
            // On success the saved token flips SessionViewModel.isLoggedIn and the app shows Home
            safeCall { call() }.onFailure { _uiState.value = LoginUiState(error = it.toUserMessage()) }
        }
    }
}
