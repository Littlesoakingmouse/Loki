/*
 * TỔNG QUAN FILE: Các Composable và hàm tiện ích dùng chung giữa nhiều màn hình.
 *
 * Thành phần chính:
 * - Avatar: ảnh đại diện tròn, hoặc chữ cái đầu của tên trên nền xám.
 * - RoundIconButton: nút tròn tối quanh camera và trên ảnh.
 * - Pill: nút dạng viên thuốc cho bộ đếm bạn và bộ lọc lịch sử.
 * - CountBadge: số đỏ nhỏ trên icon. OfflineBadge: nhãn "đang offline".
 * - timeAgo, clockTime: đổi chuỗi thời gian ISO thành "5 phút", "18/10", "11:40" theo giờ của máy.
 * - parseServerTime: đọc thời gian backend (có múi giờ) và đổi sang giờ địa phương của máy.
 *
 * Lưu ý:
 * - Backend gửi thời gian kèm múi giờ ("...+07:00"), nên máy ở múi giờ khác vẫn hiện đúng. Chuỗi cũ không có
 *   múi giờ (cache từ bản trước) được hiểu là giờ của máy.
 */
package com.example.loki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.loki.core.util.thumbnail
import com.example.loki.data.remote.dto.UserDto
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Round avatar: the photo when there is one, otherwise the first letter of the name on a grey disc
@Composable
fun Avatar(
    user: UserDto?,
    size: Dp,
    modifier: Modifier = Modifier,
    ringColor: Color? = null,
) {
    val ring = if (ringColor != null) Modifier.border(2.dp, ringColor, CircleShape).padding(3.dp) else Modifier
    Box(
        modifier = modifier.size(size).then(ring).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        val url = user?.avatarUrl
        if (url != null) {
            AsyncImage(model = thumbnail(url, 256), contentDescription = user.name, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Text(
                text = user?.name?.firstOrNull()?.uppercase() ?: "?",
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp,
            )
        }
    }
}

// The dark round buttons around the camera and on top of photos
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(background).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

// Rounded "chip" used for the friends counter and the history filter
@Composable
fun Pill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.clip(CircleShape).background(background).clickable(onClick = onClick).padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}

// Small red counter on top of an icon
@Composable
fun CountBadge(count: Long, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.error).padding(horizontal = 6.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(if (count > 99) "99+" else count.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// Quiet label while the backend can't be reached: the screen keeps showing what was cached
@Composable
fun OfflineBadge(visible: Boolean, modifier: Modifier = Modifier, text: String = "Đang offline · hiện dữ liệu đã lưu") {
    if (!visible) return
    Row(
        modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// "2026-10-18T11:40:37+07:00" -> that moment in this phone's time zone. Zone-less text (old cache) = phone time
fun parseServerTime(text: String?, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime? {
    if (text == null) return null
    return runCatching { OffsetDateTime.parse(text).atZoneSameInstant(zone).toLocalDateTime() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(text) }.getOrNull()
}

private fun String?.toDateTime(): LocalDateTime? = parseServerTime(this)

// "2026-10-18T11:40:37+07:00" -> "vừa xong" / "5 phút" / "3 giờ" / "2 ngày" / "18/10"
fun String?.timeAgo(): String {
    val time = toDateTime() ?: return ""
    val minutes = Duration.between(time, LocalDateTime.now()).toMinutes()
    return when {
        minutes < 1 -> "vừa xong"
        minutes < 60 -> "$minutes phút"
        minutes < 60 * 24 -> "${minutes / 60} giờ"
        minutes < 60 * 24 * 7 -> "${minutes / (60 * 24)} ngày"
        else -> time.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
}

// "2026-10-18T11:40:37.91" -> "11:40"
fun String?.clockTime(): String = toDateTime()?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()
