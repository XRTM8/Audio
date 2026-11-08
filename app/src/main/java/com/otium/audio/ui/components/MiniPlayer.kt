package com.otium.audio.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
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


/**
 * Poweramp-grade Full-Width Flat Bottom Console Player.
 * Large comfortable touch target seek strip, enlarged artwork, expanded typography,
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
            .background(DarkSurfaceContainer)
            .clickable { onPlayerClick() }
    ) {
        // Zero-Recomposition isolated interactive seek strip (only updates waveform/thumb without recomposing row)
        MiniPlayerTactileSeekStrip(
            positionFlow = positionFlow,
            durationMs = durationMs,
            isPlaying = isPlaying,
            onSeekTo = onSeekTo,
            waveformSeed = currentSong.id
        )

        // Enlarged console row (72dp height, 54dp album art, full playback controls)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enlarged Album Art
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant),
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

                    // Tactile Titanium White Play/Pause Circle
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .graphicsLayer {
                                scaleX = btnScale
                                scaleY = btnScale
                            }
                            .shadow(elevation = 6.dp, shape = CircleShape, spotColor = Color(0x66FFFFFF))
                            .clip(CircleShape)
                            .background(Color.White),
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
                                tint = Color.Black,
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

@Composable
private fun MiniPlayerTactileSeekStrip(
    positionFlow: StateFlow<Long>,
    durationMs: Long,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    waveformSeed: Long = 0L
) {
    val haptic = LocalHapticFeedback.current
    val position by positionFlow.collectAsState()

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(0f) }

    val rawProgress = if (durationMs > 0)
        (position.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    else 0f

    val activeProgress = if (isScrubbing) scrubProgress else rawProgress

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .pointerInput(durationMs) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        if (size.width > 0) {
                            isScrubbing = true
                            scrubProgress = (down.position.x / size.width).coerceIn(0f, 1f)
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            if (size.width > 0) {
                                val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                if (kotlin.math.abs(newProgress - scrubProgress) > 0.005f) {
                                    scrubProgress = newProgress
                                }
                            }
                        }
                        if (durationMs > 0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSeekTo((scrubProgress * durationMs).toLong())
                        }
                        isScrubbing = false
                    }
                }
        ) {
            // Tactile Waveform
            ReactiveWaveform(
                progress = activeProgress,
                accent = Color.White,
                trackColor = DarkCardBorder.copy(alpha = 0.5f),
                isPlaying = isPlaying,
                waveformSeed = waveformSeed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isScrubbing) 15.dp else 12.dp)
                    .align(Alignment.Center)
            )

            // Scrubber Head / Glowing Thumb Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .align(Alignment.Center)
            ) {
                val thumbSize = if (isScrubbing) 12.dp else 7.dp
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            val halfPx = thumbSize.toPx() / 2f
                            translationX = (activeProgress * size.width - halfPx)
                                .coerceIn(0f, (size.width - thumbSize.toPx()).coerceAtLeast(0f))
                        }
                        .size(thumbSize)
                        .shadow(
                            elevation = if (isScrubbing) 6.dp else 2.dp,
                            shape = CircleShape,
                            spotColor = Color.White
                        )
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }

            // Floating Live Scrub Time Indicator Pill
            if (isScrubbing && durationMs > 0) {
                val scrubMs = (scrubProgress * durationMs).toLong()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-30).dp)
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${Song.formatDurationFast(scrubMs)} / ${Song.formatDurationFast(durationMs)}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
