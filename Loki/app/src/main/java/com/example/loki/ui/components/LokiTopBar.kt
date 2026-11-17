/*
 * TỔNG QUAN FILE: Thanh tiêu đề dùng chung cho các màn có nút quay lại (Chat, Profile, danh sách chat).
 *
 * Thành phần chính:
 * - LokiTopBar: tiêu đề ở giữa, nút quay lại nếu có onBack, và vùng actions bên phải.
 *
 * Lưu ý:
 * - Màn Home không dùng thanh này; Home có TopBar riêng nằm đè lên Pager.
 */
package com.example.loki.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LokiTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        navigationIcon = {
            if (onBack != null) RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Quay lại", onBack)
        },
        actions = actions,
    )
}
