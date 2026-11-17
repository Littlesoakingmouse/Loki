/*
 * TỔNG QUAN FILE: Kiểm tra chuỗi người dùng gõ có phải một emoji để thả cảm xúc hay không.
 *
 * Thành phần chính:
 * - asReactionEmoji(text): trả về emoji đã cắt khoảng trắng, hoặc null nếu không hợp lệ.
 *
 * Lưu ý:
 * - Kiểm tra đơn giản, không cần thư viện: không rỗng, tối đa MAX_EMOJI_LENGTH ký tự UTF-16 (giới hạn cột emoji
 *   của backend), không chứa chữ, số hay khoảng trắng. Emoji ghép (da màu, gia đình, cờ) vẫn qua được.
 */
package com.example.loki.core.util

// The backend's emoji column holds 10 characters
const val MAX_EMOJI_LENGTH = 10

fun asReactionEmoji(text: String): String? {
    val emoji = text.trim()
    if (emoji.isEmpty() || emoji.length > MAX_EMOJI_LENGTH) return null
    val plainText = emoji.codePoints().anyMatch { Character.isLetterOrDigit(it) || Character.isWhitespace(it) }
    return emoji.takeUnless { plainText }
}
