/*
 * TỔNG QUAN FILE: Unit test mẫu do Android Studio tạo, chạy trên JVM của máy tính.
 *
 * Thành phần chính:
 * - addition_isCorrect: kiểm tra 2 + 2 = 4, chỉ để chắc chắn bộ chạy test hoạt động.
 *
 * Lưu ý:
 * - Có thể xoá khi đã có test thật; nó không kiểm tra gì của Loki.
 */
package com.example.loki

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}