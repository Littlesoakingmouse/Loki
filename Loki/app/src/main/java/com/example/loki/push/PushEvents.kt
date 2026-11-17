/*
 * TỔNG QUAN FILE: Kênh sự kiện trong app để màn hình đang mở phản ứng ngay khi có push.
 *
 * Thành phần chính:
 * - PushType: các loại push backend gửi (trường "type"); PushType.from đọc chuỗi, loại lạ thành null.
 * - events: SharedFlow mà các ViewModel lắng nghe; emit: LokiMessagingService gọi.
 *
 * Lưu ý:
 * - Bắt buộc @Singleton: nơi phát và nơi nghe phải dùng cùng một instance.
 * - Không ai nghe thì sự kiện bị bỏ; màn hình vẫn tự làm mới khi được mở lại.
 * - Thêm loại push mới ở backend thì thêm vào enum này; tên phải khớp đúng chuỗi backend gửi.
 */
package com.example.loki.push

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// The push's "type" field, as the backend sends it
enum class PushType {
    NEW_PHOTO,
    PHOTO_REACTION,
    NEW_MESSAGE,
    FRIEND_REQUEST;

    companion object {
        fun from(raw: String?): PushType? = entries.find { it.name == raw }
    }
}

// Lets open screens react to a push (e.g. show a friend's new photo) without waiting for a manual refresh
@Singleton
class PushEvents @Inject constructor() {
    // Nobody listening (app in the background) = the event is simply dropped; screens refresh on resume anyway
    private val _events = MutableSharedFlow<PushType>(extraBufferCapacity = 8)
    val events: SharedFlow<PushType> = _events.asSharedFlow()

    fun emit(type: PushType) {
        _events.tryEmit(type)
    }
}
