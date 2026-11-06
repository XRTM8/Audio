package com.otium.audio.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentGoldMuted
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary

/**
 * Ultra-optimized 120 FPS Audiophile Track Row.
 * Fixed 66dp height eliminates layout remeasuring passes during high-speed fling scrolling.
 * Zero Card allocations, hardware-accelerated memory cache for album artwork.
 */
@Composable
fun SongListItem(
    song: Song,
    isCurrentSong: Boolean,
    onSongClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEditTagClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Memory-pooled thumbnail request with crossfade disabled for zero-jank 120Hz flings
    val thumbnailRequest = remember(song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .size(140, 140)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(66.dp)
                .background(if (isCurrentSong) AccentGoldMuted else DarkSurfaceVariant)
                .clickable { onSongClick() }
        ) {
            // Left active amber glow bar for playing track
            if (isCurrentSong) {
                Box(
                    modifier = Modifier
                        .width(3.5.dp)
                        .fillMaxHeight()
                        .align(Alignment.CenterStart)
                        .background(AccentGold)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isCurrentSong) 16.dp else 12.dp,
                        end = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art (48dp, 6dp radius)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(20.dp)
                    )

                    AsyncImage(
                        model = thumbnailRequest,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    if (isCurrentSong) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(DarkSurface.copy(alpha = 0.60f)),
                            contentAlignment = Alignment.Center
                        ) {
                            MiniEqBars()
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Title + Audio Format Pill + Artist + Duration
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            textDirection = TextDirection.ContentOrLtr
                        ),
                        color = if (isCurrentSong) AccentGold else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Hi-Res / Audio Format Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (song.audioFormatBadge == "FLAC" || song.audioFormatBadge == "WAV") AccentCyan.copy(alpha = 0.2f)
                                    else DarkSurfaceContainer
                                )
                                .padding(horizontal = 4.dp, vertical = 0.5.dp)
                        ) {
                            Text(
                                text = song.audioFormatBadge,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (song.audioFormatBadge == "FLAC" || song.audioFormatBadge == "WAV") AccentCyan else TextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "${song.artist}  •  ${song.formattedDuration}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                textDirection = TextDirection.ContentOrLtr,
                                fontSize = 11.5.sp
                            ),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick Favorite Icon
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) ErrorRed else DarkCardBorder,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Hairline divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(DarkCardBorder.copy(alpha = 0.25f))
            )
        }

}

@Composable
fun MiniEqBars() {
    val inf = rememberInfiniteTransition(label = "eq_bars")
    val b1 by inf.animateFloat(
        initialValue = 0.15f, targetValue = 0.90f,
        animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "b1"
    )
    val b2 by inf.animateFloat(
        initialValue = 0.50f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(270), RepeatMode.Reverse), label = "b2"
    )
    val b3 by inf.animateFloat(
        initialValue = 0.20f, targetValue = 0.80f,
        animationSpec = infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "b3"
    )
    val b4 by inf.animateFloat(
        initialValue = 0.60f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(310), RepeatMode.Reverse), label = "b4"
    )

    Row(
        modifier = Modifier.size(width = 20.dp, height = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(b1, b2, b3, b4).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(topStart = 1.dp, topEnd = 1.dp))
                    .background(AccentGold)
            )
        }
    }
}
