/*
 * TỔNG QUAN FILE: Màn chính: VerticalPager với trang 0 là camera và các trang sau là từng ảnh trong lịch sử.
 *
 * Thành phần chính:
 * - HomeScreen: dựng Pager, TopBar, thanh dưới, lưới ảnh, bảng bạn bè; xử lý deep link; tải trước 2 ảnh kế
 *   tiếp; đánh dấu đã xem và phân trang theo ảnh đang hiện; hiện snackbar.
 * - filterLabel: chữ trên nút lọc ("Mọi người", "Của tôi", tên bạn).
 * - TopBar: avatar, nút bạn bè hoặc bộ lọc, nút chat có số chưa đọc, DropdownMenu chọn bộ lọc.
 * - HistoryBottomBar: mở lưới, quay về camera, menu của ảnh: lưu về máy, chia sẻ, xoá (ảnh của mình) hoặc
 *   nhắn tin, báo cáo, chặn (ảnh người khác).
 * - ActivitySheet (file riêng): ai đã xem ảnh của mình; mở khi bấm "hoạt động" trên ảnh.
 * - AskNotificationPermission: xin quyền thông báo trên Android 13 trở lên.
 *
 * Lưu ý:
 * - key của Pager là id ảnh để ảnh mới chèn lên đầu không làm hiện nhầm trang.
 * - Back khi đang xem ảnh thì quay về camera, khi đang mở lưới thì đóng lưới.
 * - LifecycleResumeEffect làm mới hồ sơ, bạn bè và ảnh mới mỗi khi quay lại màn hình.
 * - Deep link chat được LokiNavHost xử lý; ảnh và link mời được xử lý ở đây.
 * - Lưu ảnh trên Android 8-9 cần quyền WRITE_EXTERNAL_STORAGE nên xin quyền trước; Android 10+ không cần.
 * - Chặn từ menu ảnh: FriendsViewModel chặn xong thì gọi FeedViewModel.refresh để ảnh của người đó biến mất.
 * - Thông báo của FriendsViewModel hiện trong sheet khi sheet mở, ngược lại hiện bằng snackbar.
 */
package com.example.loki.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.imageLoader
import coil3.request.ImageRequest
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.data.remote.dto.UserDto
import com.example.loki.ui.camera.CameraPage
import com.example.loki.ui.camera.ShutterButton
import com.example.loki.ui.components.Avatar
import com.example.loki.ui.components.CountBadge
import com.example.loki.ui.components.OfflineBadge
import com.example.loki.ui.components.Pill
import com.example.loki.ui.components.RoundIconButton
import com.example.loki.ui.friends.BlockDialog
import com.example.loki.ui.friends.FriendsSheet
import com.example.loki.ui.friends.ReportDialog
import com.example.loki.ui.friends.FriendsViewModel
import com.example.loki.ui.navigation.DeepLink
import kotlinx.coroutines.launch

