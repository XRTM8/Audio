package com.otium.audio.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.TextSecondary
import kotlinx.coroutines.flow.StateFlow

/**
 * Precision Main Player Seekbar with Permanent Tactile Handle (المشغل الرئيسي - شريط بمقبض تحكم).
 * - Straight, clean track (4dp) with pure white progress.
 * - Always-visible solid white handle (مقبض تحكم دائم 14dp يتوسع لـ 18dp عند اللمس).
 * - Instant 120 FPS unified touch gesture loop with zero lag.
 */
@Composable
fun TrackSlider(
    positionFlow: StateFlow<Long>,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onSeekBy: ((Long) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val position by positionFlow.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val safeDuration = if (durationMs > 0) durationMs.toFloat() else 1f
    val currentFraction = if (isDragging) {
        dragProgress
    } else {
        (position.toFloat() / safeDuration).coerceIn(0f, 1f)
    }

    val displayedPosition = if (isDragging) {
        (dragProgress * safeDuration).toLong()
    } else {
        position
    }

    val thumbSize by animateDpAsState(
        targetValue = if (isDragging) 18.dp else 14.dp,
        animationSpec = tween(120),
        label = "slider_thumb_size"
    )

    val trackHeight = 4.dp

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth()) {
            // Interactive Touch Target Band (38dp comfortable touch height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .pointerInput(safeDuration) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val width = size.width.toFloat()
                            if (width <= 0f) return@awaitEachGesture

                            isDragging = true
                            dragProgress = (down.position.x / width).coerceIn(0f, 1f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (change.changedToUp()) {
                                    val targetPosition = (dragProgress * safeDuration).toLong()
                                    onSeekTo(targetPosition)
                                    isDragging = false
                                    change.consume()
                                    break
                                } else if (!change.pressed) {
                                    isDragging = false
                                    break
                                } else {
                                    dragProgress = (change.position.x / width).coerceIn(0f, 1f)
                                    change.consume()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // 1. Inactive Track (Subtle translucent white background)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight)
                        .clip(RoundedCornerShape(trackHeight / 2f))
                        .background(Color(0x2EFFFFFF))
                ) {
                    // 2. Active Played Track (Pure Titanium White)
                    if (currentFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(currentFraction)
                                .clip(RoundedCornerShape(trackHeight / 2f))
                                .background(Color.White)
                        )
                    }
                }

                // 3. Permanent Tactile Handle (المقبض الدائم - دائماً مرئي، يتفاعل بنعومة مع السحب)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .graphicsLayer {
                                val currentThumbPx = thumbSize.toPx()
                                val halfPx = currentThumbPx / 2f
                                translationX = (currentFraction * size.width - halfPx)
                                    .coerceIn(0f, (size.width - currentThumbPx).coerceAtLeast(0f))
                            }
                            .size(thumbSize)
                            .shadow(
                                elevation = if (isDragging) 6.dp else 3.dp,
                                shape = CircleShape,
                                spotColor = Color.White.copy(alpha = 0.6f)
                            )
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            // Time & Quick Jump Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(displayedPosition),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isDragging) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isDragging) Color.White else TextSecondary
                )

                // Quick Seek Jump Controls: [-10 ث] and [+10 ث]
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (onSeekBy != null) {
                                    onSeekBy(-10_000L)
                                } else {
                                    onSeekTo((displayedPosition - 10_000L).coerceAtLeast(0L))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "-10 ث",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (onSeekBy != null) {
                                    onSeekBy(10_000L)
                                } else {
                                    onSeekTo((displayedPosition + 10_000L).coerceAtMost(durationMs))
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+10 ث",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Text(
                    text = formatDuration(durationMs),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextSecondary
                )
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs.coerceAtLeast(0L) / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val minStr = if (minutes < 10) "0$minutes" else minutes.toString()
    val secStr = if (seconds < 10) "0$seconds" else seconds.toString()
    return "$minStr:$secStr"
}
