/*
 * TỔNG QUAN FILE: TypeConverter của Room: lưu các kiểu SQLite không hiểu thành chuỗi JSON.
 *
 * Thành phần chính:
 * - Cặp chuyển đổi cho UserDto, PhotoDto?, List<ReactionDto> và List<Long>.
 *
 * Lưu ý:
 * - Dữ liệu lưu dạng JSON thì không truy vấn SQL được bên trong. Cần lọc theo trường nào thì tách nó ra cột
 *   riêng (như senderId của CachedPhoto).
 * - Đổi tên trường trong DTO có thể làm dữ liệu cache cũ không đọc được.
 */
package com.example.loki.data.local.db

import androidx.room.TypeConverter
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.data.remote.dto.ReactionDto
import com.example.loki.data.remote.dto.UserDto
import kotlinx.serialization.json.Json

// Room stores nested objects as JSON text
class CacheConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter fun userToText(value: UserDto): String = json.encodeToString(value)
    @TypeConverter fun textToUser(value: String): UserDto = json.decodeFromString(value)

    @TypeConverter fun photoToText(value: PhotoDto?): String? = value?.let { json.encodeToString(it) }
    @TypeConverter fun textToPhoto(value: String?): PhotoDto? = value?.let { json.decodeFromString<PhotoDto>(it) }

    @TypeConverter fun reactionsToText(value: List<ReactionDto>): String = json.encodeToString(value)
    @TypeConverter fun textToReactions(value: String): List<ReactionDto> = json.decodeFromString(value)

    @TypeConverter fun idsToText(value: List<Long>): String = json.encodeToString(value)
    @TypeConverter fun textToIds(value: String): List<Long> = json.decodeFromString(value)
}
