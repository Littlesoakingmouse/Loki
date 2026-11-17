/*
 * TỔNG QUAN FILE: Retrofit interface cho hồ sơ người dùng và bạn bè (api/users/..., api/friends/...).
 *
 * Thành phần chính:
 * - me, updateMe, deleteMe, logout: hồ sơ của chính mình; logout ngừng gửi push tới máy này.
 * - searchByEmail, byId: tìm người theo email hoặc theo id trong link mời.
 * - friends, sendFriendRequest, pendingRequests, respond, deleteFriendship, unfriend: quản lý bạn bè.
 * - block, unblock, blocked: chặn người dùng. report: báo cáo vi phạm. changePassword: đổi mật khẩu.
 *
 * Lưu ý:
 * - updateMe nhận UpdateProfileRequest: chỉ gửi trường cần đổi, các trường null bị backend bỏ qua.
 */
package com.example.loki.data.remote.api

import com.example.loki.data.remote.dto.ChangePasswordRequest
import com.example.loki.data.remote.dto.FriendshipDto
import com.example.loki.data.remote.dto.LogoutRequest
import com.example.loki.data.remote.dto.ReportRequest
import com.example.loki.data.remote.dto.RespondFriendRequest
import com.example.loki.data.remote.dto.SendFriendRequest
import com.example.loki.data.remote.dto.UpdateProfileRequest
import com.example.loki.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface UserApi {
    @GET("api/users/me")
    suspend fun me(): UserDto

    @PUT("api/users/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): UserDto

    @GET("api/users/search")
    suspend fun searchByEmail(@Query("email") email: String): UserDto

    // Opens an invite link
    @GET("api/users/{userId}")
    suspend fun byId(@Path("userId") userId: Long): UserDto

    @DELETE("api/users/me")
    suspend fun deleteMe()

    // fcmToken = this phone: the backend stops pushing to it, other phones of the account keep theirs
    @POST("api/users/logout")
    suspend fun logout(@Body request: LogoutRequest)

    @PUT("api/users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest)

    @GET("api/friends")
    suspend fun friends(): List<UserDto>

    @POST("api/friends/requests")
    suspend fun sendFriendRequest(@Body request: SendFriendRequest): FriendshipDto

    // Requests other people sent to me
    @GET("api/friends/requests/pending")
    suspend fun pendingRequests(): List<FriendshipDto>

    @POST("api/friends/requests/respond")
    suspend fun respond(@Body request: RespondFriendRequest): FriendshipDto

    // Decline a request
    @DELETE("api/friends/{friendshipId}")
    suspend fun deleteFriendship(@Path("friendshipId") friendshipId: Long)

    @DELETE("api/friends/users/{friendId}")
    suspend fun unfriend(@Path("friendId") friendId: Long)

    // Also ends the friendship or request; they can no longer find or message me
    @POST("api/friends/users/{userId}/block")
    suspend fun block(@Path("userId") userId: Long)

    @DELETE("api/friends/users/{userId}/block")
    suspend fun unblock(@Path("userId") userId: Long)

    @GET("api/friends/blocked")
    suspend fun blocked(): List<UserDto>

    @POST("api/reports")
    suspend fun report(@Body request: ReportRequest)
}
