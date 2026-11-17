/*
 * TỔNG QUAN FILE: Bottom sheet quản lý bạn bè: thêm bằng email hoặc link mời, trả lời lời mời, xem và huỷ kết
 * bạn.
 *
 * Thành phần chính:
 * - FriendsSheet: ModalBottomSheet chứa LazyColumn các mục; bộ đếm "x/20"; chia sẻ link mời qua ACTION_SEND;
 *   mỗi bạn có menu Chặn / Báo cáo / Xoá bạn; mục "Đã chặn" để bỏ chặn.
 * - SearchField: ô tìm theo email. PersonRow: một dòng người kèm các nút hành động. SectionTitle: tiêu đề
 *   nhóm.
 *
 * Lưu ý:
 * - Link mời dùng scheme loki://, không bấm được trong mọi ứng dụng chat. Khi có domain nên chuyển sang App
 *   Link https.
 * - State nằm trong FriendsViewModel, được chia sẻ với Home (bộ đếm, bộ lọc, người nhận).
 */
package com.example.loki.ui.friends

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.loki.data.remote.dto.FriendshipDto
import com.example.loki.data.remote.dto.MAX_FRIENDS
import com.example.loki.data.remote.dto.UserDto
import com.example.loki.ui.components.Avatar
import com.example.loki.ui.components.RoundIconButton
import com.example.loki.ui.navigation.inviteLink

// Locket's friends sheet: add by email or invite link, answer requests, manage friends
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsSheet(
    uiState: FriendsUiState,
    myId: Long?,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
    onAdd: (UserDto) -> Unit,
    onClearFound: () -> Unit,
    onAccept: (FriendshipDto) -> Unit,
    onDecline: (FriendshipDto) -> Unit,
    onUnfriend: (UserDto) -> Unit,
    onChat: (UserDto) -> Unit,
    onBlock: (UserDto) -> Unit,
    onUnblock: (UserDto) -> Unit,
    onReport: (UserDto, reason: String) -> Unit,
) {
    val context = LocalContext.current
    var email by rememberSaveable { mutableStateOf("") }
    var confirmRemove by remember { mutableStateOf<UserDto?>(null) }
    var confirmBlock by remember { mutableStateOf<UserDto?>(null) }
    var reporting by remember { mutableStateOf<UserDto?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${uiState.friends.size}/$MAX_FRIENDS người bạn", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Bạn bè sẽ thấy ảnh của bạn trên màn hình chính",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SearchField(
                    value = email,
                    onValueChange = { email = it },
                    onSearch = { if (email.isNotBlank()) onSearch(email) },
                )
            }

            uiState.found?.let { user ->
                item {
                    PersonRow(user, subtitle = user.email) {
                        Button(onClick = { onAdd(user) }) { Text("Thêm") }
                        RoundIconButton(Icons.Rounded.Close, "Bỏ qua", onClearFound, size = 36.dp)
                    }
                }
            }
            uiState.message?.let {
                item { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
            }

            if (myId != null) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Rounded.IosShare, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Mời bạn bè qua link", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Kết bạn với mình trên Loki nhé: ${inviteLink(myId)}")
                            }
                            context.startActivity(Intent.createChooser(send, "Mời bạn bè"))
                        }) { Text("Chia sẻ") }
                    }
                }
            }

            if (uiState.pending.isNotEmpty()) {
                item { SectionTitle("Lời mời kết bạn") }
                items(uiState.pending, key = { "request-${it.id}" }) { request ->
                    PersonRow(request.requester, subtitle = "muốn kết bạn với bạn") {
                        Button(onClick = { onAccept(request) }) { Text("Chấp nhận") }
                        RoundIconButton(Icons.Rounded.Close, "Từ chối", { onDecline(request) }, size = 36.dp)
                    }
                }
            }

            item { SectionTitle("Bạn bè của bạn") }
            if (uiState.friends.isEmpty()) {
                item {
                    Text(
                        "Chưa có ai. Tìm bằng email hoặc gửi link mời ở trên nhé!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(uiState.friends, key = { "friend-${it.id}" }) { friend ->
                PersonRow(friend, subtitle = null) {
                    RoundIconButton(Icons.AutoMirrored.Rounded.Chat, "Nhắn tin", { onChat(friend) }, size = 36.dp)
                    Box {
                        var menuOpen by remember { mutableStateOf(false) }
                        RoundIconButton(Icons.Rounded.MoreVert, "Thêm", { menuOpen = true }, size = 36.dp)
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text("Xoá bạn") }, onClick = { menuOpen = false; confirmRemove = friend })
                            DropdownMenuItem(text = { Text("Báo cáo") }, onClick = { menuOpen = false; reporting = friend })
                            DropdownMenuItem(
                                text = { Text("Chặn", color = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; confirmBlock = friend },
                            )
                        }
                    }
                }
            }

            if (uiState.blocked.isNotEmpty()) {
                item { SectionTitle("Đã chặn") }
                items(uiState.blocked, key = { "blocked-${it.id}" }) { user ->
                    PersonRow(user, subtitle = "Không thấy và không liên lạc được với bạn") {
                        TextButton(onClick = { onUnblock(user) }) { Text("Bỏ chặn") }
                    }
                }
            }
        }
    }

    confirmRemove?.let { friend ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Xoá ${friend.name}?") },
            text = { Text("Hai bạn sẽ không còn thấy ảnh của nhau nữa.") },
            confirmButton = {
                TextButton(onClick = { onUnfriend(friend); confirmRemove = null }) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Huỷ") } },
        )
    }
    confirmBlock?.let { user ->
        BlockDialog(user, onConfirm = { onBlock(user); confirmBlock = null }, onDismiss = { confirmBlock = null })
    }
    reporting?.let { user ->
        ReportDialog(user, isPhoto = false, onSend = { reason -> onReport(user, reason); reporting = null }, onDismiss = { reporting = null })
    }
}

@Composable
private fun SearchField(value: String, onValueChange: (String) -> Unit, onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text("Thêm bạn bằng email", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            },
        )
        TextButton(onClick = onSearch, enabled = value.isNotBlank()) { Text("Tìm") }
    }
}

@Composable
private fun PersonRow(user: UserDto, subtitle: String?, actions: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Avatar(user, size = 48.dp)
        Column(Modifier.weight(1f)) {
            Text(user.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}
