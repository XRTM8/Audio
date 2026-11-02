package com.otium.audio.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.otium.audio.data.lyrics.LrcParser
import com.otium.audio.data.lyrics.TrackLyrics
import com.otium.audio.data.model.Song
import com.otium.audio.playback.EqualizerState
import com.otium.audio.playback.SleepTimerState
import com.otium.audio.ui.components.EqualizerSheet
import com.otium.audio.ui.components.LyricsSheet
import com.otium.audio.ui.components.QueueSheet
import com.otium.audio.ui.components.SleepTimerDialog
import com.otium.audio.ui.components.TrackSlider
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentGoldDim
import com.otium.audio.ui.theme.AccentGoldLight
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
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
    onPreAmpChange: (Float) -> Unit = {},
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

    // Dynamic Palette extracted from album art
    val trackColors = rememberTrackColors(context, song.albumArtUri)
    val animatedDominant by animateColorAsState(trackColors.dominant, tween(600), label = "dom")
    val animatedVibrant  by animateColorAsState(trackColors.vibrant,  tween(600), label = "vib")

    // State for Sheets & Overlays
    var showMoreSheet   by remember { mutableStateOf(false) }
    var showLyrics      by remember { mutableStateOf(false) }
    var showEqualizer   by remember { mutableStateOf(false) }
    var showQueue       by remember { mutableStateOf(false) }
    var showSleepTimer  by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val lyrics = remember(song.relativePath) {
        LrcParser.findLrcForAudio(song.relativePath) ?: TrackLyrics()
    }

    val dragX = remember { Animatable(0f) }

    val playBtnScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.93f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "play_scale"
    )

    val highResArtworkRequest = remember(song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .crossfade(200)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkSurface)
        ) {
            // Ambient Aura Glow behind artwork
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                animatedDominant.copy(alpha = 0.32f),
                                animatedVibrant.copy(alpha = 0.08f),
                                DarkSurface.copy(alpha = 0f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Minimalist Poweramp Top Bar ──────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = TextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Lossless Audio Badge Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceContainer.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${song.audioFormatBadge} • 32-BIT FLOAT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = AccentGold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite()
                        }) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) ErrorRed else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = { showEqualizer = true }) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Equalizer",
                                tint = if (equalizerState.isEnabled) AccentGold else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(onClick = { showMoreSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // ── Large Cinematic Artwork with Swipe-To-Skip Physics ───────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = showLyrics,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "art_lyrics"
                    ) { isLyricsMode ->
                        if (isLyricsMode) {
                            LyricsSheet(
                                lyrics = lyrics,
                                currentPositionMs = currentPositionMs,
                                onSeekTo = onSeekTo,
                                activeColor = animatedVibrant
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.92f)
                                    .aspectRatio(1f)
                                    .offset { IntOffset(dragX.value.roundToInt(), 0) }
                                    .shadow(
                                        elevation = 28.dp,
                                        shape = RoundedCornerShape(18.dp),
                                        ambientColor = animatedDominant.copy(alpha = 0.5f),
                                        spotColor = animatedDominant.copy(alpha = 0.4f)
                                    )
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(DarkSurfaceVariant)
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragEnd = {
                                                scope.launch {
                                                    if (dragX.value > 160f) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onSkipPrevious()
                                                    } else if (dragX.value < -160f) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onSkipNext()
                                                    }
                                                    dragX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                                                }
                                            },
                                            onHorizontalDrag = { _, amount ->
                                                scope.launch {
                                                    dragX.snapTo(dragX.value + amount * 0.55f)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // High-End Audiophile Vinyl Placeholder if artwork is absent or loading
                                AudiophileVinylCover()

                                AsyncImage(
                                    model = highResArtworkRequest,
                                    contentDescription = song.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )
                            }
                        }
                    }
                }

                // ── Track Title & Artist Information ────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textDirection = TextDirection.ContentOrLtr
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(song.artist)
                            if (song.album.isNotBlank() && song.album != "<unknown>") {
                                append("  •  ")
                                append(song.album)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            textDirection = TextDirection.ContentOrLtr,
                            fontSize = 13.5.sp
                        ),
                        color = TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Track Precision Slider ──────────────────────────────────
                TrackSlider(
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── Main Audio Playback Controls ────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
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

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipPrevious()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = TextPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Tactile Master Play / Pause
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(playBtnScale)
                            .shadow(
                                elevation = 14.dp,
                                shape = CircleShape,
                                spotColor = AccentGold.copy(alpha = 0.45f)
                            )
                            .clip(CircleShape)
                            .background(AccentGold),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPlayPause()
                            },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = DarkSurface,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSkipNext()
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
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

                Spacer(modifier = Modifier.height(10.dp))
            }

            // ── More Actions Sheet ──────────────────────────────────────────
            if (showMoreSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = DarkSurfaceElevated,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Track Actions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val actions = listOf(
                            Triple(Icons.Default.Lyrics, if (showLyrics) "Hide Lyrics" else "Synchronized Lyrics") {
                                showMoreSheet = false; showLyrics = !showLyrics
                            },
                            Triple(Icons.Default.Speed, "Playback Speed  (${playbackSpeed}x)") {
                                showMoreSheet = false; showSpeedDialog = true
                            },
                            Triple(Icons.Default.Bedtime, if (sleepTimerState.isActive) "Sleep Timer (${sleepTimerState.remainingSeconds}s remaining)" else "Sleep Timer") {
                                showMoreSheet = false; showSleepTimer = true
                            },
                            Triple(Icons.Default.QueueMusic, "Up Next Queue  (${queue.size} tracks)") {
                                showMoreSheet = false; showQueue = true
                            },
                            Triple(Icons.Default.EditNote, "Edit Audio Tag Metadata") {
                                showMoreSheet = false; onEditTagClick()
                            }
                        )

                        actions.forEach { (icon, label, action) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { action() }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = AccentGold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.35f))
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // ── Speed Dialog ────────────────────────────────────────────────
            if (showSpeedDialog) {
                ModalBottomSheet(
                    onDismissRequest = { showSpeedDialog = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = DarkSurfaceElevated,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Playback Speed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                FilterChip(
                                    selected = playbackSpeed == speed,
                                    onClick = { onSetPlaybackSpeed(speed); showSpeedDialog = false },
                                    label = { Text("${speed}x", fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentGold,
                                        selectedLabelColor = DarkSurface,
                                        containerColor = DarkSurfaceContainer,
                                        labelColor = TextSecondary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // ── Equalizer Sheet ─────────────────────────────────────────────
            if (showEqualizer) {
                EqualizerSheet(
                    state = equalizerState,
                    onToggleEnabled = onToggleEqualizer,
                    onBandLevelChange = onEqualizerBandChange,
                    onBassBoostChange = onBassBoostChange,
                    onVirtualizerChange = onVirtualizerChange,
                    onPresetSelected = onEqualizerPresetSelected,
                    onPreAmpChange = onPreAmpChange,
                    onDismiss = { showEqualizer = false }
                )
            }

            // ── Queue Sheet ─────────────────────────────────────────────────
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

            // ── Sleep Timer Dialog ──────────────────────────────────────────
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
}

@Composable
private fun AudiophileVinylCover() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        // Outer concentric groove rings
        listOf(0.88f, 0.74f, 0.60f, 0.46f).forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxSize(fraction)
                    .clip(CircleShape)
                    .border(1.dp, DarkCardBorder.copy(alpha = 0.6f), CircleShape)
            )
        }

        // Center Gold Label
        Box(
            modifier = Modifier
                .fillMaxSize(0.32f)
                .clip(CircleShape)
                .background(AccentGoldDim)
                .border(1.5.dp, AccentGold, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = AccentGoldLight,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
