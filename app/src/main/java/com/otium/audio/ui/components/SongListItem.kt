package com.otium.audio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.AccentGold
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
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isCurrentSong) DarkSurfaceContainer else DarkSurfaceVariant,
        animationSpec = tween(300),
        label = "item_bg"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onSongClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceContainer),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                error = null
            )
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = if (isCurrentSong) AccentGold else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isCurrentSong) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isCurrentSong) AccentGold else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "${song.artist} • ${song.formattedDuration}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (song.isFavorite) ErrorRed else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
