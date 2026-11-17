/*
 * TỔNG QUAN FILE: Unit test cho parseServerTime: thời gian backend (có múi giờ) đổi sang giờ của máy.
 *
 * Lưu ý:
 * - Truyền zone vào để test không phụ thuộc múi giờ của máy chạy test.
 */
package com.example.loki.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ServerTimeTest {
    @Test
    fun convertsToThePhonesZone() {
        // 10:00 in Vietnam is 04:00 in Paris (winter time, UTC+1)
        val paris = parseServerTime("2026-11-18T10:00:00+07:00", ZoneId.of("Europe/Paris"))
        assertEquals(LocalDateTime.of(2026, 11, 18, 4, 0), paris)
    }

    @Test
    fun zonelessTextIsReadAsPhoneTime() {
        assertEquals(LocalDateTime.of(2026, 10, 18, 11, 40, 37), parseServerTime("2026-10-18T11:40:37", ZoneId.of("UTC")))
    }

    @Test
    fun garbageIsNull() {
        assertNull(parseServerTime("yesterday"))
        assertNull(parseServerTime(null))
    }
}
