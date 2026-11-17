/*
 * TỔNG QUAN FILE: Màn danh sách cuộc trò chuyện (hộp thư).
 *
 * Thành phần chính:
 * - ChatListScreen: LazyColumn các cuộc trò chuyện, nhãn offline, tải lại mỗi khi màn hình được mở lại.
 * - ConversationRow: avatar, tên, tin cuối, thời gian và số tin chưa đọc.
 *
 * Lưu ý:
 * - Dữ liệu hiện ngay từ cache; LifecycleResumeEffect chỉ làm mới, ví dụ sau khi đọc xong một cuộc trò chuyện.
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loki.data.remote.dto.ConversationDto
import com.example.loki.ui.components.Avatar
import com.example.loki.ui.components.LokiTopBar
import com.example.loki.ui.components.OfflineBadge
import com.example.loki.ui.components.timeAgo

@Composable
fun ChatListScreen(
    onBack: () -> Unit,
    onOpen: (ConversationDto) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Reload whenever the screen comes back, e.g. after reading a chat
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose {}
    }

    Scaffold(topBar = { LokiTopBar("Tin nhắn", onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (uiState.isOffline) {
                item {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) { OfflineBadge(visible = true) }
                }
            }
            uiState.error?.let {
                item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
            }
            if (uiState.conversations.isEmpty() && uiState.error == null) {
                item {
                    Text(
                        "Chưa có tin nhắn nào.\nTrả lời một bức ảnh để bắt đầu trò chuyện!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            items(uiState.conversations, key = { it.id }) { conversation ->
                ConversationRow(conversation, onClick = { onOpen(conversation) })
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: ConversationDto, onClick: () -> Unit) {
    val unread = conversation.unreadCount > 0
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Avatar(conversation.otherUser, size = 56.dp, ringColor = if (unread) MaterialTheme.colorScheme.primary else null)
        Column(Modifier.weight(1f)) {
            Text(conversation.otherUser.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    conversation.lastMessageContent.orEmpty(),
                    color = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text("· ${conversation.lastMessageAt.timeAgo()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (unread) Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
    }
}
