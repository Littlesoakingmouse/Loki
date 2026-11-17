/*
 * TỔNG QUAN FILE: Widget màn hình chính viết bằng Glance: hiện ảnh mới nhất bạn bè gửi, bấm vào mở ảnh đó
 * trong app.
 *
 * Thành phần chính:
 * - WidgetEntryPoint: lấy PhotoRepository và SessionStore từ Hilt, vì widget không dùng được
 *   @AndroidEntryPoint.
 * - LokiWidget.provideGlance: lấy dữ liệu (server rồi cache), tải bitmap, sau đó vẽ trong provideContent.
 * - loadBitmap: tải ảnh nhỏ bằng Coil, tắt hardware bitmap.
 * - SenderTag, Placeholder: phần giao diện; LokiWidgetReceiver: receiver khai báo trong Manifest.
 *
 * Lưu ý:
 * - Dùng Composable của androidx.glance, không phải Compose thường.
 * - Chưa đăng nhập thì chỉ hiện placeholder, không bao giờ hiện ảnh của tài khoản trước.
 * - Widget chỉ cập nhật khi gọi updateAll (SessionViewModel, LokiMessagingService) hoặc theo chu kỳ 30 phút.
 * - Bitmap gửi sang launcher bị giới hạn dung lượng (~1 MB), nên chỉ tải ảnh 400 px (WIDGET_IMAGE_PX).
 */
package com.example.loki.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.example.loki.core.util.thumbnail
import com.example.loki.data.local.datastore.SessionStore
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.data.repository.PhotoRepository
import com.example.loki.ui.navigation.photoIntent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

// Widgets aren't Activities, so they reach Hilt objects through an entry point
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun photoRepository(): PhotoRepository
    fun sessionStore(): SessionStore
}

private const val WIDGET_IMAGE_PX = 400
private val White = ColorProvider(Color.White)
private val Yellow = ColorProvider(Color(0xFFFFC83D))

// Home screen widget showing the newest photo a friend sent you; tapping it opens that photo in the app
class LokiWidget : GlanceAppWidget() {

    // One layout that stretches to whatever size the user gives the widget
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val deps = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        // Logged out = placeholder, never the previous account's cached photo.
        // Offline = the newest cached photo; its image comes from Coil's disk cache
        val myId = deps.sessionStore().userId.first().takeIf { deps.sessionStore().token.first() != null }
        val photo = myId?.let { deps.photoRepository().latestFromOthers(it) }
        // 400 px is sharp enough for a 2x2 widget; much bigger and the bitmap can overflow the
        // ~1 MB limit for sending a widget layout to the launcher (TransactionTooLargeException)
        val image = photo?.let { loadBitmap(context, thumbnail(it.imageUrl, WIDGET_IMAGE_PX)!!, WIDGET_IMAGE_PX) }
        val avatar = photo?.sender?.avatarUrl?.let { loadBitmap(context, thumbnail(it, 96)!!, 96) }

        provideContent {
            Box(
                modifier = GlanceModifier.fillMaxSize()
                    .cornerRadius(28.dp)
                    .background(Color(0xFF141414))
                    .clickable(actionStartActivity(photoIntent(context, photo?.id))),
                contentAlignment = Alignment.BottomStart,
            ) {
                if (photo != null && image != null) {
                    Image(
                        provider = ImageProvider(image),
                        contentDescription = photo.caption ?: "Ảnh của ${photo.sender.name}",
                        contentScale = ContentScale.Crop,
                        modifier = GlanceModifier.fillMaxSize(),
                    )
                    SenderTag(photo, avatar)
                } else {
                    Placeholder()
                }
            }
        }
    }

    private suspend fun loadBitmap(context: Context, url: String, size: Int): Bitmap? {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(size) // widgets have a small memory budget
            .allowHardware(false) // RemoteViews can't draw hardware bitmaps
            .build()
        return (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
    }
}

@Composable
private fun SenderTag(photo: PhotoDto, avatar: Bitmap?) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(10.dp)) {
        photo.caption?.let {
            Text(
                it,
                maxLines = 1,
                style = TextStyle(color = White, fontWeight = FontWeight.Bold, fontSize = 13.sp),
                modifier = GlanceModifier.cornerRadius(14.dp).background(Color(0x99000000)).padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Spacer(GlanceModifier.size(6.dp))
        }
        Row(
            modifier = GlanceModifier.cornerRadius(14.dp).background(Color(0x99000000)).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (avatar != null) {
                Image(
                    provider = ImageProvider(avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.size(20.dp).cornerRadius(10.dp),
                )
                Spacer(GlanceModifier.width(6.dp))
            }
            Text(
                photo.sender.name,
                maxLines = 1,
                style = TextStyle(color = White, fontWeight = FontWeight.Bold, fontSize = 12.sp),
                modifier = GlanceModifier.padding(end = 6.dp),
            )
        }
    }
}

@Composable
private fun Placeholder() {
    Column(
        modifier = GlanceModifier.fillMaxSize().padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("💛", style = TextStyle(fontSize = 28.sp))
        Spacer(GlanceModifier.size(6.dp))
        Text(
            "Mở Loki để xem ảnh từ bạn bè",
            style = TextStyle(color = Yellow, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center),
        )
    }
}

class LokiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LokiWidget()
}
