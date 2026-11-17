/*
 * TỔNG QUAN FILE: Room DAO cho cache hồ sơ của mình và danh sách bạn bè.
 *
 * Thành phần chính:
 * - observeMe: dòng có isMe = 1. observeFriends: các dòng còn lại, theo thứ tự backend trả về.
 * - replaceFriends (@Transaction): thay toàn bộ danh sách bạn.
 *
 * Lưu ý:
 * - Hồ sơ và bạn bè chung một bảng, phân biệt bằng cột isMe; deleteFriends không đụng tới dòng của mình.
 */
package com.example.loki.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.loki.data.local.db.entity.CachedUser
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isMe = 1 LIMIT 1")
    fun observeMe(): Flow<CachedUser?>

    @Query("SELECT * FROM users WHERE isMe = 0 ORDER BY position")
    fun observeFriends(): Flow<List<CachedUser>>

    @Upsert
    suspend fun upsert(users: List<CachedUser>)

    @Query("DELETE FROM users WHERE isMe = 0")
    suspend fun deleteFriends()

    @Transaction
    suspend fun replaceFriends(friends: List<CachedUser>) {
        deleteFriends()
        upsert(friends)
    }
}
