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
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentGoldMuted
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary

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

    val thumbnailRequest = remember(song.albumArtUri) {
        ImageRequest.Builder(context)
            .data(song.albumArtUri)
            .size(128, 128)
            .crossfade(120)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    if (isCurrentSong) AccentGoldMuted else DarkSurfaceVariant
                )
                .clickable { onSongClick() }
        ) {
            // Left accent bar for currently playing track — Poweramp style
            if (isCurrentSong) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(64.dp)
                        .align(Alignment.CenterStart)
                        .background(AccentGold)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isCurrentSong) 19.dp else 16.dp,
                        end = 8.dp,
                        top = 9.dp,
                        bottom = 9.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art — 54dp, 8dp corners, no extra border (Poweramp style)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = thumbnailRequest,
                        contentDescription = song.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )

                    // Playing overlay — animated EQ bars
                    if (isCurrentSong) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(DarkSurface.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            MiniEqBars()
                        }
                    } else if (song.albumArtUri == null) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title + artist — left-weighted
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.sp,
                            textDirection = TextDirection.ContentOrLtr
                        ),
                        color = if (isCurrentSong) AccentGold else TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(song.artist)
                            append("  •  ")
                            append(song.formattedDuration)
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDirection = TextDirection.ContentOrLtr,
                            fontSize = 12.sp
                        ),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite icon only (no edit pencil cluttering the list)
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite
                                      else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (song.isFavorite) ErrorRed else DarkCardBorder,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Hairline bottom divider (Poweramp style — no card borders)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .align(Alignment.BottomCenter)
                    .background(DarkCardBorder.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
fun MiniEqBars() {
    val inf = rememberInfiniteTransition(label = "eq_bars")
    val b1 by inf.animateFloat(
        initialValue = 0.18f, targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "b1"
    )
    val b2 by inf.animateFloat(
        initialValue = 0.52f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(290), RepeatMode.Reverse), label = "b2"
    )
    val b3 by inf.animateFloat(
        initialValue = 0.25f, targetValue = 0.78f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "b3"
    )
    val b4 by inf.animateFloat(
        initialValue = 0.6f, targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(340), RepeatMode.Reverse), label = "b4"
    )

    Row(
        modifier = Modifier.size(width = 24.dp, height = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(b1, b2, b3, b4).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(AccentGold)
            )
        }
    }
}
