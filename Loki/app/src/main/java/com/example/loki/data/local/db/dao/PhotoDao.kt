/*
 * TỔNG QUAN FILE: Room DAO cho cache lịch sử ảnh.
 *
 * Thành phần chính:
 * - observeAll, observeBySender: Flow lịch sử (mới nhất trước) mà màn Home lắng nghe.
 * - inScope: danh sách để so sánh khi đồng bộ; senderId null nghĩa là toàn bộ lịch sử.
 * - latestFromOthers: ảnh mới nhất người khác gửi, dùng cho widget khi offline.
 * - deleteBySender: bỏ ảnh của người vừa bị chặn khỏi cache.
 * - sync (@Transaction): xoá ảnh đã bị xoá trên server, upsert trang mới, cắt cache còn keep ảnh.
 *
 * Lưu ý:
 * - ORDER BY createdAt DESC, id DESC: id giúp thứ tự ổn định khi hai ảnh cùng thời gian, tránh Pager nhảy.
 * - Giữ sync trong một transaction để Flow chỉ phát một lần, UI không thấy trạng thái dở dang.
 */
package com.example.loki.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.loki.data.local.db.entity.CachedPhoto
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos ORDER BY createdAt DESC, id DESC")
    fun observeAll(): Flow<List<CachedPhoto>>

    @Query("SELECT * FROM photos WHERE senderId = :senderId ORDER BY createdAt DESC, id DESC")
    fun observeBySender(senderId: Long): Flow<List<CachedPhoto>>

    // What the widget shows when the backend can't be reached: the newest photo someone else sent
    @Query("SELECT * FROM photos WHERE senderId != :myId ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun latestFromOthers(myId: Long): CachedPhoto?

    // The cached rows a fresh page is compared against (null sender = the unfiltered history)
    @Query("SELECT * FROM photos WHERE (:senderId IS NULL OR senderId = :senderId) ORDER BY createdAt DESC, id DESC")
    suspend fun inScope(senderId: Long?): List<CachedPhoto>

    @Query("SELECT * FROM photos WHERE id = :id")
    suspend fun byId(id: Long): CachedPhoto?

    @Upsert
    suspend fun upsert(photos: List<CachedPhoto>)

    @Query("DELETE FROM photos WHERE senderId = :senderId")
    suspend fun deleteBySender(senderId: Long)

    @Query("DELETE FROM photos WHERE id IN (:ids)")
    suspend fun delete(ids: Collection<Long>)

    // Keeps the cache to the newest `keep` photos
    @Query("DELETE FROM photos WHERE id NOT IN (SELECT id FROM photos ORDER BY createdAt DESC, id DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Transaction
    suspend fun sync(upserts: List<CachedPhoto>, deletes: Collection<Long>, keep: Int) {
        if (deletes.isNotEmpty()) delete(deletes)
        upsert(upserts)
        trimTo(keep)
    }
}
