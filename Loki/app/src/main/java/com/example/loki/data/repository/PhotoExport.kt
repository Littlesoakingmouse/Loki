/*
 * TỔNG QUAN FILE: Đưa một ảnh ra khỏi app: lưu vào thư viện ảnh của máy, hoặc chia sẻ sang app khác.
 *
 * Thành phần chính:
 * - saveToGallery(url): tải ảnh gốc rồi ghi vào Pictures/Loki. Android 10+ ghi qua MediaStore (không cần quyền);
 *   Android 8-9 cần quyền WRITE_EXTERNAL_STORAGE (màn hình xin trước khi gọi).
 * - shareIntent(url, text): tải ảnh vào cache/shared rồi trả Intent chia sẻ với content:// từ FileProvider.
 *
 * Lưu ý:
 * - Dùng OkHttpClient riêng không có AuthInterceptor: ảnh nằm trên Cloudinary, không được gửi JWT tới đó.
 * - Thư mục cache/shared là thư mục duy nhất mở cho FileProvider (xml/file_paths.xml); file cũ hơn 1 ngày bị
 *   dọn mỗi lần chia sẻ.
 */
package com.example.loki.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoExport @Inject constructor(@ApplicationContext private val context: Context) {
    // No AuthInterceptor: these are public Cloudinary URLs
    private val client = OkHttpClient.Builder().callTimeout(60, TimeUnit.SECONDS).build()

    suspend fun saveToGallery(url: String) = withContext(Dispatchers.IO) {
        val bytes = download(url)
        val name = "Loki_${System.currentTimeMillis()}.jpg"
        if (Build.VERSION.SDK_INT >= 29) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Loki")
                put(MediaStore.Images.Media.IS_PENDING, 1) // hidden from other apps until fully written
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Couldn't create the gallery entry")
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: throw IOException("Couldn't write the photo")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Loki").apply { mkdirs() }
            val file = File(dir, name).apply { writeBytes(bytes) }
            // Makes it show up in the gallery right away
            MediaScannerConnection.scanFile(context, arrayOf(file.path), arrayOf("image/jpeg"), null)
        }
    }

    // A chooser for sending the photo to another app
    suspend fun shareIntent(url: String, text: String?): Intent = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000 }?.forEach { it.delete() }
        val file = File(dir, "loki_${System.currentTimeMillis()}.jpg").apply { writeBytes(download(url)) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            text?.let { putExtra(Intent.EXTRA_TEXT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        Intent.createChooser(send, null)
    }

    private fun download(url: String): ByteArray =
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.bytes() ?: throw IOException("Empty photo")
        }
}
