/*
 * TỔNG QUAN FILE: Repository bạn bè: tìm người, lời mời, huỷ kết bạn, chặn và báo cáo.
 *
 * Thành phần chính:
 * - observeFriends / refreshFriends: danh sách bạn bè (cache Room, lấy qua UserRepository).
 * - searchByEmail, byId (link mời), sendRequest, pendingRequests, accept, decline, unfriend.
 * - block, unblock, blocked: chặn thì xoá luôn ảnh của người đó khỏi cache và làm mới danh sách bạn.
 * - report: gửi báo cáo vi phạm (người dùng, kèm ảnh nếu có).
 *
 * Lưu ý:
 * - ViewModel chỉ gọi tới đây, không gọi UserApi trực tiếp.
 * - Giới hạn bạn bè (MAX_FRIENDS) do backend chặn; lỗi trả về được hiện nguyên văn cho người dùng.
 */
package com.example.loki.data.repository

import com.example.loki.data.local.db.dao.PhotoDao
import com.example.loki.data.remote.api.UserApi
import com.example.loki.data.remote.dto.FriendshipDto
import com.example.loki.data.remote.dto.FriendshipStatus
import com.example.loki.data.remote.dto.ReportRequest
import com.example.loki.data.remote.dto.RespondFriendRequest
import com.example.loki.data.remote.dto.SendFriendRequest
import com.example.loki.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FriendRepository @Inject constructor(
    private val userApi: UserApi,
    private val users: UserRepository,
    private val photoDao: PhotoDao,
) {
    fun observeFriends(): Flow<List<UserDto>> = users.observeFriends()

    suspend fun refreshFriends() = users.refreshFriends()

    suspend fun pendingRequests(): List<FriendshipDto> = userApi.pendingRequests()

    suspend fun searchByEmail(email: String): UserDto = userApi.searchByEmail(email.trim())

    // An opened invite link
    suspend fun byId(userId: Long): UserDto = userApi.byId(userId)

    suspend fun sendRequest(user: UserDto) {
        userApi.sendFriendRequest(SendFriendRequest(user.id))
    }

    suspend fun accept(request: FriendshipDto) {
        userApi.respond(RespondFriendRequest(request.id, FriendshipStatus.ACCEPTED))
        refreshFriends()
    }

    suspend fun decline(request: FriendshipDto) {
        userApi.deleteFriendship(request.id)
    }

    suspend fun unfriend(user: UserDto) {
        userApi.unfriend(user.id)
        refreshFriends()
    }

    // Their photos leave this phone too; the backend stops showing them in the history
    suspend fun block(user: UserDto) {
        userApi.block(user.id)
        photoDao.deleteBySender(user.id)
        refreshFriends()
    }

    suspend fun unblock(user: UserDto) {
        userApi.unblock(user.id)
    }

    suspend fun blocked(): List<UserDto> = userApi.blocked()

    suspend fun report(user: UserDto, photoId: Long? = null, reason: String? = null) {
        userApi.report(ReportRequest(user.id, photoId, reason?.trim()?.ifBlank { null }))
    }
}
