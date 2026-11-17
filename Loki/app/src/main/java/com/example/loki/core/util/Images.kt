/*
 * TỔNG QUAN FILE: Tiện ích xử lý ảnh: file chụp tạm, nén trước khi tải lên, URL ảnh thu nhỏ.
 *
 * Thành phần chính:
 * - newCaptureFile: tạo file mới trong cacheDir/camera cho mỗi lần chụp, xoá file cũ hơn 1 ngày.
 * - compressForUpload: giải mã ở kích thước nhỏ, cắt vuông (tuỳ chọn), nén JPEG 85.
 * - thumbnail: chèn tham số biến đổi của Cloudinary vào URL để tải ảnh nhỏ; URL khác giữ nguyên.
 *
 * Lưu ý:
 * - compressForUpload chặn thread, phải gọi trong Dispatchers.IO.
 * - Shortcut: trên Android 8 (dưới API 28) nhánh BitmapFactory bỏ qua xoay EXIF, ảnh có thể bị nằm ngang.
 * - Giải mã thẳng ở kích thước nhỏ để tránh OutOfMemory với ảnh gốc nhiều MB.
 */
package com.example.loki.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File

// Where the in-app camera saves a shot before it is sent; a new file per shot so a retake never shows a cached image
fun newCaptureFile(context: Context): File {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    dir.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 24 * 60 * 60 * 1000 }?.forEach { it.delete() }
    return File(dir, "${System.currentTimeMillis()}.jpg")
}

// Camera photos are several MB: scale to at most maxSide px and re-encode as JPEG before uploading.
// square = center-crop to 1:1, matching the square viewfinder and how photos are shown
fun compressForUpload(context: Context, uri: Uri, maxSide: Int = 1080, square: Boolean = true): ByteArray {
    val decoded = if (Build.VERSION.SDK_INT >= 28) {
        // ImageDecoder also applies the EXIF rotation (and front-camera mirroring) the camera writes
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val side = if (square) minOf(info.size.width, info.size.height) else maxOf(info.size.width, info.size.height)
            val scale = maxSide.toFloat() / side
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // cropping needs a software bitmap
            if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
        }
    } else {
        // shortcut: ignores EXIF rotation on Android 8.x, add androidx.exifinterface if those phones matter
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Couldn't read the photo")
    }
    val bitmap = if (square) decoded.centerSquare() else decoded
    return ByteArrayOutputStream().use {
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)
        it.toByteArray()
    }
}

private fun Bitmap.centerSquare(): Bitmap {
    val side = minOf(width, height)
    if (width == height) return this
    return Bitmap.createBitmap(this, (width - side) / 2, (height - side) / 2, side, side)
}

// Cloudinary resizes on the fly from the URL: a 300 px square for the grid instead of the 1080 px original.
// Smaller downloads, and many more photos fit in the image cache. Other URLs (Google avatars) pass through
fun thumbnail(url: String?, size: Int): String? {
    if (url == null || "res.cloudinary.com" !in url || "/upload/" !in url) return url
    return url.replaceFirst("/upload/", "/upload/w_$size,h_$size,c_fill,q_auto/")
}
