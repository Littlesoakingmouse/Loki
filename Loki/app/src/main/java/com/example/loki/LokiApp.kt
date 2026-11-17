/*
 * TỔNG QUAN FILE: Lớp Application: điểm khởi động của app và của Hilt.
 *
 * Thành phần chính:
 * - @HiltAndroidApp: tạo container DI cấp app.
 * - workManagerConfiguration: đưa HiltWorkerFactory cho WorkManager để OutboxWorker được inject.
 * - onCreate: lên lịch OutboxWorker để gửi nốt những gì còn trong outbox.
 * - newImageLoader: cấu hình Coil, bộ nhớ RAM 25% và bộ nhớ đĩa cố định 250 MB.
 *
 * Lưu ý:
 * - Manifest phải trỏ android:name tới lớp này và gỡ bộ khởi tạo WorkManager mặc định, nếu không Worker không
 *   có phụ thuộc.
 * - Bộ nhớ đệm ảnh trên đĩa là thứ giúp lịch sử ảnh hiện được khi offline.
 */
package com.example.loki

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import com.example.loki.data.worker.OutboxWorker
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

// Downloaded photos are kept on disk so the cached history shows its images
private const val IMAGE_DISK_CACHE_BYTES = 250L * 1024 * 1024

// Hilt builds the app-wide dependency container when this class starts
@HiltAndroidApp
class LokiApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    // Lets WorkManager create @HiltWorker workers with their dependencies
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Picks up anything still queued, e.g. after an earlier run gave up on a server error
        OutboxWorker.schedule(this)
    }

    // A fixed disk budget: Coil's default is a share of free space, which is tiny on a nearly full phone
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(IMAGE_DISK_CACHE_BYTES)
                    .build()
            }
            .crossfade(true)
            .build()
}
