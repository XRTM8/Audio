package com.otium.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentTitaniumWhite
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSheetBackground
import com.otium.audio.ui.theme.DarkSheetScrim
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    queue: List<Song>,
    currentSong: Song?,
    onPlayTrackAt: (Int) -> Unit,
    onRemoveTrackAt: (Int) -> Unit,
    onMoveTrack: (Int, Int) -> Unit,
    onClearQueue: (() -> Unit)? = null,
    onSaveQueueAsPlaylist: ((String) -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var showSavePlaylistDialog by remember { mutableStateOf(false) }
    val itemHeightPx = with(density) { 64.dp.toPx() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        scrimColor = DarkSheetScrim,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                            contentDescription = null,
                            tint = AccentTitaniumWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "طابور التشغيل",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "${queue.size} مقطع في القائمة  ·  اسحب لإعادة الترتيب",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (queue.isNotEmpty() && onSaveQueueAsPlaylist != null) {
                        IconButton(onClick = { showSavePlaylistDialog = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "حفظ كقائمة تشغيل",
                                tint = AccentGold
                            )
                        }
                    }
                    if (queue.isNotEmpty() && onClearQueue != null) {
                        IconButton(onClick = onClearQueue) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "مسح الطابور",
                                tint = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showSavePlaylistDialog && onSaveQueueAsPlaylist != null) {
                com.otium.audio.ui.screens.CreatePlaylistDialog(
                    onDismiss = { showSavePlaylistDialog = false },
                    onCreate = { name ->
                        onSaveQueueAsPlaylist(name)
                        showSavePlaylistDialog = false
                    }
                )
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "طابور التشغيل فارغ",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(queue, key = { index, song -> "${song.id}_$index" }) { index, song ->
                        val isPlaying = currentSong?.id == song.id
                        val isBeingDragged = draggedIndex == index

                        val thumbnailRequest = if (song.albumArtUri != android.net.Uri.EMPTY) {
                            remember(song.albumArtUri) {
                                ImageRequest.Builder(context)
                                    .data(song.albumArtUri)
                                    .size(144, 144)
                                    .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                                    .allowHardware(true)
                                    .crossfade(false)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .build()
                            }
                        } else null

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isBeingDragged) 10f else 1f)
                                .graphicsLayer {
                                    translationY = if (isBeingDragged) dragOffsetY else 0f
                                    scaleX = if (isBeingDragged) 1.03f else 1f
                                    scaleY = if (isBeingDragged) 1.03f else 1f
                                }
                                .shadow(
                                    elevation = if (isBeingDragged) 12.dp else 0.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = Color(0x66000000)
                                )
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isPlaying) DarkSurfaceVariant else DarkSurfaceContainer)
                                .clickable { onPlayTrackAt(index) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drag Handle (المقبض التفاعلي للسحب وإعادة الترتيب)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .pointerInput(queue.size, index) {
                                        detectVerticalDragGestures(
                                            onDragStart = {
                                                draggedIndex = index
                                                dragOffsetY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            onDragEnd = {
                                                draggedIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onDragCancel = {
                                                draggedIndex = null
                                                dragOffsetY = 0f
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                dragOffsetY += dragAmount
                                                val currentIndex = draggedIndex ?: return@detectVerticalDragGestures

                                                if (dragOffsetY > itemHeightPx && currentIndex < queue.size - 1) {
                                                    onMoveTrack(currentIndex, currentIndex + 1)
                                                    draggedIndex = currentIndex + 1
                                                    dragOffsetY -= itemHeightPx
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                } else if (dragOffsetY < -itemHeightPx && currentIndex > 0) {
                                                    onMoveTrack(currentIndex, currentIndex - 1)
                                                    draggedIndex = currentIndex - 1
                                                    dragOffsetY += itemHeightPx
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = if (isBeingDragged) AccentGold else TextTertiary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Album Art
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (isPlaying) AccentTitaniumWhite else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (thumbnailRequest != null) {
                                    AsyncImage(
                                        model = thumbnailRequest,
                                        contentDescription = song.displayTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Song Title & Artist
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.displayTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = if (isPlaying) AccentTitaniumWhite else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${song.artist}  ·  ${song.formattedDuration}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Quick Delete from Queue
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onRemoveTrackAt(index)
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove from Queue",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
