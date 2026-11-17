/*
 * TỔNG QUAN FILE: Trang 0 của Home: khung camera vuông, chụp ảnh, nhập caption, chọn người nhận và gửi.
 *
 * Thành phần chính:
 * - PhotoShape: hình bo góc dùng chung cho mọi ảnh. MAX_CAPTION: caption tối đa 40 ký tự.
 * - CameraPage: chuyển giữa chế độ xem trước và chế độ gửi; Back huỷ ảnh vừa chụp; gửi xong hiện dấu "đã gửi".
 *   Nút thư viện (góc dưới khung camera) chọn ảnh có sẵn bằng PickVisualMedia, không cần quyền đọc bộ nhớ.
 * - CameraState + rememberCameraState: state holder gom quyền camera, camera trước/sau, flash, trạng thái
 *   chụp; gắn và gỡ CameraX theo lifecycle.
 * - Viewfinder: PreviewView của CameraX nhúng qua AndroidView, hoặc nút xin quyền khi chưa có.
 * - ShutterRow, ShutterButton, CaptionField, SendControls, RecipientPicker, RecipientItem, SentBadge: các phần
 *   giao diện.
 *
 * Lưu ý:
 * - Chỉ giữ camera khi trang đang hiển thị (isActive); DisposableEffect nhả camera khi vuốt đi, tiết kiệm pin.
 * - PreviewView dùng chế độ COMPATIBLE (TextureView) để bo góc và nằm trong Pager được.
 * - Ảnh selfie được lưu lật ngang cho giống bản xem trước.
 * - Chặn bấm chụp liên tục bằng isCapturing.
 * - Logic gửi nằm trong SendPhotoViewModel; file này chỉ lo giao diện và camera.
 */
package com.example.loki.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.loki.core.util.newCaptureFile
import com.example.loki.data.remote.dto.UserDto
import com.example.loki.ui.components.Avatar
import com.example.loki.ui.components.OfflineBadge
import com.example.loki.ui.components.RoundIconButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Photos are square with big rounded corners everywhere: viewfinder, history, grid
val PhotoShape = RoundedCornerShape(48.dp)
private const val MAX_CAPTION = 40

// Page 0 of Home: a square live viewfinder, then caption + recipients once a shot is taken
@Composable
fun CameraPage(
    isActive: Boolean,
    friends: List<UserDto>,
    hasHistory: Boolean,
    onOpenHistory: () -> Unit,
    onSent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SendPhotoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val camera = rememberCameraState(isActive = isActive)
    var captured by rememberSaveable { mutableStateOf<Uri?>(null) }
    var caption by rememberSaveable { mutableStateOf("") }
    var showSent by remember { mutableStateOf(false) }
    val discard = { captured = null; caption = "" }
    // A photo from the gallery goes through the same caption + recipients + send flow as a shot
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) captured = uri
    }

    // Back discards the shot instead of leaving the app
    BackHandler(enabled = captured != null, onBack = discard)

    // A successful send: back to the viewfinder with a "sent" check
    LaunchedEffect(uiState.sentCount) {
        if (uiState.sentCount == 0) return@LaunchedEffect
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        discard()
        onSent()
        showSent = true
        delay(1_400)
        showSent = false
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(100.dp)) // room for Home's top bar

        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(PhotoShape).background(MaterialTheme.colorScheme.surfaceContainer)) {
            val photo = captured
            if (photo == null) {
                Viewfinder(camera)
                RoundIconButton(
                    icon = Icons.Rounded.PhotoLibrary,
                    contentDescription = "Chọn ảnh từ thư viện",
                    onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                    background = Color.Black.copy(alpha = 0.45f),
                    tint = Color.White,
                )
            } else {
                AsyncImage(
                    model = photo,
                    contentDescription = "Ảnh vừa chụp",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                CaptionField(
                    value = caption,
                    onValueChange = { caption = it.take(MAX_CAPTION) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                )
            }
            SentBadge(visible = showSent, modifier = Modifier.align(Alignment.Center))
        }

        if (captured == null) {
            ShutterRow(camera, onCaptured = { captured = it })
            Spacer(Modifier.weight(1f))
            OfflineBadge(visible = uiState.pendingCount > 0, text = "${uiState.pendingCount} ảnh sẽ tự gửi khi có mạng")
            Row(
                modifier = Modifier.clip(CircleShape).clickable(enabled = hasHistory, onClick = onOpenHistory)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(if (hasHistory) "Lịch sử" else "Chưa có ảnh nào", style = MaterialTheme.typography.titleSmall)
                if (hasHistory) Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
            }
            Spacer(Modifier.height(28.dp))
        } else {
            SendControls(
                isSending = uiState.isSending,
                onCancel = discard,
                onSend = { captured?.let { viewModel.send(it, caption, friends.map(UserDto::id)) } },
            )
            uiState.error?.let { error ->
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp).clickable(onClick = viewModel::errorShown),
                )
            }
            Spacer(Modifier.weight(1f))
            RecipientPicker(
                friends = friends,
                selectedIds = uiState.selectedIds,
                onEveryone = viewModel::selectEveryone,
                onToggle = viewModel::toggle,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

// Everything the viewfinder and the shutter row share
@Stable
private class CameraState(
    val controller: LifecycleCameraController,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    var hasPermission by mutableStateOf(false)
    var useFront by mutableStateOf(true) // Locket opens on the selfie camera
    var flashOn by mutableStateOf(false)
    var isCapturing by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    val shutterFlash = Animatable(0f)

    fun capture(onCaptured: (Uri) -> Unit) {
        if (isCapturing || !hasPermission) return
        isCapturing = true
        error = null
        scope.launch {
            shutterFlash.snapTo(1f)
            shutterFlash.animateTo(0f, tween(300))
        }
        val file = newCaptureFile(context)
        // Selfies are saved mirrored, the way the preview showed them
        val metadata = ImageCapture.Metadata().apply { isReversedHorizontal = useFront }
        val options = ImageCapture.OutputFileOptions.Builder(file).setMetadata(metadata).build()
        controller.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    onCaptured(Uri.fromFile(file))
                }

                override fun onError(exception: ImageCaptureException) {
                    isCapturing = false
                    error = "Không chụp được ảnh, thử lại nhé"
                }
            },
        )
    }
}

