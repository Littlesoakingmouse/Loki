/*
 * TỔNG QUAN FILE: Lưới "Tất cả ảnh" 3 cột; bấm một ảnh để quay lại xem nó toàn màn hình.
 *
 * Thành phần chính:
 * - HistoryGrid: LazyVerticalGrid ảnh thu nhỏ, tự gọi onLoadMore khi cuộn gần cuối.
 *
 * Lưu ý:
 * - Dùng URL thu nhỏ của Cloudinary để ảnh nhẹ và bộ nhớ đệm chứa được nhiều ảnh hơn.
 */
package com.example.loki.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.loki.core.util.thumbnail
import com.example.loki.data.remote.dto.PhotoDto
import com.example.loki.ui.components.RoundIconButton

// "All photos" view: a 3-column grid; tapping a photo jumps back to it in the full-screen history
@Composable
fun HistoryGrid(
    title: String,
    photos: List<PhotoDto>,
    onOpen: (index: Int) -> Unit,
    onClose: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf { (gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= photos.size - 6 }
    }
    LaunchedEffect(nearEnd, photos.size) { if (nearEnd) onLoadMore() }

    Column(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            RoundIconButton(Icons.Rounded.Close, "Đóng", onClose)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(photos, key = { _, p -> p.id }) { index, photo ->
                Box(
                    Modifier.aspectRatio(1f).clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable { onOpen(index) },
                ) {
                    AsyncImage(
                        model = thumbnail(photo.imageUrl, 300),
                        contentDescription = photo.caption ?: "Ảnh của ${photo.sender.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}
