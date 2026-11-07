package com.otium.audio.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.absoluteValue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.QueueMusic
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
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import com.otium.audio.ui.theme.AmbientBackdrop
import kotlinx.coroutines.flow.StateFlow

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.IntOffset

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
import com.otium.audio.ui.theme.rememberTrackColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    positionFlow: StateFlow<Long>,
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
    onReverbChange: (Int) -> Unit = {},
    onReverbPresetChange: (String) -> Unit = {},
    onChorusChange: (Int) -> Unit = {},
    onClarityChange: (Int) -> Unit = {},
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Dynamic Palette extracted from album art
    val trackColors = rememberTrackColors(context, song.albumArtUri)
    val animatedDominant by animateColorAsState(trackColors.dominant, tween(600), label = "dom")

    // A slow halo behind the cover. It deepens while playing and settles when paused,
    // which is the whole animation budget: one float, one curve, no loops of effects.
    val haloTransition = rememberInfiniteTransition(label = "halo")
    val halo by haloTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )
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

    val dragY = remember { Animatable(0f) }

    val playlist = remember(queue, song) {
        if (queue.isNotEmpty()) queue else listOf(song)
    }
    val currentTrackIndex = remember(playlist, song.id) {
        playlist.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
    }

    val pagerState = rememberPagerState(
        initialPage = currentTrackIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0))
    ) {
        playlist.size
    }

    var userSettledPage by remember { mutableIntStateOf(-1) }

    // Keep pager in sync with active playing track when changed externally
    LaunchedEffect(currentTrackIndex) {
        if (playlist.isNotEmpty() &&
            currentTrackIndex in playlist.indices &&
            currentTrackIndex != userSettledPage &&
            pagerState.currentPage != currentTrackIndex &&
            !pagerState.isScrollInProgress
        ) {
            pagerState.animateScrollToPage(
                page = currentTrackIndex,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
        }
        userSettledPage = -1
    }

    // Switch tracks when user settles on a new page via gesture
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            if (playlist.isNotEmpty() &&
                settledPage in playlist.indices &&
                playlist[settledPage].id != song.id
            ) {
                userSettledPage = settledPage
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onPlayQueueIndex(settledPage)
            }
        }
    }

    val playBtnScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.93f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "play_scale"
    )

    val highResArtworkRequest = remember(song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .size(540, 540)
            .crossfade(150)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    val screenHeightPx = with(density) {
        context.resources.displayMetrics.heightPixels.toFloat()
    }
    val dismissThreshold = with(density) { 68.dp.toPx() }
    var isDismissing by remember { mutableStateOf(false) }

    // Ensure initial entry starts clean at offset 0
    LaunchedEffect(Unit) {
        dragY.snapTo(0f)
        isDismissing = false
    }

    fun triggerDismiss() {
        if (isDismissing) return
        isDismissing = true
        scope.launch {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            dragY.animateTo(
                targetValue = screenHeightPx,
                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
            )
            onCollapse()
        }
    }

    val verticalDraggableState = rememberDraggableState { delta ->
        if (!isDismissing) {
            val newY = (dragY.value + delta).coerceAtLeast(0f)
            scope.launch { dragY.snapTo(newY) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, dragY.value.roundToInt()) }
            .graphicsLayer {
                val progress = (dragY.value / (screenHeightPx * 0.45f)).coerceIn(0f, 1f)
                scaleX = 1f - progress * 0.08f
                scaleY = 1f - progress * 0.08f
                alpha = 1f - progress * 0.5f
            }
            .draggable(
                state = verticalDraggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity ->
                    if (dragY.value > dismissThreshold || velocity > 800f) {
                        triggerDismiss()
                    } else {
                        scope.launch {
                            dragY.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioMediumBouncy
                                )
                            )
                        }
                    }
                }
            )
            .background(AmbientBackdrop)
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
                // Centered tactile swipe-down pull bar
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp, bottom = 4.dp)
                        .size(width = 44.dp, height = 4.5.dp)
                        .clip(CircleShape)
                        .background(TextSecondary.copy(alpha = 0.5f))
                        .clickable { triggerDismiss() }
                )

                // ── Minimalist Poweramp Top Bar with swipe-down dismiss ──────────────
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    IconButton(onClick = ::triggerDismiss) {
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
                            text = song.audioFormatBadge,
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
            }

                // ── Cinematic Artwork HorizontalPager Carousel ───────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = showLyrics,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                        label = "art_lyrics"
                    ) { isLyricsMode ->
                        if (isLyricsMode) {
                            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                                LyricsSheet(
                                    lyrics = lyrics,
                                    positionFlow = positionFlow,
                                    onSeekTo = onSeekTo,
                                    activeColor = animatedVibrant
                                )
                            }
                        } else {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                HorizontalPager(
                                    state = pagerState,
                                    userScrollEnabled = playlist.size > 1,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(),
                                    contentPadding = PaddingValues(horizontal = 42.dp),
                                    pageSpacing = 16.dp,
                                    beyondViewportPageCount = 1,
                                    key = { page ->
                                        if (page in playlist.indices) playlist[page].id else page
                                    }
                                ) { page ->
                                    val pageSong = playlist.getOrElse(page) { song }
                                    val isCurrent = page == pagerState.currentPage

                                    // Dynamic 3D carousel depth calculation in unified LTR physics
                                    val rawOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                    val pageOffset = rawOffset.absoluteValue
                                    val pageScale = (1f - (pageOffset * 0.12f)).coerceIn(0.85f, 1f)
                                    val pageAlpha = (1f - (pageOffset * 0.40f)).coerceIn(0.55f, 1f)
                                    val pageRotation = (rawOffset * -10f).coerceIn(-18f, 18f)

                                    val pageCoverRequest = remember(pageSong.albumArtUri) {
                                        ImageRequest.Builder(context)
                                            .data(pageSong.albumArtUri)
                                            .size(540, 540)
                                            .crossfade(150)
                                            .memoryCachePolicy(CachePolicy.ENABLED)
                                            .diskCachePolicy(CachePolicy.ENABLED)
                                            .build()
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .graphicsLayer {
                                                scaleX = pageScale
                                                scaleY = pageScale
                                                alpha = pageAlpha
                                                rotationY = pageRotation
                                                cameraDistance = 16f * density.density
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        // Hardware-accelerated GPU ambient halo glow (zero per-frame allocations)
                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.96f)
                                                    .aspectRatio(1f)
                                                    .scale(1.22f)
                                                    .graphicsLayer {
                                                        alpha = halo * if (isPlaying) 0.85f else 0.35f
                                                    }
                                                    .background(
                                                        brush = Brush.radialGradient(
                                                            colors = listOf(
                                                                animatedVibrant.copy(alpha = 0.50f),
                                                                animatedVibrant.copy(alpha = 0.18f),
                                                                Color.Transparent
                                                            )
                                                        ),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.96f)
                                                .aspectRatio(1f)
                                                .shadow(
                                                    elevation = if (isCurrent) 24.dp else 6.dp,
                                                    shape = RoundedCornerShape(20.dp),
                                                    ambientColor = animatedDominant.copy(alpha = 0.5f),
                                                    spotColor = animatedDominant.copy(alpha = 0.4f)
                                                )
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(DarkSurfaceVariant),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            AudiophileVinylCover()

                                            AsyncImage(
                                                model = pageCoverRequest,
                                                contentDescription = pageSong.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.matchParentSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Track Title & Artist Information with Crossfade ─────────
                val activeDisplaySong = remember(pagerState.currentPage, playlist, song) {
                    if (playlist.isNotEmpty() && pagerState.currentPage in playlist.indices) {
                        playlist[pagerState.currentPage]
                    } else song
                }

                AnimatedContent(
                    targetState = activeDisplaySong,
                    transitionSpec = {
                        if (targetState.id != initialState.id) {
                            (slideInHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { fullWidth -> fullWidth / 5 }
                            ) + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                targetOffsetX = { fullWidth -> -fullWidth / 5 }
                            ) + fadeOut(tween(160)))
                        } else {
                            fadeIn(tween(200)) togetherWith fadeOut(tween(160))
                        }
                    },
                    label = "track_title_crossfade"
                ) { targetSong ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .pointerInput(playlist.size, pagerState.currentPage) {
                                var totalX = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = { totalX = 0f },
                                    onHorizontalDrag = { change, dragAmount ->
                                        change.consume()
                                        totalX += dragAmount
                                    },
                                    onDragEnd = {
                                        val swipeThreshold = 45.dp.toPx()
                                        if (totalX < -swipeThreshold) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSkipNext()
                                        } else if (totalX > swipeThreshold) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSkipPrevious()
                                        }
                                    }
                                )
                            },
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = targetSong.title,
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
                                val cleanArtist = targetSong.artist.replace(Regex("[\\p{C}\\p{Z}]+"), " ").trim()
                                val cleanAlbum = targetSong.album.replace(Regex("[\\p{C}\\p{Z}]+"), " ").trim()

                                fun norm(s: String) = s.replace(Regex("[\\p{C}\\p{Z}|\\-_]+"), "").lowercase()
                                val isSame = norm(cleanArtist).isNotBlank() && norm(cleanArtist) == norm(cleanAlbum)

                                append(if (cleanArtist.isNotBlank()) cleanArtist else "فنان غير معروف")
                                if (cleanAlbum.isNotBlank() &&
                                    !cleanAlbum.equals("<unknown>", ignoreCase = true) &&
                                    !cleanAlbum.equals("unknown", ignoreCase = true) &&
                                    !isSame
                                ) {
                                    append("  •  ")
                                    append(cleanAlbum)
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
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Track Precision Slider ──────────────────────────────────
                TrackSlider(
                    positionFlow = positionFlow,
                    durationMs = durationMs,
                    onSeekTo = onSeekTo,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ── Main Audio Playback Controls ────────────────────────────
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                            text = "خيارات المقطع",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val actions = listOf(
                            Triple(Icons.Default.Lyrics, if (showLyrics) "إخفاء الكلمات" else "عرض الكلمات") {
                                showMoreSheet = false; showLyrics = !showLyrics
                            },
                            Triple(Icons.Default.Speed, "سرعة التشغيل (${playbackSpeed}x)") {
                                showMoreSheet = false; showSpeedDialog = true
                            },
                            Triple(Icons.Default.Bedtime, if (sleepTimerState.isActive) "مؤقت النوم (${sleepTimerState.remainingSeconds} ثانية)" else "مؤقت النوم") {
                                showMoreSheet = false; showSleepTimer = true
                            },
                            Triple(Icons.AutoMirrored.Filled.QueueMusic, "قائمة التشغيل الحالية (${queue.size} مقطع)") {
                                showMoreSheet = false; showQueue = true
                            },
                            Triple(Icons.Default.EditNote, "تعديل بيانات المقطع الصوتي") {
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
                            "سرعة التشغيل",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                FilterChip(
                                    selected = kotlin.math.abs(playbackSpeed - speed) < 0.01f,
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
                    onReverbChange = onReverbChange,
                    onReverbPresetChange = onReverbPresetChange,
                    onChorusChange = onChorusChange,
                    onClarityChange = onClarityChange,
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
