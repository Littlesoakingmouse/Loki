/*
 * TỔNG QUAN FILE: Màn trò chuyện với một người.
 *
 * Thành phần chính:
 * - ChatScreen: polling tin nhắn khi màn hình đang hiện, LazyColumn đảo ngược (tin mới nhất ở dưới cùng), ô
 *   nhập tin.
 * - MessageBubble: bong bóng tin nhắn, có ảnh đính kèm nếu là trả lời ảnh.
 * - PendingBubble: tin gửi lúc offline, hiện trạng thái đang chờ.
 * - MessageInput: ô nhập và nút gửi.
 *
 * Lưu ý:
 * - repeatOnLifecycle(RESUMED) dừng việc tải tin (push + polling 15 giây) khi app xuống nền và chạy lại khi quay
 *   về. Mở màn này thì gỡ thông báo tin nhắn của người đó.
 * - Tin đang chờ luôn mới hơn mọi tin trên server nên được đặt ở cuối danh sách.
 * - Tham số của cuộc trò chuyện được ChatViewModel đọc từ route, màn hình không nhận trực tiếp.
 */
package com.example.loki.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.example.loki.data.local.db.entity.PendingMessage
import com.example.loki.core.util.thumbnail
import com.example.loki.data.remote.dto.MessageDto
import com.example.loki.push.cancelChatNotification
import com.example.loki.ui.components.LokiTopBar
import com.example.loki.ui.components.OfflineBadge
import com.example.loki.ui.components.clockTime

@Composable
fun ChatScreen(onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    // Load only while the screen is visible; stops when the app goes to the background
    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            cancelChatNotification(context, viewModel.otherUserId) // its messages are on screen now
            viewModel.pollMessages()
        }
    }

    Scaffold(
        topBar = { LokiTopBar(viewModel.title, onBack = onBack) },
        bottomBar = {
            MessageInput(
                value = uiState.draft,
                onValueChange = viewModel::onDraftChange,
                canSend = uiState.draft.isNotBlank() && !uiState.isSending,
                onSend = viewModel::send,
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OfflineBadge(visible = uiState.isOffline, modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp))
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp)) }
            // reverseLayout: the newest message (first in the list) sits at the bottom
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(12.dp),
            ) {
                // Queued offline: newer than everything the backend has, so they sit at the very bottom
                itemsIndexed(uiState.pending, key = { _, p -> "pending-${p.id}" }) { index, pending ->
                    PendingBubble(pending, showStatus = index == 0)
                }
                itemsIndexed(uiState.messages, key = { _, m -> m.id }) { index, message ->
                    val isMine = message.sender.id == uiState.myId
                    // Show the time under the last bubble of a run from the same person
                    val showTime = index == 0 || uiState.messages[index - 1].sender.id != message.sender.id
                    MessageBubble(message, isMine = isMine, showTime = showTime)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto, isMine: Boolean, showTime: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // A reply to one of the photos: the photo sits above the text, like Locket
        message.photo?.let { photo ->
            Text(
                if (isMine) "Bạn đã trả lời một ảnh" else "Đã trả lời ảnh của bạn",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AsyncImage(
                model = thumbnail(photo.imageUrl, 320),
                contentDescription = photo.caption,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(160.dp).clip(RoundedCornerShape(28.dp)),
            )
        }
        Box(
            modifier = Modifier.widthIn(max = 280.dp).clip(RoundedCornerShape(22.dp))
                .background(if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                message.content.orEmpty(),
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showTime) {
            Text(
                message.createdAt.clockTime(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun PendingBubble(message: PendingMessage, showStatus: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier.widthIn(max = 280.dp).alpha(0.6f).clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(message.content, color = MaterialTheme.colorScheme.onPrimary)
        }
        if (showStatus) {
            Text(
                "Chờ có mạng để gửi",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MessageInput(value: String, onValueChange: (String) -> Unit, canSend: Boolean, onSend: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            maxLines = 4,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(
                    Modifier.clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                ) {
                    if (value.isEmpty()) Text("Nhắn tin...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape)
                .background(if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.Send,
                contentDescription = "Gửi",
                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
