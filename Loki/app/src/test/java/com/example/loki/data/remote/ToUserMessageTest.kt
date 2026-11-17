/*
 * TỔNG QUAN FILE: Unit test cho Throwable.toUserMessage() trong ApiErrors.kt.
 *
 * Thành phần chính:
 * - httpError: dựng HttpException giả với mã và body tuỳ ý, không cần server.
 * - Các test: đọc message của backend, ưu tiên validationErrors, body không phải JSON, lỗi mất mạng,
 *   Cloudinary từ chối ảnh.
 *
 * Lưu ý:
 * - Test này giữ hợp đồng giữa GlobalExceptionHandler của backend và app; đổi định dạng lỗi thì phải cập nhật
 *   cả hai.
 * - Chạy bằng: ./gradlew :app:testDebugUnitTest
 */
package com.example.loki.data.remote

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import com.example.loki.data.remote.storage.UploadRejectedException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ToUserMessageTest {
    private fun httpError(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody()))

    @Test
    fun readsBackendMessage() {
        val e = httpError(401, """{"status":401,"error":"Unauthorized","message":"Wrong password"}""")
        assertEquals("Wrong password", e.toUserMessage())
    }

    @Test
    fun prefersValidationErrorsOverGenericMessage() {
        val e = httpError(400, """{"message":"Invalid input","validationErrors":{"password":"Too short"}}""")
        assertEquals("Too short", e.toUserMessage())
    }

    @Test
    fun fallsBackWhenBodyIsNotJson() {
        assertEquals("Máy chủ gặp lỗi (500). Thử lại sau nhé.", httpError(500, "<html>oops</html>").toUserMessage())
    }

    @Test
    fun uploadRejectedSaysWhy() {
        assertEquals("Không tải ảnh lên được: Invalid image file", UploadRejectedException("Invalid image file").toUserMessage())
    }

    @Test
    fun networkErrorSaysServerUnreachable() {
        assertEquals("Không kết nối được máy chủ. Kiểm tra mạng rồi thử lại.", IOException().toUserMessage())
    }
}
