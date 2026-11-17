/*
 * TỔNG QUAN FILE: Activity duy nhất của app; mọi màn hình là Composable vẽ bên trong nó.
 *
 * Thành phần chính:
 * - deepLink: state chứa đích đến từ thông báo, widget hoặc link mời; được xoá sau khi màn hình xử lý xong.
 * - onCreate: bật edge-to-edge, đọc deep link lần đầu, dựng LokiTheme và LokiNavHost.
 * - onNewIntent: nhận deep link mới khi app đang mở (launchMode="singleTop").
 *
 * Lưu ý:
 * - Chỉ đọc intent trong onCreate khi savedInstanceState == null, để xoay màn hình không xử lý lại link cũ.
 * - Thanh trạng thái để chữ sáng vì giao diện luôn tối.
 */
package com.example.loki

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import com.example.loki.ui.navigation.DeepLink
import com.example.loki.ui.navigation.LokiNavHost
import com.example.loki.ui.navigation.toDeepLink
import com.example.loki.ui.theme.LokiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Set by a notification, the widget or an invite link; cleared once the screen has handled it
    private val deepLink = mutableStateOf<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light icons on the always-dark UI
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (savedInstanceState == null) deepLink.value = intent.toDeepLink()
        setContent {
            LokiTheme {
                // Surface gives light text on black to screens without a Scaffold (Home's pager)
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    LokiNavHost(
                        deepLink = deepLink.value,
                        onDeepLinkHandled = { deepLink.value = null },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    // launchMode="singleTop": tapping a notification while the app is open lands here
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toDeepLink()?.let { deepLink.value = it }
    }
}
