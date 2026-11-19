package com.otium.audio.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.roundToInt


/**
 * Poweramp-grade Full-Width Flat Bottom Console Player.
 * Large comfortable touch target seek strip with tactile thumb, enlarged artwork, expanded typography,
 * full playback controls (SkipPrevious, Play/Pause, SkipNext) and zero-allocation scrubber.
 */
@Composable
fun MiniPlayer(
    currentSong: Song,
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>,
    durationMs: Long,
    onPlayPauseClick: () -> Unit,
    onSkipNextClick: () -> Unit,
    onPlayerClick: () -> Unit,
    onSkipPreviousClick: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val hasAlbumArt = currentSong.albumArtUri != android.net.Uri.EMPTY
    val thumbReq = if (hasAlbumArt) {
        remember(currentSong.albumArtUri) {
            ImageRequest.Builder(context)
                .data(currentSong.albumArtUri)
                .size(160, 160)
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    } else null

    val btnScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.94f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "mini_btn_scale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface) // Pure AMOLED Black (#000000)
            .clickable { onPlayerClick() }
    ) {
        // Specular hairline divider to cleanly separate from song list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.6.dp)
                .background(DarkCardBorder)
        )

        // Precision progress bar with tactile thumb ("العتلة الاحترافية")
        MiniPlayerProgressBar(
            positionFlow = positionFlow,
            durationMs = durationMs,
            onSeekTo = onSeekTo
        )

        // Console row (70dp height, 52dp album art, full playback controls)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(0.6.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(24.dp)
                )

                if (thumbReq != null) {
                    AsyncImage(
                        model = thumbReq,
                        contentDescription = currentSong.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Artist
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentSong.displayTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textDirection = TextDirection.ContentOrLtr
                    ),
                    color = TextPrimary,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${currentSong.artist}  ·  ${currentSong.audioFormatBadge}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        textDirection = TextDirection.ContentOrLtr,
                        fontSize = 12.sp
                    ),
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Playback controls cluster in fixed LTR order
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Skip Previous Flat Icon
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipPreviousClick()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    val isDark = LocalOtiumColors.current.isDark
                    // Tactile Play/Pause Circle
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .graphicsLayer {
                                scaleX = btnScale
                                scaleY = btnScale
                            }
                            .shadow(
                                elevation = 6.dp,
                                shape = CircleShape,
                                spotColor = if (isDark) Color(0x66FFFFFF) else Color(0x33000000)
                            )
                            .clip(CircleShape)
                            .background(if (isDark) Color.White else Color(0xFF101217)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlayPauseClick()
                            },
                            modifier = Modifier.size(46.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (isDark) Color.Black else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Skip Next Flat Icon
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipNextClick()
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Studio Precision Progress Bar with Tactile Scrubber Thumb ("شريط المشغل المصغر مع العتلة الاحترافية").
 * - شريط مكبر بسمك 5dp في الوضع العادي، ويتوسع بنعومة إلى 7dp أثناء اللمس والتمرير.
 * - عتلة احترافية مصقولة ("العتلة نفسها") بقطر 12.5dp تتوسع بنوابض مرنة إلى 16.5dp أثناء السحب.
 * - إمكانية التقديم والتأخير الفوري بالنقر أو السحب السلس بدون تأخير وبدون تعارض مع نقر فتح المشغل.
 */
@Composable
private fun MiniPlayerProgressBar(
    positionFlow: StateFlow<Long>,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
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

    val restingBarHeight = 2.5.dp
    val draggingBarHeight = 5.dp
    val barHeight by animateDpAsState(
        targetValue = if (isDragging) draggingBarHeight else restingBarHeight,
        animationSpec = tween(120),
        label = "mini_bar_height"
    )

    val restingThumbSize = 8.dp
    val draggingThumbSize = 15.dp
    val currentThumbSize by animateDpAsState(
        targetValue = if (isDragging) draggingThumbSize else restingThumbSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mini_thumb_size"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .height(18.dp)
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
                                    dragProgress = ((change.position.x - thumbBaseRadius) / usable).coerceIn(0f, 1f)
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

            val isDark = LocalOtiumColors.current.isDark

            // 1. Inactive Track (Clean translucent titanium obsidian or subtle slate)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(barHeight / 2f))
                    .background(if (isDark) Color(0x22FFFFFF) else Color(0xFFDDE1E8))
            )

            // 2. Active Played Track
            val activeEndPx = if (currentFraction >= 0.999f) totalWidthPx else thumbCenterPx
            val activeTrackWidth = with(density) { activeEndPx.toDp() }
            if (currentFraction > 0.0001f) {
                Box(
                    modifier = Modifier
                        .width(activeTrackWidth)
                        .height(barHeight)
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(barHeight / 2f))
                        .background(if (isDark) Color.White else Color(0xFF101217))
                )
            }

            // 3. Tactile Thumb ("العتلة الاحترافية نفسها")
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
    }
}
