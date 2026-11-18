package com.otium.audio.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.Icons
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.AccentTitaniumWhite
import com.otium.audio.ui.theme.DarkSheetBackground
import com.otium.audio.ui.theme.DarkSheetCard
import com.otium.audio.ui.theme.DarkSheetCardBorder
import com.otium.audio.ui.theme.DarkSheetScrim
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onEditTagClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    onShowDetailsClick: (() -> Unit)? = null,
    onRemoveFromPlaylistClick: (() -> Unit)? = null,
    playlistName: String? = null,
    onSelectMultipleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val thumbReq = if (song.albumArtUri != android.net.Uri.EMPTY) {
        remember(song.albumArtUri) {
            ImageRequest.Builder(context)
                .data(song.albumArtUri)
                .size(128, 128)
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        scrimColor = DarkSheetScrim,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = TextTertiary.copy(alpha = 0.5f),
                width = 36.dp,
                height = 4.dp
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Track Hero Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF22222E))
                        .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(10.dp)),
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
                            contentDescription = song.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " • ${song.formattedDuration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0x22FFFFFF))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = song.audioFormatBadge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.5.sp
                                ),
                                color = AccentTitaniumWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Actions 3-Tile Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                    label = "تشغيل كالتالي",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onPlayNext()
                        onDismiss()
                    }
                )
                QuickActionButton(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    label = "إضافة للطابور",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onAddToQueue()
                        onDismiss()
                    }
                )
                QuickActionButton(
                    icon = Icons.Default.Share,
                    label = "مشاركة المقطع",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        shareAudio(context, song)
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Grouped Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
            ) {
                MenuRowItem(
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    label = "إضافة إلى قائمة تشغيل...",
                    onClick = {
                        onAddToPlaylistClick()
                    }
                )

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = DarkSheetCardBorder.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                MenuRowItem(
                    icon = Icons.Default.EditNote,
                    label = "تعديل بيانات المقطع (الوسوم)",
                    onClick = {
                        onEditTagClick()
                        onDismiss()
                    }
                )

                if (onSelectMultipleClick != null) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = DarkSheetCardBorder.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    MenuRowItem(
                        icon = Icons.Default.Check,
                        label = "تحديد متعدد للأغاني",
                        onClick = {
                            onSelectMultipleClick()
                            onDismiss()
                        }
                    )
                }

                if (onShowDetailsClick != null) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = DarkSheetCardBorder.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    MenuRowItem(
                        icon = Icons.Default.Info,
                        label = "تفاصيل المقطع التقنية",
                        onClick = {
                            onDismiss()
                            onShowDetailsClick()
                        }
                    )
                }

                if (onRemoveFromPlaylistClick != null) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = DarkSheetCardBorder.copy(alpha = 0.35f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    MenuRowItem(
                        icon = Icons.Default.Delete,
                        label = if (playlistName.isNullOrBlank()) "إزالة من قائمة التشغيل هذه" else "إزالة من $playlistName",
                        iconTint = ErrorRed,
                        textColor = ErrorRed,
                        onClick = {
                            onDismiss()
                            onRemoveFromPlaylistClick()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Isolated Danger Card: Delete from device
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ErrorRed.copy(alpha = 0.08f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                    .clickable {
                        onDeleteClick()
                        onDismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(ErrorRed.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "حذف من الجهاز",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        color = ErrorRed
                    )
                    Text(
                        text = "حذف الملف الصوتي نهائياً من وحدة التخزين",
                        style = MaterialTheme.typography.labelSmall,
                        color = ErrorRed.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSheetCard)
            .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0x14FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentTitaniumWhite,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 11.5.sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MenuRowItem(
    icon: ImageVector,
    label: String,
    iconTint: Color = AccentTitaniumWhite,
    textColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (iconTint == ErrorRed) ErrorRed.copy(alpha = 0.12f) else Color(0x10FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            ),
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun shareAudio(context: Context, song: Song) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, song.contentUri)
            putExtra(Intent.EXTRA_SUBJECT, song.displayTitle)
            putExtra(Intent.EXTRA_TEXT, "${song.displayTitle} - ${song.artist}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "مشاركة المقطع الصوتي").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    } catch (_: Exception) {}
}
