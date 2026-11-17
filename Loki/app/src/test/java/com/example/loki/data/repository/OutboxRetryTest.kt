/*
 * TỔNG QUAN FILE: Unit test cho isRetryable trong OutboxRepository: lỗi nào giữ mục trong hàng đợi để thử lại,
 * lỗi nào bỏ mục đó.
 *
 * Lưu ý:
 * - Nếu một lỗi không thử lại được mà bị coi là thử lại được, mục đó sẽ chặn mãi cả hàng đợi phía sau.
 */
package com.example.loki.data.repository

import com.example.loki.data.remote.storage.UploadRejectedException
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class OutboxRetryTest {
    private fun http(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun connectionAndServerTroubleAreRetried() {
        assertTrue(isRetryable(IOException()))
        listOf(401, 408, 429, 500, 503).forEach { assertTrue("HTTP $it", isRetryable(http(it))) }
    }

    @Test
    fun refusalsAreDropped() {
        listOf(400, 403, 404, 409).forEach { assertFalse("HTTP $it", isRetryable(http(it))) }
        assertFalse(isRetryable(UploadRejectedException("Invalid image file")))
        assertFalse(isRetryable(SerializationException("bad json")))
    }
}
