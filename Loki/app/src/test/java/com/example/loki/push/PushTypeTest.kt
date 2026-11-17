/*
 * TỔNG QUAN FILE: Unit test cho PushType.from: đọc trường "type" của push backend gửi.
 *
 * Lưu ý:
 * - Tên phải khớp đúng chuỗi backend gửi (NotificationService / các Service gọi sendToUser).
 */
package com.example.loki.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PushTypeTest {
    @Test
    fun readsKnownTypes() {
        assertEquals(PushType.NEW_PHOTO, PushType.from("NEW_PHOTO"))
        assertEquals(PushType.NEW_MESSAGE, PushType.from("NEW_MESSAGE"))
        assertEquals(PushType.FRIEND_REQUEST, PushType.from("FRIEND_REQUEST"))
    }

    @Test
    fun unknownOrMissingIsNull() {
        assertNull(PushType.from("SOMETHING_NEW"))
        assertNull(PushType.from(null))
    }
}
