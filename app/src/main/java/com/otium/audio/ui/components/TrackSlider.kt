package com.otium.audio.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.TextSecondary
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

/**
 * Tactile Studio Scrubber Thumb ("العتلة الاحترافية").
 * عتلة نقية ومصقولة بتصميم تيتانيوم فاخر (Titanium White & Obsidian Glass):
 * - قرص أبيض تيتانيوم نقي ومصقول بدقة متناهية
 * - ظل محيطي وبؤري دقيق (Ambient & Spot Drop Shadow) يمنح العتلة عمقاً طبيعياً فاخراً
 * - إطار مجهري خفيف (Hairline rim) يحفظ تباينها وتحديدها المتقن
 * - حركة نابضية مرنة (Spring physics) تتوسع بنعومة فائقة عند السحب أو اللمس
 * - مؤشر دقيق داخلي يظهر أثناء السحب النشط للتحكم المتقن
 */
@Composable
fun PlayerScrubberThumb(
    isDragging: Boolean,
    currentSize: Dp,
    modifier: Modifier = Modifier
) {
    val shadowElevation by animateDpAsState(
        targetValue = if (isDragging) 6.dp else 2.5.dp,
        animationSpec = tween(120),
        label = "thumb_shadow"
    )

    Box(
        modifier = modifier
            .size(currentSize)
            .shadow(
                elevation = shadowElevation,
                shape = CircleShape,
                spotColor = Color(0x99000000),
                ambientColor = Color(0x40000000)
            )
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0x22000000), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isDragging) {
            Box(
                modifier = Modifier
                    .size(4.5.dp)
                    .clip(CircleShape)
                    .background(Color(0x22000000))
            )
        }
    }
}

/**
 * Precision Studio Seekbar (شريط التقدم الاحترافي داخل المشغل الرئيسي).
 * - شريط مكبر وأنيق بسمك 6.5dp في الوضع العادي، ويتوسع بنعومة إلى 9dp أثناء اللمس والتمرير.
 * - عتلة احترافية مصقولة ("العتلة نفسها") بقطر 14.5dp تتوسع بنوابض مرنة إلى 20dp أثناء السحب.
 * - إمكانية التحكم الكاملة: نقر فوري للتقديم/التأخير، وسحب سلس جداً بدون أي تأخير على 120 فريم.
 * - أزرار القفز السريع [-10 ث] و [+10 ث] مع استجابة لمسية.
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
    } else if (durationMs > 0) {
        (position.toFloat() / safeDuration).coerceIn(0f, 1f)
    } else {
        0f
    }

    val displayedPosition = if (isDragging) {
        (dragProgress * safeDuration).toLong()
    } else {
        position
    }

    val restingBarHeight = 6.5.dp
    val draggingBarHeight = 9.dp
    val barHeight by animateDpAsState(
        targetValue = if (isDragging) draggingBarHeight else restingBarHeight,
        animationSpec = tween(120),
        label = "slider_bar_height"
    )

    val restingThumbSize = 14.5.dp
    val draggingThumbSize = 20.dp
    val currentThumbSize by animateDpAsState(
        targetValue = if (isDragging) draggingThumbSize else restingThumbSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "slider_thumb_size"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth()) {
            // Touch interaction zone (44dp comfortable target)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .pointerInput(safeDuration) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val width = size.width.toFloat()
                                if (width <= 0f) continue

                                val thumbBaseRadius = restingThumbSize.toPx() / 2f
                                val usable = (width - 2 * thumbBaseRadius).coerceAtLeast(1f)

                                isDragging = true
                                dragProgress = ((down.position.x - thumbBaseRadius) / usable).coerceIn(0f, 1f)
                                var lastHapticProgress = dragProgress
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                try {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                        if (!change.pressed) {
                                            val targetPosition = if (durationMs > 0) {
                                                (dragProgress * durationMs.toFloat()).toLong().coerceIn(0L, durationMs)
                                            } else 0L
                                            onSeekTo(targetPosition)
                                            change.consume()
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            break
                                        }
                                        val newProgress = ((change.position.x - thumbBaseRadius) / usable).coerceIn(0f, 1f)
                                        if (kotlin.math.abs(newProgress - lastHapticProgress) >= 0.035f) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            lastHapticProgress = newProgress
                                        }
                                        dragProgress = newProgress
                                        change.consume()
                                    }
                                } finally {
                                    isDragging = false
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                val density = LocalDensity.current
                val totalWidthPx = constraints.maxWidth.toFloat()
                val thumbBaseRadiusPx = with(density) { restingThumbSize.toPx() / 2f }
                val usableWidth = (totalWidthPx - 2 * thumbBaseRadiusPx).coerceAtLeast(1f)
                val thumbCenterPx = thumbBaseRadiusPx + currentFraction * usableWidth
                val currentThumbSizePx = with(density) { currentThumbSize.toPx() }

                // 1. Inactive Track (Clean translucent titanium obsidian, rounded pill shape)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(barHeight / 2f))
                        .background(Color(0x33FFFFFF))
                )

                // 2. Active Played Track (Pure Titanium White, rounded pill shape)
                val activeEndPx = if (currentFraction >= 0.999f) totalWidthPx else thumbCenterPx
                val activeTrackWidth = with(density) { activeEndPx.toDp() }
                if (currentFraction > 0.0001f) {
                    Box(
                        modifier = Modifier
                            .width(activeTrackWidth)
                            .height(barHeight)
                            .align(Alignment.CenterStart)
                            .clip(RoundedCornerShape(barHeight / 2f))
                            .background(Color.White)
                    )
                }

                // 3. Tactile Thumb ("العتلة الاحترافية")
                val thumbLeftOffsetPx = (thumbCenterPx - currentThumbSizePx / 2f)
                    .roundToInt()
                    .coerceIn(0, (totalWidthPx - currentThumbSizePx).roundToInt().coerceAtLeast(0))

                Box(
                    modifier = Modifier
                        .offset { IntOffset(x = thumbLeftOffsetPx, y = 0) }
                        .align(Alignment.CenterStart)
                ) {
                    PlayerScrubberThumb(
                        isDragging = isDragging,
                        currentSize = currentThumbSize
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