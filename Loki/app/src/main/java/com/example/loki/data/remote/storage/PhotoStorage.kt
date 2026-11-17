/*
 * TỔNG QUAN FILE: Tải ảnh JPEG lên Cloudinary và trả về URL; backend chỉ lưu URL này.
 *
 * Thành phần chính:
 * - upload(jpeg): gửi multipart form tới Cloudinary, đọc secure_url từ phản hồi.
 * - UploadRejectedException: Cloudinary từ chối (4xx: preset sai, file hỏng...). Thử lại cũng vô ích.
 * - CloudinaryResponse, CloudinaryError (private): ánh xạ JSON trả về.
 *
 * Lưu ý:
 * - Phân loại lỗi để outbox xử lý đúng: mất mạng, 5xx, 429 hoặc phản hồi không đọc được -> IOException (thử
 *   lại sau); 4xx khác -> UploadRejectedException (bỏ mục đó, không chặn hàng đợi).
 * - Dùng OkHttpClient riêng, không có AuthInterceptor, để JWT của Loki không bị gửi cho Cloudinary.
 * - execute() chặn thread nên toàn bộ hàm chạy trong Dispatchers.IO.
 * - Shortcut: upload preset dạng unsigned, ai biết tên preset đều tải lên được. Trước khi phát hành nên để
 *   backend ký từng lần tải lên.
 * - Tên cloud và preset lấy từ BuildConfig (local.properties), không viết cứng ở đây.
 */
package com.example.loki.data.remote.storage

import com.example.loki.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// Set in app/build.gradle.kts (override with loki.cloudinary* in local.properties)
private const val CLOUDINARY_CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
private const val CLOUDINARY_UPLOAD_PRESET = BuildConfig.CLOUDINARY_UPLOAD_PRESET

// Cloudinary refused the file itself; sending it again won't help
class UploadRejectedException(message: String) : Exception(message)

// Uploads photos to Cloudinary; the backend only stores the returned URL
@Singleton
class PhotoStorage @Inject constructor() {
    // Its own client on purpose: the app's Retrofit client would send our backend JWT to Cloudinary
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS) // a 1080 px JPEG on a slow connection
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    // shortcut: an unsigned preset lets anyone who knows its name upload; have the backend sign uploads before going public
    suspend fun upload(jpeg: ByteArray): String = withContext(Dispatchers.IO) {
        check(CLOUDINARY_CLOUD_NAME != "your-cloud-name") {
            "Photo upload isn't set up: set loki.cloudinaryCloudName in local.properties"
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
            .addFormDataPart("file", "photo.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUDINARY_CLOUD_NAME/image/upload")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            // Busy or broken on their side: worth another try later
            if (response.code >= 500 || response.code == 429) throw IOException("Cloudinary HTTP ${response.code}")
            val text = response.body?.string().orEmpty()
            val result = runCatching { json.decodeFromString<CloudinaryResponse>(text) }.getOrNull()
            if (!response.isSuccessful) {
                throw UploadRejectedException(result?.error?.message ?: "Cloudinary HTTP ${response.code}")
            }
            result?.secureUrl ?: throw IOException("Unreadable Cloudinary response")
        }
    }
}

@Serializable
private data class CloudinaryResponse(
    @SerialName("secure_url") val secureUrl: String? = null,
    val error: CloudinaryError? = null,
)

@Serializable
private data class CloudinaryError(val message: String? = null)
