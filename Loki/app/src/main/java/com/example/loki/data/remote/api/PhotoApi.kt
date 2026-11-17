/*
 * TỔNG QUAN FILE: Retrofit interface cho ảnh: gửi, lịch sử, xoá, đã xem, cảm xúc (api/photos/...).
 *
 * Thành phần chính:
 * - PAGE_SIZE = 15: số ảnh mỗi trang; FeedViewModel và PhotoRepository dùng nó để biết đã hết trang.
 * - send: chỉ gửi URL ảnh (đã tải lên Cloudinary trước) cùng danh sách người nhận.
 * - history: ảnh mình gửi và ảnh bạn bè gửi cho mình; friendId = null thì bỏ hẳn tham số khỏi URL.
 * - latest: ảnh mới nhất cho widget, trả 404 khi chưa ai gửi ảnh.
 * - markSeen, react, removeReaction, delete.
 * - activity: chỉ với ảnh của mình, ai đã xem và thả emoji gì.
 *
 * Lưu ý:
 * - File ảnh không đi qua backend; xem PhotoStorage.
 * - Đổi PAGE_SIZE phải khớp với giới hạn bên backend và với logic staleIds trong HistorySync.
 */
package com.example.loki.data.remote.api

import com.example.loki.data.remote.dto.AddReactionRequest
import com.example.loki.data.remote.dto.PhotoActivityDto
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.data.remote.dto.ReactionDto
import com.example.loki.data.remote.dto.SendPhotoRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

const val PAGE_SIZE = 15

interface PhotoApi {
    // The image must already be uploaded; this only sends its URL to friends
    @POST("api/photos")
    suspend fun send(@Body request: SendPhotoRequest): PhotoDto

    @GET("api/photos/feed")
    suspend fun feed(@Query("page") page: Int, @Query("size") size: Int = PAGE_SIZE): List<PhotoDto>

    // Photos I sent + photos friends sent me, newest first. friendId = one friend only (my own id = only mine)
    @GET("api/photos/history")
    suspend fun history(
        @Query("page") page: Int,
        @Query("friendId") friendId: Long? = null,
        @Query("size") size: Int = PAGE_SIZE,
    ): List<PhotoDto>

    // Only the sender can delete
    @DELETE("api/photos/{photoId}")
    suspend fun delete(@Path("photoId") photoId: Long)

    // 404 when nobody has sent you a photo yet
    @GET("api/photos/widget")
    suspend fun latest(): PhotoDto

    @PUT("api/photos/{photoId}/seen")
    suspend fun markSeen(@Path("photoId") photoId: Long)

    // Adds or replaces my emoji on a photo
    @POST("api/photos/reactions")
    suspend fun react(@Body request: AddReactionRequest): ReactionDto

    @DELETE("api/photos/{photoId}/reactions")
    suspend fun removeReaction(@Path("photoId") photoId: Long)

    // Only for my own photos: who saw it and what they reacted
    @GET("api/photos/{photoId}/activity")
    suspend fun activity(@Path("photoId") photoId: Long): List<PhotoActivityDto>
}
