/*
 * TỔNG QUAN FILE: Màn hồ sơ: đổi ảnh đại diện và tên, cài đặt thông báo, đăng xuất, xoá tài khoản.
 *
 * Thành phần chính:
 * - ProfileScreen: chọn ảnh bằng PickVisualMedia, ô sửa tên, các nút hành động, hộp thoại xác nhận xoá tài
 *   khoản.
 * - NotificationSetting: công tắc thông báo; nếu hệ thống đang chặn thông báo thì mở trang cài đặt của
 *   Android.
 * - ChangePasswordDialog: chỉ hiện với tài khoản email + mật khẩu (UserDto.hasPassword); tự đóng khi đổi xong.
 *
 * Lưu ý:
 * - Ô tên được khởi tạo lại khi hồ sơ từ server về.
 * - Màu thumb của Switch được chỉnh vì màu mặc định gần như vô hình trên nền tối.
 * - Đăng xuất hoặc xoá tài khoản xong, app tự quay về Login qua isLoggedIn.
 */
package com.example.loki.ui.profile

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.loki.ui.components.Avatar
import com.example.loki.ui.components.LokiTopBar

@Composable
fun ProfileScreen(onBack: () -> Unit, viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // Re-initialised once the profile arrives from the server
    var displayName by rememberSaveable(uiState.user?.id) { mutableStateOf(uiState.user?.displayName.orEmpty()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var changingPassword by rememberSaveable { mutableStateOf(false) }
    // Changed: close the dialog
    LaunchedEffect(uiState.passwordChangedCount) { if (uiState.passwordChangedCount > 0) changingPassword = false }
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::changeAvatar)
    }

    Scaffold(topBar = { LokiTopBar("Hồ sơ", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Tap the avatar to change it
            val pick = { pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            Box {
                Avatar(
                    uiState.user,
                    size = 128.dp,
                    ringColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(CircleShape).clickable(enabled = !uiState.isUploadingAvatar, onClick = pick),
                )
                Box(
                    modifier = Modifier.align(Alignment.BottomEnd).size(38.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.isUploadingAvatar) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Icon(Icons.Rounded.PhotoCamera, "Đổi ảnh đại diện", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Text(uiState.user?.name.orEmpty(), style = MaterialTheme.typography.headlineMedium)
            Text(uiState.user?.email.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Tên hiển thị") },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { viewModel.save(displayName) },
                enabled = uiState.user != null && displayName.isNotBlank() && displayName.trim() != uiState.user?.displayName && !uiState.isSaving,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Lưu") }
            uiState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

            Spacer(Modifier.height(8.dp))
            NotificationSetting(
                enabled = notificationsEnabled,
                onChange = { on ->
                    viewModel.setNotificationsEnabled(on)
                    // Turned on here but blocked in Android settings: send the user there, or nothing would show
                    if (on && !NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                },
            )

            Spacer(Modifier.height(24.dp))
            if (uiState.user?.hasPassword == true) {
                TextButton(onClick = { changingPassword = true }, modifier = Modifier.fillMaxWidth()) { Text("Đổi mật khẩu") }
            }
            TextButton(onClick = viewModel::logout, modifier = Modifier.fillMaxWidth()) { Text("Đăng xuất") }
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Xoá tài khoản", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (changingPassword) {
        ChangePasswordDialog(
            isSaving = uiState.isChangingPassword,
            message = uiState.message,
            onSave = viewModel::changePassword,
            onDismiss = { changingPassword = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xoá tài khoản?") },
            text = { Text("Thông tin cá nhân sẽ bị xoá và bạn sẽ bị gỡ khỏi danh sách bạn bè của mọi người. Không thể hoàn tác.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteAccount() }) {
                    Text("Xoá vĩnh viễn", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } },
        )
    }
}

@Composable
private fun ChangePasswordDialog(isSaving: Boolean, message: String?, onSave: (current: String, new: String) -> Unit, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Mật khẩu hiện tại") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedTextField(
                    value = new,
                    onValueChange = { new = it },
                    label = { Text("Mật khẩu mới (tối thiểu $MIN_PASSWORD_LENGTH ký tự)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(current, new) }, enabled = !isSaving && current.isNotEmpty() && new.isNotEmpty()) {
                Text("Lưu")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
    )
}

@Composable
private fun NotificationSetting(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable { onChange(!enabled) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            if (enabled) Icons.Rounded.Notifications else Icons.Rounded.NotificationsOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f)) {
            Text("Thông báo", style = MaterialTheme.typography.titleSmall)
            Text(
                if (enabled) "Báo khi có ảnh, cảm xúc và tin nhắn mới" else "Đã tắt. Widget vẫn tự cập nhật ảnh mới",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            // The default "off" thumb uses the outline colour, which vanishes on our dark surfaces
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            ),
        )
    }
}
