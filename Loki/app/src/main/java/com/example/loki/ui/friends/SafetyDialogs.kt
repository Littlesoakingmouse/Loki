/*
 * TỔNG QUAN FILE: Hộp thoại an toàn dùng chung: xác nhận chặn và báo cáo vi phạm.
 *
 * Thành phần chính:
 * - BlockDialog: giải thích hậu quả của việc chặn rồi hỏi xác nhận.
 * - ReportDialog: chọn nhanh lý do hoặc gõ lý do; gửi kèm ảnh nếu báo cáo từ một ảnh.
 *
 * Lưu ý:
 * - Dùng ở cả FriendsSheet (từ danh sách bạn) và HomeScreen (từ menu của một ảnh).
 * - Lý do là tuỳ chọn; backend giới hạn 500 ký tự.
 */
package com.example.loki.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.loki.data.remote.dto.UserDto

private const val MAX_REASON = 500
private val QUICK_REASONS = listOf("Nội dung không phù hợp", "Quấy rối", "Giả mạo người khác", "Spam")

@Composable
fun BlockDialog(user: UserDto, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chặn ${user.name}?") },
        text = {
            Text(
                "Hai bạn sẽ không còn là bạn bè. ${user.name} không tìm thấy bạn, không gửi được ảnh, tin nhắn hay lời mời " +
                    "cho bạn nữa. Ảnh cũ của người này cũng biến mất khỏi lịch sử của bạn. Bạn có thể bỏ chặn trong mục Bạn bè.",
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Chặn", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}

// photo = true when reporting one photo rather than the person
@Composable
fun ReportDialog(user: UserDto, isPhoto: Boolean, onSend: (reason: String) -> Unit, onDismiss: () -> Unit) {
    var reason by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPhoto) "Báo cáo ảnh này?" else "Báo cáo ${user.name}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Loki sẽ xem xét báo cáo. ${user.name} không biết ai đã báo cáo.")
                QUICK_REASONS.forEach { quick ->
                    FilterChip(selected = reason == quick, onClick = { reason = quick }, label = { Text(quick) })
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it.take(MAX_REASON) },
                    label = { Text("Lý do (không bắt buộc)") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSend(reason) }) { Text("Gửi báo cáo", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}
