/*
 * TỔNG QUAN FILE: Theme Material 3 của app, luôn ở chế độ tối.
 *
 * Thành phần chính:
 * - LokiColorScheme: darkColorScheme ánh xạ bảng màu Loki vào các vai trò của Material.
 * - LokiShapes: độ bo góc nhỏ, vừa, lớn, rất lớn.
 * - LokiTheme: bọc MaterialTheme, được gọi một lần trong MainActivity.
 *
 * Lưu ý:
 * - App không có theme sáng; themes.xml cũng đặt nền đen để không nháy trắng lúc mở app.
 */
package com.example.loki.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Always dark, like Locket: photos pop against black and the camera feels like a viewfinder
private val LokiColorScheme = darkColorScheme(
    primary = LokiYellow,
    onPrimary = LokiOnYellow,
    primaryContainer = LokiYellow,
    onPrimaryContainer = LokiOnYellow,
    secondary = LokiSurfaceHighest,
    onSecondary = LokiOnSurface,
    secondaryContainer = LokiSurfaceHigh,
    onSecondaryContainer = LokiOnSurface,
    background = LokiBlack,
    onBackground = LokiOnSurface,
    surface = LokiBlack,
    onSurface = LokiOnSurface,
    surfaceVariant = LokiSurfaceHigh,
    onSurfaceVariant = LokiMuted,
    surfaceContainerLowest = LokiBlack,
    surfaceContainerLow = LokiSurface,
    surfaceContainer = LokiSurface,
    surfaceContainerHigh = LokiSurfaceHigh,
    surfaceContainerHighest = LokiSurfaceHighest,
    outline = LokiSurfaceHighest,
    outlineVariant = LokiSurfaceHigh,
    error = LokiError,
)

private val LokiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

@Composable
fun LokiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LokiColorScheme,
        typography = Typography,
        shapes = LokiShapes,
        content = content,
    )
}
