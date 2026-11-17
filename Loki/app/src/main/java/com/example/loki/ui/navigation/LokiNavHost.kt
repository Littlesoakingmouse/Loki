/*
 * TỔNG QUAN FILE: Bản đồ điều hướng của app và cách chuyển giữa trạng thái đăng nhập và đăng xuất.
 *
 * Thành phần chính:
 * - LoginRoute, HomeRoute, ProfileRoute, ChatListRoute, ChatRoute: route có kiểu (@Serializable).
 * - LokiNavHost: không vẽ gì khi chưa đọc xong token; dựng NavHost với màn bắt đầu theo isLoggedIn; xử lý deep
 *   link chat.
 *
 * Lưu ý:
 * - key(isLoggedIn) tạo lại cả đồ thị và back stack mỗi khi đăng nhập hoặc đăng xuất, nên Back không quay lại
 *   màn cũ.
 * - Màn hình không nhận navController, chỉ nhận lambda như onBack, onChat.
 * - Deep link khi chưa đăng nhập được giữ lại và xử lý sau khi đăng nhập.
 */
package com.example.loki.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.loki.ui.chat.ChatListScreen
import com.example.loki.ui.chat.ChatScreen
import com.example.loki.ui.home.HomeScreen
import com.example.loki.ui.login.LoginScreen
import com.example.loki.ui.profile.ProfileScreen
import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

// Camera + photo history in one vertical pager, like Locket
@Serializable
object HomeRoute

@Serializable
object ProfileRoute

@Serializable
object ChatListRoute

// conversationId is null when the chat starts from a friend or a photo reply
@Serializable
data class ChatRoute(val otherUserId: Long, val otherUserName: String, val conversationId: Long? = null)

@Composable
fun LokiNavHost(
    deepLink: DeepLink?,
    onDeepLinkHandled: () -> Unit,
    modifier: Modifier = Modifier,
    sessionViewModel: SessionViewModel = hiltViewModel(),
) {
    val isLoggedIn = sessionViewModel.isLoggedIn.collectAsStateWithLifecycle().value ?: return

    // A fresh graph whenever login state flips, so logging in/out (or an expired token) also clears the back stack
    key(isLoggedIn) {
        val navController = rememberNavController()
        val back: () -> Unit = { navController.popBackStack() }

        // Chats open as their own screen; photo and invite links are handled by Home.
        // Logged out: the link waits until after login
        if (isLoggedIn && deepLink is DeepLink.Chat) {
            LaunchedEffect(deepLink) {
                navController.navigate(ChatRoute(deepLink.otherUserId, deepLink.otherUserName, deepLink.conversationId))
                onDeepLinkHandled()
            }
        }

        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) HomeRoute else LoginRoute,
            modifier = modifier,
        ) {
            composable<LoginRoute> { LoginScreen() }
            composable<HomeRoute> {
                HomeScreen(
                    deepLink = deepLink.takeIf { it !is DeepLink.Chat },
                    onDeepLinkHandled = onDeepLinkHandled,
                    onProfile = { navController.navigate(ProfileRoute) },
                    onChats = { navController.navigate(ChatListRoute) },
                    onChat = { user -> navController.navigate(ChatRoute(user.id, user.name)) },
                )
            }
            composable<ProfileRoute> { ProfileScreen(onBack = back) }
            composable<ChatListRoute> {
                ChatListScreen(
                    onBack = back,
                    onOpen = { c -> navController.navigate(ChatRoute(c.otherUser.id, c.otherUser.name, c.id)) },
                )
            }
            composable<ChatRoute> { ChatScreen(onBack = back) }
        }
    }
}
