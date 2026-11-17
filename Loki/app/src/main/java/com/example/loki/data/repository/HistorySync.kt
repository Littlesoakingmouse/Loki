/*
 * TỔNG QUAN FILE: Hàm thuần tìm những ảnh trong cache đã bị xoá trên server, dựa vào một trang lịch sử.
 *
 * Thành phần chính:
 * - CachedRef: id và createdAt của một ảnh, đủ để so sánh.
 * - staleIds: một trang bao phủ một khoảng thời gian; ảnh cache nằm trong khoảng đó mà không có trong trang
 *   thì đã bị xoá. Ảnh ngoài khoảng được giữ nguyên.
 *
 * Lưu ý:
 * - Hàm không chạm mạng hay database nên dễ test; mọi thay đổi phải chạy lại HistorySyncTest.
 * - So sánh thời gian bằng chuỗi ISO; createdAt thiếu được coi là cũ nhất.
 * - Trang 0 rỗng nghĩa là trong phạm vi lọc không còn ảnh nào, nên xoá hết cache của phạm vi đó.
 */
package com.example.loki.data.repository

// A cached photo as far as syncing is concerned
data class CachedRef(val id: Long, val createdAt: String?)

/**
 * Which cached photos disappeared on the server, judged from one page of the history.
 *
 * A page (newest first) covers a time window: from its first photo (or "now" for page 0) down to its
 * last photo (or the very beginning when it's the last page). Any cached photo inside that window that
 * the page doesn't contain was deleted by its sender. Photos outside the window are left alone.
 * Timestamps are ISO strings, which compare correctly as text; a missing one counts as the oldest.
 */
fun staleIds(cached: List<CachedRef>, page: List<CachedRef>, isFirstPage: Boolean, isLastPage: Boolean): Set<Long> {
    if (page.isEmpty()) {
        // An empty first page = nothing left in this scope; an empty later page gives no window to judge
        return if (isFirstPage) cached.map { it.id }.toSet() else emptySet()
    }
    val pageIds = page.map { it.id }.toSet()
    val newest = page.first().createdAt.orEmpty()
    val oldest = page.last().createdAt.orEmpty()
    return cached.filter { c ->
        val time = c.createdAt.orEmpty()
        val belowTop = isFirstPage || time <= newest
        val aboveBottom = isLastPage || time >= oldest
        c.id !in pageIds && belowTop && aboveBottom
    }.map { it.id }.toSet()
}
