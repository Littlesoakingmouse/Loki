/*
 * TỔNG QUAN FILE: Hiệu ứng emoji bay lên khi thả cảm xúc.
 *
 * Thành phần chính:
 * - EmojiBurst: 14 hạt emoji với vị trí, độ trễ và kích thước ngẫu nhiên, mỗi hạt có Animatable riêng.
 * - Particle (private): thông số của một hạt.
 *
 * Lưu ý:
 * - key đổi sau mỗi lần bấm, nhờ đó thả cùng một emoji hai lần vẫn chạy lại hiệu ứng.
 * - Dùng graphicsLayer để đổi vị trí, độ mờ và kích thước mà không phải dựng lại layout.
 */
package com.example.loki.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// A burst of the chosen emoji floating up the screen, Locket's reaction feedback.
// key changes on every tap, so the same emoji twice still replays
@Composable
fun EmojiBurst(emoji: String?, key: Int, modifier: Modifier = Modifier) {
    if (emoji == null) return
    BoxWithConstraints(modifier.fillMaxSize()) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val particles = remember(key) { List(14) { Particle(Random.nextFloat(), Random.nextFloat(), Random.nextInt(0, 350)) } }
        particles.forEach { p ->
            val progress = remember(key) { Animatable(0f) }
            LaunchedEffect(key) {
                delay(p.delayMs.toLong())
                progress.animateTo(1f, tween(1_400, easing = FastOutSlowInEasing))
            }
            Box(
                Modifier.graphicsLayer {
                    translationX = p.x * (widthPx - 120f) + 20f
                    translationY = heightPx * (1f - progress.value * (0.6f + p.spread * 0.4f))
                    alpha = if (progress.value == 0f) 0f else 1f - progress.value
                    val scale = 0.8f + p.spread * 0.8f
                    scaleX = scale
                    scaleY = scale
                },
            ) { Text(emoji, fontSize = 36.sp) }
        }
    }
}

private class Particle(val x: Float, val spread: Float, val delayMs: Int)
