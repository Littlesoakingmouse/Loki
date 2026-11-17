/*
 * TỔNG QUAN FILE: Unit test cho staleIds trong HistorySync.kt (tìm ảnh đã bị xoá trên server).
 *
 * Thành phần chính:
 * - ref, cached: dữ liệu mẫu gồm 5 ảnh từ 08:00 tới 12:00.
 * - Các test: xoá trong cửa sổ trang đầu, giữ ảnh ngoài cửa sổ, trang cuối, trang giữa, trang rỗng, giây có
 *   phần lẻ.
 *
 * Lưu ý:
 * - Nhớ dùng hậu tố L (setOf(4L)) để kiểu là Set<Long>, nếu không assertEquals sẽ báo khác nhau.
 * - Sửa logic đồng bộ trong HistorySync thì thêm test cho trường hợp mới ở đây.
 */
package com.example.loki.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class HistorySyncTest {
    private fun ref(id: Long, time: String) = CachedRef(id, "2026-10-20T$time")

    // Cached newest first: 5 (12:00) … 1 (08:00)
    private val cached = listOf(ref(5, "12:00"), ref(4, "11:00"), ref(3, "10:00"), ref(2, "09:00"), ref(1, "08:00"))

    @Test
    fun `first page drops a photo deleted inside its window`() {
        val page = listOf(ref(6, "13:00"), ref(5, "12:00"), ref(3, "10:00")) // 4 was deleted
        assertEquals(setOf(4L), staleIds(cached, page, isFirstPage = true, isLastPage = false))
    }

    @Test
    fun `photos older than a full page are kept`() {
        val page = listOf(ref(5, "12:00"), ref(4, "11:00"))
        assertEquals(emptySet<Long>(), staleIds(cached, page, isFirstPage = true, isLastPage = false))
    }

    @Test
    fun `last page also drops everything older that the server no longer has`() {
        val page = listOf(ref(5, "12:00"), ref(3, "10:00"))
        assertEquals(setOf(4L, 2L, 1L), staleIds(cached, page, isFirstPage = true, isLastPage = true))
    }

    @Test
    fun `a later page only judges its own window`() {
        val page = listOf(ref(3, "10:00"), ref(1, "08:00")) // page 2: 2 was deleted, 5 and 4 are above it
        assertEquals(setOf(2L), staleIds(cached, page, isFirstPage = false, isLastPage = false))
    }

    @Test
    fun `empty first page clears the scope`() {
        assertEquals(setOf(5L, 4L, 3L, 2L, 1L), staleIds(cached, emptyList(), isFirstPage = true, isLastPage = true))
    }

    @Test
    fun `empty later page deletes nothing`() {
        assertEquals(emptySet<Long>(), staleIds(cached, emptyList(), isFirstPage = false, isLastPage = true))
    }

    @Test
    fun `fractional seconds still compare in order`() {
        val withFractions = listOf(CachedRef(9, "2026-10-20T10:00:00.5"), CachedRef(8, "2026-10-20T10:00:00"))
        val page = listOf(CachedRef(10, "2026-10-20T11:00:00"), CachedRef(9, "2026-10-20T10:00:00.5"))
        // 8 is older than the page's last photo and the page isn't the last: kept
        assertEquals(emptySet<Long>(), staleIds(withFractions, page, isFirstPage = true, isLastPage = false))
    }
}
