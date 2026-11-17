/*
 * TỔNG QUAN FILE: Bảng "Hoạt động" của một ảnh mình đã gửi: từng người nhận đã xem chưa, lúc nào, thả emoji gì.
 *
 * Thành phần chính:
 * - ActivitySheet: ModalBottomSheet; dòng trên cùng đếm số người đã xem trên tổng số người nhận.
 * - ActivityRow: avatar, tên, "Đã xem 5 phút" hoặc "Chưa xem", emoji bên phải.
 *
 * Lưu ý:
 * - Dữ liệu lấy từ FeedViewModel.showActivity (backend GET api/photos/{id}/activity), không cache: cần có mạng.
 * - Backend xếp người xem gần nhất lên trước, người chưa xem ở cuối.
 */
package com.example.loki.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loki.data.remote.dto.PhotoActivityDto
import com.example.loki.ui.components.Avatar
import com.example.loki.ui.components.timeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitySheet(activity: List<PhotoActivityDto>, isLoading: Boolean, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                val seen = activity.count { it.seenAt != null }
                Text(
                    if (isLoading) "Hoạt động" else "Đã xem $seen/${activity.size}",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            if (isLoading) {
                item { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (activity.isEmpty()) {
                item { Text("Chưa có ai nhận ảnh này", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(activity, key = { it.user.id }) { ActivityRow(it) }
        }
    }
}

@Composable
private fun ActivityRow(item: PhotoActivityDto) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Avatar(item.user, size = 44.dp)
        Column(Modifier.weight(1f)) {
            Text(item.user.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (item.seenAt != null) "Đã xem ${item.seenAt.timeAgo()}" else "Chưa xem",
                style = MaterialTheme.typography.bodySmall,
                color = if (item.seenAt != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item.emoji?.let { Text(it, fontSize = 24.sp, modifier = Modifier.padding(start = 8.dp)) }
    }
}