// Locket's home: page 0 is the camera, swiping up walks through the photo history one by one
@Composable
fun HomeScreen(
    deepLink: DeepLink?,
    onDeepLinkHandled: () -> Unit,
    onProfile: () -> Unit,
    onChats: () -> Unit,
    onChat: (UserDto) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
    feedViewModel: FeedViewModel = hiltViewModel(),
    friendsViewModel: FriendsViewModel = hiltViewModel(),
) {
    val home by homeViewModel.uiState.collectAsStateWithLifecycle()
    val feed by feedViewModel.uiState.collectAsStateWithLifecycle()
    val friends by friendsViewModel.uiState.collectAsStateWithLifecycle()
    val photos = feed.photos

    val pager = rememberPagerState { 1 + photos.size }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    var showFriends by rememberSaveable { mutableStateOf(false) }
    var showGrid by rememberSaveable { mutableStateOf(false) }
    var openPhotoId by remember { mutableStateOf<Long?>(null) }
    var burstEmoji by remember { mutableStateOf<String?>(null) }
    var burstKey by remember { mutableIntStateOf(0) }

    val onCamera = pager.currentPage == 0
    val currentPhoto: PhotoDto? = photos.getOrNull(pager.currentPage - 1)
    val goToCamera: () -> Unit = { scope.launch { pager.animateScrollToPage(0) } }
    val context = LocalContext.current

    // Android 8-9 need the storage permission to put a photo in the gallery
    var photoToSave by remember { mutableStateOf<PhotoDto?>(null) }
    val storagePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) photoToSave?.let(feedViewModel::saveToGallery)
        photoToSave = null
    }
    val savePhoto: (PhotoDto) -> Unit = { photo ->
        val needsPermission = Build.VERSION.SDK_INT < 29 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            photoToSave = photo
            storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            feedViewModel.saveToGallery(photo)
        }
    }

    // The share chooser is ready (the photo had to be downloaded first)
    LaunchedEffect(feed.share) {
        feed.share?.let {
            context.startActivity(it)
            feedViewModel.shareHandled()
        }
    }
    // Friends' messages (blocked, reported) show in the sheet when it's open, as a snackbar otherwise
    LaunchedEffect(friends.message, showFriends) {
        val message = friends.message
        if (message != null && !showFriends) {
            snackbar.showSnackbar(message)
            friendsViewModel.messageShown()
        }
    }

    AskNotificationPermission()

    // Coming back from Profile/Chat or the background: refresh avatar, badges, friends and new photos
    LifecycleResumeEffect(Unit) {
        homeViewModel.reload()
        friendsViewModel.reload()
        feedViewModel.checkForNew()
        onPauseOrDispose {}
    }

    // Notification / widget / invite link
    LaunchedEffect(deepLink) {
        when (deepLink) {
            is DeepLink.Photo -> {
                showGrid = false
                feedViewModel.setFilter(null)
                feedViewModel.refresh()
                openPhotoId = deepLink.photoId ?: -1L
            }
            is DeepLink.AddFriend -> {
                friendsViewModel.openInvite(deepLink.userId)
                showFriends = true
            }
            else -> return@LaunchedEffect
        }
        onDeepLinkHandled()
    }
    // Jump to the opened photo once the refreshed history contains it (or to the newest one)
    LaunchedEffect(openPhotoId, photos, feed.isRefreshing) {
        val id = openPhotoId ?: return@LaunchedEffect
        if (photos.isEmpty() || feed.isRefreshing) return@LaunchedEffect
        val index = photos.indexOfFirst { it.id == id }
        pager.scrollToPage(if (index >= 0) index + 1 else 1)
        openPhotoId = null
    }

    // Download the next two photos while this one is looked at, so swiping up never waits on the network
    LaunchedEffect(pager.settledPage, photos) {
        photos.drop(pager.settledPage).take(2).forEach { photo ->
            context.imageLoader.enqueue(ImageRequest.Builder(context).data(photo.imageUrl).build())
        }
    }

    // Seen receipts and paging follow the photo on screen
    LaunchedEffect(pager.settledPage, photos.size) {
        val page = pager.settledPage
        photos.getOrNull(page - 1)?.let(feedViewModel::markSeen)
        if (page > 0 && page >= photos.size - 3) feedViewModel.loadMore()
    }

    LaunchedEffect(feed.message) {
        feed.message?.let {
            snackbar.showSnackbar(it)
            feedViewModel.messageShown()
        }
    }

    // Back from a photo returns to the camera, like Locket
    BackHandler(enabled = !onCamera && !showGrid) { goToCamera() }
    BackHandler(enabled = showGrid) { showGrid = false }

    Box(Modifier.fillMaxSize()) {
        VerticalPager(
            state = pager,
            key = { page -> if (page == 0) "camera" else photos.getOrNull(page - 1)?.id ?: page },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            if (page == 0) {
                CameraPage(
                    isActive = pager.currentPage == 0 && !showGrid,
                    friends = friends.friends,
                    hasHistory = photos.isNotEmpty(),
                    onOpenHistory = { scope.launch { pager.animateScrollToPage(1) } },
                    onSent = feedViewModel::refresh,
                    modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
                )
            } else {
                photos.getOrNull(page - 1)?.let { photo ->
                    PhotoPage(
                        photo = photo,
                        myId = feed.myId,
                        onReact = { emoji ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val removing = photo.reactions.any { it.user.id == feed.myId && it.emoji == emoji }
                            if (!removing) {
                                burstEmoji = emoji
                                burstKey++
                            }
                            feedViewModel.toggleReaction(photo, emoji)
                        },
                        onReply = { feedViewModel.reply(photo, it) },
                        onShowActivity = { feedViewModel.showActivity(photo) },
                        modifier = Modifier.statusBarsPadding().navigationBarsPadding(),
                    )
                }
            }
        }

        EmojiBurst(emoji = burstEmoji, key = burstKey)

        // Just under the top bar; the cached history stays usable underneath
        OfflineBadge(
            visible = feed.isOffline,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp),
        )

        TopBar(
            me = home.me,
            onCamera = onCamera,
            friendCount = friends.friends.size,
            pendingCount = friends.pending.size,
            unreadChats = home.unreadChats,
            filterLabel = filterLabel(feed.friendFilter, feed.myId, friends.friends),
            filterOptions = friends.friends,
            onFilter = { id ->
                feedViewModel.setFilter(id)
                scope.launch { pager.scrollToPage(1) }
            },
            myId = feed.myId,
            onProfile = onProfile,
            onFriends = { showFriends = true },
            onChats = onChats,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
        )

        AnimatedVisibility(
            visible = !onCamera,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        ) {
            HistoryBottomBar(
                photo = currentPhoto,
                myId = feed.myId,
                onGrid = { showGrid = true },
                onCamera = goToCamera,
                onMessage = { currentPhoto?.let { onChat(it.sender) } },
                onDelete = { currentPhoto?.let(feedViewModel::delete) },
                onSave = { currentPhoto?.let(savePhoto) },
                onShare = { currentPhoto?.let(feedViewModel::share) },
                onReport = { reason -> currentPhoto?.let { friendsViewModel.report(it.sender, it.id, reason) } },
                onBlock = { currentPhoto?.let { friendsViewModel.block(it.sender, onDone = feedViewModel::refresh) } },
            )
        }

        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 120.dp))

        AnimatedVisibility(visible = showGrid, enter = fadeIn(), exit = fadeOut()) {
            HistoryGrid(
                title = filterLabel(feed.friendFilter, feed.myId, friends.friends),
                photos = photos,
                onOpen = { index ->
                    showGrid = false
                    scope.launch { pager.scrollToPage(index + 1) }
                },
                onClose = { showGrid = false },
                onLoadMore = feedViewModel::loadMore,
            )
        }
    }

    if (showFriends) {
        FriendsSheet(
            uiState = friends,
            myId = feed.myId,
            onDismiss = {
                showFriends = false
                friendsViewModel.clearFound()
            },
            onSearch = friendsViewModel::search,
            onAdd = friendsViewModel::sendRequest,
            onClearFound = friendsViewModel::clearFound,
            onAccept = friendsViewModel::accept,
            onDecline = friendsViewModel::decline,
            onUnfriend = friendsViewModel::unfriend,
            onChat = { friend ->
                showFriends = false
                onChat(friend)
            },
            onBlock = { user -> friendsViewModel.block(user, onDone = feedViewModel::refresh) },
            onUnblock = friendsViewModel::unblock,
            onReport = { user, reason -> friendsViewModel.report(user, null, reason) },
        )
    }

    feed.activity?.let { activity ->
        ActivitySheet(activity = activity, isLoading = feed.isActivityLoading, onDismiss = feedViewModel::closeActivity)
    }
}

