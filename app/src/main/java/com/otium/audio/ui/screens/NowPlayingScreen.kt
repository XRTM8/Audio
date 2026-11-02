package com.otium.audio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.otium.audio.data.lyrics.LrcParser
import com.otium.audio.data.lyrics.TrackLyrics
import com.otium.audio.data.model.Song
import com.otium.audio.playback.EqualizerState
import com.otium.audio.playback.SleepTimerState
import com.otium.audio.ui.components.AudioWaveformVisualizer
import com.otium.audio.ui.components.EqualizerSheet
import com.otium.audio.ui.components.LyricsSheet
import com.otium.audio.ui.components.QueueSheet
import com.otium.audio.ui.components.SleepTimerDialog
import com.otium.audio.ui.components.TrackSlider
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.rememberTrackColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: Int,
    isShuffleEnabled: Boolean,
    playbackSpeed: Float,
    equalizerState: EqualizerState,
    queue: List<Song> = emptyList(),
    sleepTimerState: SleepTimerState = SleepTimerState(),
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetPlaybackSpeed: (Float) -> Unit,
    onToggleEqualizer: (Boolean) -> Unit,
    onEqualizerBandChange: (Short, Float) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onEqualizerPresetSelected: (String) -> Unit,
    onPlayQueueIndex: (Int) -> Unit = {},
    onRemoveQueueItem: (Int) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onStartSleepTimer: (Int) -> Unit = {},
    onStartSleepEndOfTrack: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // 1. Dynamic Palette Extraction
    val trackColors = rememberTrackColors(context, song.albumArtUri)
    val animatedDominant by animateColorAsState(trackColors.dominant, tween(600), label = "dominant_color")
    val animatedVibrant by animateColorAsState(trackColors.vibrant, tween(600), label = "vibrant_color")

    // 2. Sheets / Views State
    var showLyrics by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Synchronized Lyrics loader
    val lyrics = remember(song.relativePath) {
        LrcParser.findLrcForAudio(song.relativePath) ?: TrackLyrics()
    }

    // 3. Album Art Horizontal Swipe Gesture (Physics spring)
    val dragOffsetX = remember { Animatable(0f) }

    // 4. Play/Pause Button Pulse Animation
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "play_btn_scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedDominant.copy(alpha = 0.55f),
                        DarkSurfaceVariant.copy(alpha = 0.85f),
                        DarkSurface
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = TextPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Lyrics Toggle Button
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = "Lyrics",
                            tint = if (showLyrics) animatedVibrant else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Sleep Timer Button
                    IconButton(onClick = { showSleepTimer = true }) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepTimerState.isActive) animatedVibrant else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Queue Button
                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Queue",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Equalizer Button
                    IconButton(onClick = { showEqualizer = true }) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = if (equalizerState.isEnabled) animatedVibrant else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Speed Button
                    IconButton(onClick = { showSpeedDialog = !showSpeedDialog }) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Speed",
                            tint = if (playbackSpeed != 1.0f) animatedVibrant else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Favorite Button
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleFavorite()
                    }) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) ErrorRed else TextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Speed Selection Dropdown / Pill Row (Animated)
            AnimatedVisibility(visible = showSpeedDialog) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSetPlaybackSpeed(speed)
                                showSpeedDialog = false
                            },
                            label = { Text("${speed}x") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = animatedVibrant,
                                selectedLabelColor = DarkSurface
                            ),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            // Center Content: Artwork or Lyrics Sheet
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showLyrics,
                    transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                    label = "center_content_switch"
                ) { isLyricsMode ->
                    if (isLyricsMode) {
                        LyricsSheet(
                            lyrics = lyrics,
                            currentPositionMs = currentPositionMs,
                            onSeekTo = onSeekTo,
                            activeColor = animatedVibrant
                        )
                    } else {
                        // Large Artwork with Swipe Physics
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                                .clip(RoundedCornerShape(28.dp))
                                .background(DarkSurfaceVariant)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures(
                                        onDragEnd = {
                                            scope.launch {
                                                val threshold = 180f
                                                if (dragOffsetX.value > threshold) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onSkipPrevious()
                                                } else if (dragOffsetX.value < -threshold) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onSkipNext()
                                                }
                                                dragOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                            }
                                        },
                                        onHorizontalDrag = { _, dragAmount ->
                                            scope.launch {
                                                dragOffsetX.snapTo(dragOffsetX.value + dragAmount * 0.6f)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = song.albumArtUri,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = animatedVibrant,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Waveform Visualizer
            AudioWaveformVisualizer(
                isPlaying = isPlaying,
                primaryColor = animatedVibrant,
                secondaryColor = animatedDominant.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Song Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Smooth Track Slider
            TrackSlider(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onSeekTo = onSeekTo
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Playback Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffleEnabled) animatedVibrant else TextSecondary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSkipPrevious()
                }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Play / Pause Circle with Bouncing Animation
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(playButtonScale)
                        .clip(CircleShape)
                        .background(animatedVibrant),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlayPause()
                        },
                        modifier = Modifier.size(76.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DarkSurface,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSkipNext()
                }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(onClick = onToggleRepeat) {
                    val (icon, tint) = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Pair(Icons.Default.RepeatOne, animatedVibrant)
                        Player.REPEAT_MODE_ALL -> Pair(Icons.Default.Repeat, animatedVibrant)
                        else -> Pair(Icons.Default.Repeat, TextSecondary)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = "Repeat",
                        tint = tint,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        // Equalizer Modal Bottom Sheet
        if (showEqualizer) {
            EqualizerSheet(
                state = equalizerState,
                onToggleEnabled = onToggleEqualizer,
                onBandLevelChange = onEqualizerBandChange,
                onBassBoostChange = onBassBoostChange,
                onVirtualizerChange = onVirtualizerChange,
                onPresetSelected = onEqualizerPresetSelected,
                onDismiss = { showEqualizer = false }
            )
        }

        // Queue / Up Next Modal Bottom Sheet
        if (showQueue) {
            QueueSheet(
                queue = queue,
                currentSong = song,
                onPlayTrackAt = onPlayQueueIndex,
                onRemoveTrackAt = onRemoveQueueItem,
                onMoveTrack = onMoveQueueItem,
                onDismiss = { showQueue = false }
            )
        }

        // Sleep Timer Dialog
        if (showSleepTimer) {
            SleepTimerDialog(
                state = sleepTimerState,
                onStartTimer = onStartSleepTimer,
                onStartEndOfTrack = onStartSleepEndOfTrack,
                onCancelTimer = onCancelSleepTimer,
                onDismiss = { showSleepTimer = false }
            )
        }
    }
}
