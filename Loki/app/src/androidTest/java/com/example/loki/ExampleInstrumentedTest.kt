/*
 * TỔNG QUAN FILE: Instrumented test mẫu do Android Studio tạo, chạy trên emulator hoặc máy thật.
 *
 * Thành phần chính:
 * - useAppContext: kiểm tra package name của app là com.example.loki.
 *
 * Lưu ý:
 * - Đổi applicationId trước khi phát hành thì test này phải sửa theo.
 * - Thư mục androidTest là nơi đặt test Room in-memory và test giao diện Compose.
 */
package com.example.loki

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.loki", appContext.packageName)
    }
}