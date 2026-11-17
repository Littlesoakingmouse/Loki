/*
 * TỔNG QUAN FILE: Repository hồ sơ của mình và danh sách bạn bè, cache trong Room.
 *
 * Thành phần chính:
 * - observeMe, observeFriends: Flow từ Room cho thanh trên, bộ đếm bạn và hàng chọn người nhận.
 * - refreshMe, refreshFriends: gọi API rồi ghi vào Room.
 * - saveMe: lưu hồ sơ mới sau khi sửa để mọi màn hình cập nhật.
 * - updateDisplayName, changeAvatar (nén sẵn JPEG -> Cloudinary -> backend), changePassword.
 *
 * Lưu ý:
 * - refreshFriends thay toàn bộ danh sách và giữ thứ tự backend trả về qua cột position.
 */
package com.example.loki.data.repository

import com.example.loki.data.local.db.dao.UserDao
import com.example.loki.data.local.db.entity.CachedUser
import com.example.loki.data.remote.api.UserApi
import com.example.loki.data.remote.dto.ChangePasswordRequest
import com.example.loki.data.remote.dto.UpdateProfileRequest
import com.example.loki.data.remote.storage.PhotoStorage
import com.example.loki.data.remote.dto.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// My profile and my friends, cached so the top bar, counter and recipient row show on launch
@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi,
    private val userDao: UserDao,
    private val photoStorage: PhotoStorage,
) {
    fun observeMe(): Flow<UserDto?> = userDao.observeMe().map { it?.user }

    fun observeFriends(): Flow<List<UserDto>> = userDao.observeFriends().map { rows -> rows.map { it.user } }

    suspend fun refreshMe(): UserDto = userApi.me().also { saveMe(it) }

    // After editing the profile, so every screen shows the new name or avatar
    suspend fun saveMe(me: UserDto) {
        userDao.upsert(listOf(CachedUser(me.id, me, isMe = true, position = 0)))
    }

    suspend fun updateDisplayName(name: String): UserDto =
        userApi.updateMe(UpdateProfileRequest(displayName = name.trim())).also { saveMe(it) }

    // jpeg: already square-cropped and compressed
    suspend fun changeAvatar(jpeg: ByteArray): UserDto =
        userApi.updateMe(UpdateProfileRequest(avatarUrl = photoStorage.upload(jpeg))).also { saveMe(it) }

    suspend fun changePassword(current: String, new: String) {
        userApi.changePassword(ChangePasswordRequest(current, new))
    }

    suspend fun refreshFriends() {
        val friends = userApi.friends()
        userDao.replaceFriends(friends.mapIndexed { i, friend -> CachedUser(friend.id, friend, isMe = false, position = i) })
    }
}
