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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.SubcomposeAsyncImage
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
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.rememberTrackColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
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
    onEditTagClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Dynamic Palette
    val trackColors = rememberTrackColors(context, song.albumArtUri)
    val animatedDominant by animateColorAsState(trackColors.dominant, tween(500), label = "dominant_color")
    val animatedVibrant by animateColorAsState(trackColors.vibrant, tween(500), label = "vibrant_color")

    // State for Sheets & Overlays
    var showMoreActionsSheet by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    // Synchronized Lyrics loader
    val lyrics = remember(song.relativePath) {
        LrcParser.findLrcForAudio(song.relativePath) ?: TrackLyrics()
    }

    // Artwork Swipe Physics
    val dragOffsetX = remember { Animatable(0f) }

    // Play/Pause Button Pulse Animation
    val playButtonScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "play_btn_scale"
    )

    // SOLID OPAQUE BASE to prevent any bleeding of underlying HomeScreen
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkSurface)
    ) {
        // Ambient Radial/Vertical Glow behind artwork
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            animatedDominant.copy(alpha = 0.35f),
                            DarkSurfaceVariant.copy(alpha = 0.5f),
                            DarkSurface
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Poweramp Minimalist Top Bar (Fixed LTR to eliminate RTL overlapping)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Lossless Audio Engine Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceContainer.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "HI-RES AUDIO • 32-BIT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AccentGold
                        )
                    }

                    // Right Actions: Favorite, Equalizer, and Clean More Menu
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite()
                        }) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) ErrorRed else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { showEqualizer = true }) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Equalizer",
                                tint = if (equalizerState.isEnabled) AccentGold else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(onClick = { showMoreActionsSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Center Content: Artwork or Lyrics Sheet
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = showLyrics,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
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
                        // Large Luxury Artwork with Swipe Physics
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .offset { IntOffset(dragOffsetX.value.roundToInt(), 0) }
                                .clip(RoundedCornerShape(24.dp))
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
                            SubcomposeAsyncImage(
                                model = song.albumArtUri,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize(),
                                error = {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(DarkSurfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = AccentGold.copy(alpha = 0.5f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Waveform Visualizer
            AudioWaveformVisualizer(
                isPlaying = isPlaying,
                primaryColor = animatedVibrant,
                secondaryColor = animatedDominant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Song Info (Title & Artist)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDirection = TextDirection.ContentOrLtr
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${song.artist} • ${song.album}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDirection = TextDirection.ContentOrLtr
                    ),
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Smooth Track Slider
            TrackSlider(
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onSeekTo = onSeekTo
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Main Playback Controls
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleShuffle) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (isShuffleEnabled) AccentGold else TextSecondary,
                            modifier = Modifier.size(24.dp)
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
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Play / Pause Circle with Bouncing Animation
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .scale(playButtonScale)
                            .clip(CircleShape)
                            .background(AccentGold),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlayPause()
                            },
                            modifier = Modifier.size(74.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkSurface,
                                modifier = Modifier.size(38.dp)
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
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    IconButton(onClick = onToggleRepeat) {
                        val (icon, tint) = when (repeatMode) {
                            Player.REPEAT_MODE_ONE -> Pair(Icons.Default.RepeatOne, AccentGold)
                            Player.REPEAT_MODE_ALL -> Pair(Icons.Default.Repeat, AccentGold)
                            else -> Pair(Icons.Default.Repeat, TextSecondary)
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // More Actions Modal Bottom Sheet
        if (showMoreActionsSheet) {
            val actionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showMoreActionsSheet = false },
                sheetState = actionsSheetState,
                containerColor = DarkSurfaceElevated,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Track Actions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Synchronized Lyrics
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreActionsSheet = false
                                showLyrics = !showLyrics
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Lyrics, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (showLyrics) "Hide Lyrics" else "View Synchronized Lyrics", color = TextPrimary)
                    }

                    // 2. Playback Speed
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreActionsSheet = false
                                showSpeedDialog = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Playback Speed (${playbackSpeed}x)", color = TextPrimary)
                    }

                    // 3. Sleep Timer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreActionsSheet = false
                                showSleepTimer = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(if (sleepTimerState.isActive) "Sleep Timer Active (${sleepTimerState.remainingSeconds}s)" else "Set Sleep Timer", color = TextPrimary)
                    }

                    // 4. Playing Queue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreActionsSheet = false
                                showQueue = true
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.QueueMusic, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Up Next Queue (${queue.size} tracks)", color = TextPrimary)
                    }

                    // 5. Edit Tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showMoreActionsSheet = false
                                onEditTagClick()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.EditNote, contentDescription = null, tint = AccentGold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Edit Audio Tag Metadata", color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }

        // Speed Selection Dialog
        if (showSpeedDialog) {
            val speedSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showSpeedDialog = false },
                sheetState = speedSheetState,
                containerColor = DarkSurfaceElevated
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Playback Speed",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
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
                                    selectedContainerColor = AccentGold,
                                    selectedLabelColor = DarkSurface
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
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
