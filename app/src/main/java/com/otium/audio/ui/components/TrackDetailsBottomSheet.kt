package com.otium.audio.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaMetadataRetriever
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.otium.audio.audio.dsp.NativeAudioDsp
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentTitaniumWhite
import com.otium.audio.ui.theme.DarkSheetBackground
import com.otium.audio.ui.theme.DarkSheetCard
import com.otium.audio.ui.theme.DarkSheetCardBorder
import com.otium.audio.ui.theme.DarkSheetScrim
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsBottomSheet(
    song: Song,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var details by remember { mutableStateOf<TrackTechnicalSpecs?>(null) }

    LaunchedEffect(song) {
        withContext(Dispatchers.IO) {
            val native = NativeAudioDsp.readAudioMetadata(song.relativePath)
            val fallbackBitrate: Int
            val fallbackSampleRate: Int
            val fallbackFormat: String

            if (native != null && native.bitrate > 0) {
                fallbackBitrate = native.bitrate
                fallbackSampleRate = native.sampleRate
                fallbackFormat = native.format
            } else {
                var br = 0
                var sr = 44100
                var fmt = song.audioFormatBadge
                try {
                    val mmr = MediaMetadataRetriever()
                    mmr.setDataSource(context, song.contentUri)
                    val brStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    val srStr = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                    } else null
                    val mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    if (brStr != null) br = brStr.toIntOrNull()?.div(1000) ?: 0
                    if (srStr != null) sr = srStr.toIntOrNull() ?: 44100
                    if (mime != null) {
                        fmt = when {
                            mime.contains("flac") -> "FLAC"
                            mime.contains("alac") -> "ALAC"
                            mime.contains("mpeg") || mime.contains("mp3") -> "MP3"
                            mime.contains("aac") -> "AAC"
                            mime.contains("ogg") || mime.contains("vorbis") -> "OGG"
                            mime.contains("opus") -> "OPUS"
                            mime.contains("wav") -> "WAV"
                            else -> song.audioFormatBadge
                        }
                    }
                    mmr.release()
                } catch (_: Throwable) {}
                fallbackBitrate = br
                fallbackSampleRate = sr
                fallbackFormat = fmt
            }

            val file = if (song.relativePath.isNotBlank()) File(song.relativePath) else null
            val fileSizeBytes = if (file?.exists() == true) file.length() else song.size
            val sizeMb = fileSizeBytes / (1024f * 1024f)

            val dateStr = if (song.dateAdded > 0) {
                val epochMs = if (song.dateAdded < 100_000_000_000L) song.dateAdded * 1000L else song.dateAdded
                SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(epochMs))
            } else {
                "غير معروف"
            }

            details = TrackTechnicalSpecs(
                format = fallbackFormat.uppercase(),
                bitrateKbps = fallbackBitrate,
                sampleRateHz = fallbackSampleRate,
                channels = "Stereo (2.0)",
                fileSizeMb = sizeMb,
                fullPath = song.relativePath.ifBlank { song.contentUri.toString() },
                dateAddedFormatted = dateStr
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSheetBackground,
        scrimColor = DarkSheetScrim,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSheetCard)
                            .border(1.dp, DarkSheetCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "تفاصيل المقطع التقنية",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = AccentTitaniumWhite
                        )
                        Text(
                            text = "Audiophile Technical Specs",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Track Hero Tile
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp),
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
                    if (song.albumArtUri != android.net.Uri.EMPTY) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(song.albumArtUri)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .crossfade(false)
                                .build(),
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
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${song.artist} • ${song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Specs Grid
            Text(
                text = "خصائص الصوت الرقمي",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSheetCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSheetCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val d = details
                    SpecRow(
                        label = "ترميز الصوت (Codec / Format)",
                        value = d?.format ?: song.audioFormatBadge,
                        valueColor = AccentTitaniumWhite,
                        isHighlight = true
                    )
                    HorizontalDivider(color = DarkSheetCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                    val bitrateText = if (d != null && d.bitrateKbps > 0) "${d.bitrateKbps} kbps" else "معالجة متغيرة (VBR)"
                    SpecRow(
                        label = "معدل البت (Bitrate)",
                        value = bitrateText,
                        valueColor = AccentGold
                    )
                    HorizontalDivider(color = DarkSheetCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                    val sampleRateText = if (d != null && d.sampleRateHz > 0) "${d.sampleRateHz / 1000f} kHz" else "44.1 kHz"
                    SpecRow(
                        label = "تردد العينة (Sample Rate)",
                        value = sampleRateText,
                        valueColor = AccentCyan
                    )
                    HorizontalDivider(color = DarkSheetCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                    SpecRow(
                        label = "مدة المسار (Duration)",
                        value = song.formattedDuration,
                        valueColor = TextPrimary
                    )
                    HorizontalDivider(color = DarkSheetCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                    val sizeText = if (d != null) String.format(Locale.getDefault(), "%.2f MB", d.fileSizeMb) else "..."
                    SpecRow(
                        label = "حجم الملف (File Size)",
                        value = sizeText,
                        valueColor = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage & File Location
            Text(
                text = "موقع وتاريخ التخزين",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSheetCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSheetCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val fullPath = details?.fullPath ?: song.relativePath
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مسار الملف في الجهاز:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("File Path", fullPath))
                                    Toast.makeText(context, "تم نسخ المسار إلى الحافظة ✓", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = AccentGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "نسخ",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = AccentGold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = fullPath,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = AccentTitaniumWhite.copy(alpha = 0.9f),
                        lineHeight = 16.sp
                    )

                    HorizontalDivider(color = DarkSheetCardBorder.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 10.dp))

                    SpecRow(
                        label = "تاريخ الإضافة إلى المكتبة",
                        value = details?.dateAddedFormatted ?: "...",
                        valueColor = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SpecRow(
    label: String,
    value: String,
    valueColor: Color,
    isHighlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.SemiBold
            ),
            color = valueColor
        )
    }
}

private data class TrackTechnicalSpecs(
    val format: String,
    val bitrateKbps: Int,
    val sampleRateHz: Int,
    val channels: String,
    val fileSizeMb: Float,
    val fullPath: String,
    val dateAddedFormatted: String
)
