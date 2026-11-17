/*
 * TỔNG QUAN FILE: Unit test cho asReactionEmoji: chỉ nhận một emoji để thả cảm xúc.
 */
package com.example.loki.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmojiTest {
    @Test
    fun acceptsEmojiIncludingCombined() {
        assertEquals("🥹", asReactionEmoji(" 🥹 "))
        assertEquals("👍🏽", asReactionEmoji("👍🏽"))
        assertEquals("👨‍👩‍👧", asReactionEmoji("👨‍👩‍👧"))
    }

    @Test
    fun rejectsTextAndTooLong() {
        assertNull(asReactionEmoji(""))
        assertNull(asReactionEmoji("hi"))
        assertNull(asReactionEmoji("🔥 ok"))
        assertNull(asReactionEmoji("🔥🔥🔥🔥🔥🔥"))
    }
}