private fun filterLabel(filter: Long?, myId: Long?, friends: List<UserDto>): String = when (filter) {
    null -> "Mọi người"
    myId -> "Của tôi"
    else -> friends.find { it.id == filter }?.name ?: "Bạn bè"
}

@Composable
private fun TopBar(
    me: UserDto?,
    onCamera: Boolean,
    friendCount: Int,
    pendingCount: Int,
    unreadChats: Long,
    filterLabel: String,
    filterOptions: List<UserDto>,
    onFilter: (Long?) -> Unit,
    myId: Long?,
    onProfile: () -> Unit,
    onFriends: () -> Unit,
    onChats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            Avatar(
                me,
                size = 46.dp,
                ringColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(CircleShape).clickable(onClickLabel = "Hồ sơ", onClick = onProfile),
            )
        }

        if (onCamera) {
            Box {
                Pill(onClick = onFriends) {
                    Icon(Icons.Rounded.Groups, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("$friendCount bạn bè", style = MaterialTheme.typography.titleSmall)
                }
                CountBadge(pendingCount.toLong(), modifier = Modifier.align(Alignment.TopEnd))
            }
        } else {
            var open by remember { mutableStateOf(false) }
            Box {
                Pill(onClick = { open = true }) {
                    Text(filterLabel, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Lọc ảnh", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text("Mọi người") }, onClick = { open = false; onFilter(null) })
                    if (myId != null) DropdownMenuItem(text = { Text("Của tôi") }, onClick = { open = false; onFilter(myId) })
                    filterOptions.forEach { friend ->
                        DropdownMenuItem(
                            text = { Text(friend.name) },
                            leadingIcon = { Avatar(friend, size = 28.dp) },
                            onClick = { open = false; onFilter(friend.id) },
                        )
                    }
                }
            }
        }

        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Box {
                RoundIconButton(Icons.AutoMirrored.Rounded.Chat, "Tin nhắn", onChats, size = 46.dp)
                CountBadge(unreadChats, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

// Under a photo: open the grid, jump back to the camera, more actions
@Composable
private fun HistoryBottomBar(
    photo: PhotoDto?,
    myId: Long?,
    onGrid: () -> Unit,
    onCamera: () -> Unit,
    onMessage: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onReport: (reason: String) -> Unit,
    onBlock: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmBlock by remember { mutableStateOf(false) }
    var reporting by remember { mutableStateOf(false) }
    val isMine = photo?.sender?.id == myId

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(Icons.Rounded.GridView, "Xem tất cả ảnh", onGrid, size = 48.dp, background = Color.Transparent)
        ShutterButton(enabled = true, onClick = onCamera, size = 64.dp)
        Box {
            RoundIconButton(Icons.Rounded.MoreHoriz, "Thêm", { menuOpen = true }, size = 48.dp, background = Color.Transparent)
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (photo != null && !isMine) {
                    DropdownMenuItem(text = { Text("Nhắn tin cho ${photo.sender.name}") }, onClick = { menuOpen = false; onMessage() })
                }
                if (photo != null) {
                    DropdownMenuItem(text = { Text("Lưu về máy") }, onClick = { menuOpen = false; onSave() })
                    DropdownMenuItem(text = { Text("Chia sẻ") }, onClick = { menuOpen = false; onShare() })
                }
                if (isMine) {
                    DropdownMenuItem(
                        text = { Text("Xoá ảnh", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; confirmDelete = true },
                    )
                } else if (photo != null) {
                    DropdownMenuItem(text = { Text("Báo cáo ảnh") }, onClick = { menuOpen = false; reporting = true })
                    DropdownMenuItem(
                        text = { Text("Chặn ${photo.sender.name}", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; confirmBlock = true },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xoá ảnh này?") },
            text = { Text("Ảnh sẽ biến mất khỏi màn hình của mọi người đã nhận.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Xoá", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") } },
        )
    }
    if (confirmBlock && photo != null) {
        BlockDialog(photo.sender, onConfirm = { confirmBlock = false; onBlock() }, onDismiss = { confirmBlock = false })
    }
    if (reporting && photo != null) {
        ReportDialog(photo.sender, isPhoto = true, onSend = { reason -> reporting = false; onReport(reason) }, onDismiss = { reporting = false })
    }
}

// Android 13+ needs permission before showing push notifications
@Composable
private fun AskNotificationPermission() {
    if (Build.VERSION.SDK_INT < 33) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
