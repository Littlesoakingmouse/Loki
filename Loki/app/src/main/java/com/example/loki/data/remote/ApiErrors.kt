/*
 * TỔNG QUAN FILE: Xử lý lỗi gọi mạng dùng chung cho cả app.
 *
 * Thành phần chính:
 * - Throwable.toUserMessage(): biến exception thành câu tiếng Việt hiển thị cho người dùng. HttpException thì
 *   đọc JSON lỗi của backend (ưu tiên validationErrors theo từng trường, rồi tới message); IOException thì báo
 *   mất mạng; UploadRejectedException thì báo không tải ảnh lên được. Mọi câu đều bằng tiếng Việt.
 * - safeCall { }: giống runCatching nhưng ném lại CancellationException để coroutine bị huỷ vẫn dừng đúng
 *   cách.
 * - ApiError (private): khớp với ApiErrorResponse của GlobalExceptionHandler bên backend.
 *
 * Lưu ý:
 * - Đổi định dạng JSON lỗi ở backend thì phải sửa ApiError ở đây, và cập nhật ToUserMessageTest.
 * - Không bắt CancellationException trong safeCall, nếu không coroutine bị huỷ sẽ chạy tiếp và hiện lỗi vô lý.
 * - ViewModel dựa vào việc phân biệt IOException (mất mạng: hiện nhãn offline hoặc đưa vào outbox) với
 *   HttpException (server từ chối: hiện lỗi).
 */
package com.example.loki.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import com.example.loki.data.remote.storage.UploadRejectedException
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

// The backend's ApiErrorResponse: {status, error, message, validationErrors, ...}
@Serializable
private data class ApiError(
    val message: String? = null,
    val validationErrors: Map<String, String>? = null,
)

fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> response()?.errorBody()?.string()
        ?.let { body -> runCatching { errorJson.decodeFromString<ApiError>(body) }.getOrNull() }
        // Field errors ("password too short") say more than the generic "invalid input"
        ?.let { it.validationErrors?.values?.joinToString("\n")?.ifBlank { null } ?: it.message }
        ?: "Máy chủ gặp lỗi (${code()}). Thử lại sau nhé."
    is IOException -> "Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại."
    is UploadRejectedException -> "Không tải ảnh lên được: $message"
    else -> message ?: "Đã có lỗi xảy ra"
}

// Like runCatching, but lets coroutine cancellation through
suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
