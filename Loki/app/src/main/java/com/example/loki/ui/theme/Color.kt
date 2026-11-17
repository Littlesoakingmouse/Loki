/*
 * TỔNG QUAN FILE: Bảng màu của Loki: nền gần đen và một màu nhấn vàng.
 *
 * Thành phần chính:
 * - LokiYellow, LokiOnYellow, LokiBlack, LokiSurface..., LokiOnSurface, LokiMuted, LokiError.
 *
 * Lưu ý:
 * - Màn hình không nên dùng trực tiếp các màu này mà đọc qua MaterialTheme.colorScheme (xem Theme.kt).
 */
package com.example.loki.ui.theme

import androidx.compose.ui.graphics.Color

// Locket-style palette: near-black surfaces with one warm yellow accent
val LokiYellow = Color(0xFFFFC83D)
val LokiOnYellow = Color(0xFF1A1400)
val LokiBlack = Color(0xFF000000)
val LokiSurface = Color(0xFF141414)
val LokiSurfaceHigh = Color(0xFF242424)
val LokiSurfaceHighest = Color(0xFF333333)
val LokiOnSurface = Color(0xFFF5F5F5)
val LokiMuted = Color(0xFF9A9A9A)
val LokiError = Color(0xFFFF6B6B)
