package com.otium.audio.ui.components.nowplaying

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary

@Composable
fun NowPlayingTopBar(
    audioFormatBadge: String,
    isFavorite: Boolean,
    isEqualizerEnabled: Boolean,
    onCollapse: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenMore: () -> Unit,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
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
                    text = audioFormatBadge,
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
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) ErrorRed else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onOpenEqualizer) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = if (isEqualizerEnabled) AccentGold else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = onOpenMore) {
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
}

@Composable
fun NowPlayingControls(
    isPlaying: Boolean,
    repeatMode: Int,
    isShuffleEnabled: Boolean,
    activeAccent: Color,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleShuffle: () -> Unit,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    val playBtnScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.93f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "play_scale"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleRepeat()
            }) {
                Icon(
                    imageVector = when (repeatMode) {
                        Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                        Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                        else -> Icons.Default.Repeat
                    },
                    contentDescription = "Repeat",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) activeAccent else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
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

            // Play/Pause tactile circular capsule
            Box(
                modifier = Modifier
                    .scale(playBtnScale)
                    .size(68.dp)
                    .shadow(16.dp, CircleShape, spotColor = activeAccent.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(activeAccent)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp)
                )
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

            IconButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onToggleShuffle()
            }) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled) activeAccent else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