@Composable
private fun rememberCameraState(isActive: Boolean): CameraState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val state = remember { CameraState(LifecycleCameraController(context), scope, context) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { state.hasPermission = it }
    LaunchedEffect(isActive) {
        state.hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (isActive && !state.hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Only hold the camera while this page is on screen
    DisposableEffect(isActive, state.hasPermission, lifecycleOwner) {
        if (isActive && state.hasPermission) state.controller.bindToLifecycle(lifecycleOwner)
        onDispose { state.controller.unbind() }
    }
    LaunchedEffect(state.useFront) {
        val selector = if (state.useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        state.controller.cameraSelector = selector
    }
    LaunchedEffect(state.flashOn) {
        state.controller.imageCaptureFlashMode = if (state.flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }
    return state
}

@Composable
private fun Viewfinder(camera: CameraState) {
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { camera.hasPermission = it }

    Box(Modifier.fillMaxSize()) {
        if (camera.hasPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        // TextureView-backed so Compose can clip it to the rounded square
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        controller = camera.controller // also gives pinch-to-zoom and tap-to-focus
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().alpha(camera.shutterFlash.value).background(Color.White))
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Rounded.NoPhotography, contentDescription = null, modifier = Modifier.size(48.dp))
                Text("Loki cần quyền camera để chụp ảnh", textAlign = TextAlign.Center)
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Cho phép") }
            }
        }
        camera.error?.let {
            Text(
                it,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
                    .clip(CircleShape).background(Color.Black.copy(alpha = 0.6f))
                    .clickable { camera.error = null }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun ShutterRow(camera: CameraState, onCaptured: (Uri) -> Unit) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(
            icon = if (camera.flashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
            contentDescription = if (camera.flashOn) "Tắt flash" else "Bật flash",
            onClick = { camera.flashOn = !camera.flashOn },
            size = 52.dp,
            background = Color.Transparent,
            tint = if (camera.flashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        ShutterButton(
            enabled = camera.hasPermission && !camera.isCapturing,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                camera.capture(onCaptured)
            },
        )
        RoundIconButton(
            icon = Icons.Rounded.Cameraswitch,
            contentDescription = "Đổi camera",
            onClick = { camera.useFront = !camera.useFront },
            size = 52.dp,
            background = Color.Transparent,
        )
    }
}

// The big round shutter: yellow ring, white core that shrinks while pressed
@Composable
fun ShutterButton(enabled: Boolean, onClick: () -> Unit, size: Dp = 84.dp) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier.size(size)
            .border(size * 0.05f, MaterialTheme.colorScheme.primary, CircleShape)
            .padding(size * 0.09f)
            .scale(if (pressed) 0.88f else 1f)
            .clip(CircleShape)
            .background(if (enabled) Color.White else Color.White.copy(alpha = 0.4f))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun CaptionField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.width(260.dp),
        decorationBox = { inner ->
            Box(
                modifier = Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text("Thêm một tin nhắn", color = Color.White.copy(alpha = 0.7f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                inner()
            }
        },
    )
}

@Composable
private fun SendControls(isSending: Boolean, onCancel: () -> Unit, onSend: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(Icons.Rounded.Close, "Huỷ", onCancel, size = 52.dp, background = Color.Transparent)
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
                .clickable(enabled = !isSending, onClick = onSend),
            contentAlignment = Alignment.Center,
        ) {
            if (isSending) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            } else {
                Icon(
                    Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Gửi",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.width(52.dp)) // keeps the send button centered
    }
}

@Composable
private fun RecipientPicker(
    friends: List<UserDto>,
    selectedIds: Set<Long>,
    onEveryone: () -> Unit,
    onToggle: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            RecipientItem(label = "Tất cả", selected = selectedIds.isEmpty(), onClick = onEveryone) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Groups, contentDescription = null) }
            }
        }
        items(friends, key = { it.id }) { friend ->
            RecipientItem(label = friend.name, selected = friend.id in selectedIds, onClick = { onToggle(friend.id) }) {
                Avatar(friend, size = 52.dp)
            }
        }
    }
}

@Composable
private fun RecipientItem(label: String, selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.width(68.dp).clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier.border(2.5.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                .padding(4.dp),
        ) { icon() }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SentBadge(visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(), modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(52.dp))
            }
            Text("Đã gửi!", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
    }
}
