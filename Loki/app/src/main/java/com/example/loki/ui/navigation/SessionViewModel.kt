/*
 * TỔNG QUAN FILE: ViewModel cấp app theo dõi trạng thái đăng nhập.
 *
 * Thành phần chính:
 * - isLoggedIn: StateFlow ba trạng thái (null: đang đọc token, true, false).
 * - init: gia hạn token khi mở app; cập nhật widget mỗi khi đăng nhập hoặc đăng xuất.
 *
 * Lưu ý:
 * - Dùng Boolean? có chủ ý: nếu mặc định false, màn Login sẽ nháy lên một chút trước khi vào Home.
 */
package com.example.loki.ui.navigation

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loki.data.repository.SessionRepository
import com.example.loki.widget.LokiWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    @ApplicationContext context: Context,
    session: SessionRepository,
) : ViewModel() {
    // null while the stored token is still being read
    val isLoggedIn: StateFlow<Boolean?> = session.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { session.refresh() }
        // Log in / log out / app start: show the right photo (or none) on the home screen widget
        viewModelScope.launch { session.isLoggedIn.collect { LokiWidget().updateAll(context) } }
    }
}
