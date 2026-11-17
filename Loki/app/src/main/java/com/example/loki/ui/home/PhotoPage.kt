/*
 * TỔNG QUAN FILE: Một trang ảnh trong lịch sử: ảnh vuông, caption, người gửi và thời gian, thanh trả lời hoặc
 * hoạt động của mình.
 *
 * Thành phần chính:
 * - QUICK_REACTIONS: 4 emoji thả nhanh.
 * - PhotoPage: bố cục một ảnh.
 * - ReplyBar: ô "Gửi tin nhắn..." kèm emoji, chỉ hiện trên ảnh của người khác; nút "+" thả emoji bất kỳ.
 * - EmojiDialog: gõ hoặc dán một emoji; kiểm tra bằng asReactionEmoji.
 * - MyActivity: trên ảnh của mình, hiện ai đã thả cảm xúc; bấm vào để mở ActivitySheet (ai đã xem).
 *
 * Lưu ý:
 * - Trả lời ảnh tạo tin nhắn trong cuộc trò chuyện với người gửi ảnh, kèm photoId.
 */
package com.example.loki.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.loki.core.util.asReactionEmoji
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.data.remote.dto.ReactionDto
import com.example.loki.ui.camera.PhotoShape
import com.example.loki.ui.components.Avatar
import com.example.loki.ui.components.timeAgo

val QUICK_REACTIONS = listOf("💛", "😂", "😍", "🔥")

// One photo of the history: square photo, caption on it, sender + time below, reply bar (or my activity) at the bottom
@Composable
fun PhotoPage(
    photo: PhotoDto,
    myId: Long?,
    onReact: (String) -> Unit,
    onReply: (String) -> Unit,
    onShowActivity: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMine = photo.sender.id == myId
    Column(
        modifier = modifier.fillMaxSize().imePadding().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(100.dp)) // room for Home's top bar

        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(PhotoShape).background(MaterialTheme.colorScheme.surfaceContainer)) {
            AsyncImage(
                model = photo.imageUrl,
                contentDescription = photo.caption ?: "Ảnh của ${photo.sender.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            photo.caption?.let {
                Text(
                    it,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Avatar(photo.sender, size = 28.dp)
            Text(if (isMine) "Bạn" else photo.sender.name, style = MaterialTheme.typography.titleSmall)
            Text(photo.createdAt.timeAgo(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.weight(1f))
        if (isMine) {
            MyActivity(photo.reactions, onClick = onShowActivity)
        } else {
            ReplyBar(
                senderName = photo.sender.name,
                myEmoji = photo.reactions.find { it.user.id == myId }?.emoji,
                onReact = onReact,
                onReply = onReply,
            )
        }
        Spacer(Modifier.height(116.dp)) // room for Home's bottom bar
    }
}

// Locket's "Send message..." pill, with quick emoji reactions on the right
@Composable
private fun ReplyBar(senderName: String, myEmoji: String?, onReact: (String) -> Unit, onReply: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var pickEmoji by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val send = {
        onReply(text)
        text = ""
        focus.clearFocus()
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 20.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) send() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            "Trả lời $senderName...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    inner()
                }
            },
        )
        if (text.isBlank()) {
            QUICK_REACTIONS.forEach { emoji ->
                Box(
                    modifier = Modifier.size(42.dp).clip(CircleShape)
                        .background(if (emoji == myEmoji) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                        .clickable { onReact(emoji) },
                    contentAlignment = Alignment.Center,
                ) { Text(emoji, fontSize = 22.sp) }
            }
            // Any other emoji; the one I picked shows here when it isn't a quick one
            val custom = myEmoji?.takeIf { it !in QUICK_REACTIONS }
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .background(if (custom != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else Color.Transparent)
                    .clickable { pickEmoji = true },
                contentAlignment = Alignment.Center,
            ) {
                if (custom != null) Text(custom, fontSize = 22.sp)
                else Icon(Icons.Rounded.AddReaction, contentDescription = "Thả emoji khác", modifier = Modifier.size(22.dp))
            }
        } else {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).clickable(onClick = send),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Gửi", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }
    }
    if (pickEmoji) {
        EmojiDialog(onPick = { emoji -> pickEmoji = false; onReact(emoji) }, onDismiss = { pickEmoji = false })
    }
}

// Type or paste one emoji (the keyboard's emoji tab works)
@Composable
private fun EmojiDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val emoji = asReactionEmoji(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thả emoji") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(fontSize = 28.sp),
                placeholder = { Text("😎") },
                supportingText = { if (text.isNotEmpty() && emoji == null) Text("Chỉ một emoji thôi nhé") },
            )
        },
        confirmButton = { TextButton(onClick = { emoji?.let(onPick) }, enabled = emoji != null) { Text("Thả") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}

// On my own photos: who reacted, instead of a reply bar. Tap it for who has seen the photo
@Composable
private fun MyActivity(reactions: List<ReactionDto>, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClickLabel = "Xem ai đã xem ảnh", onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(
                if (reactions.isEmpty()) "Chưa có hoạt động nào" else "${reactions.size} hoạt động",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (reactions.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(reactions, key = { it.id }) { reaction ->
                    Box {
                        Avatar(reaction.user, size = 40.dp)
                        Text(reaction.emoji, fontSize = 16.sp, modifier = Modifier.align(Alignment.BottomEnd))
                    }
                }
            }
        }
    }
}
