package com.otium.audio.ui.components.nowplaying

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.model.Song
import com.otium.audio.playback.SleepTimerState
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentGoldDim
import com.otium.audio.ui.theme.AccentGoldLight
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSheetBackground
import com.otium.audio.ui.theme.DarkSheetScrim
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingMoreSheet(
    context: Context,
    showLyrics: Boolean,
    playbackSpeed: Float,
    sleepTimerState: SleepTimerState,
    queueSize: Int,
    onToggleLyrics: () -> Unit,
    onOpenSpeedDialog: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenQueue: () -> Unit,
    onEditTagClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheetBackground,
        scrimColor = DarkSheetScrim,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                    onDismiss(); onToggleLyrics()
                },
                Triple(Icons.Default.Speed, "سرعة التشغيل (${playbackSpeed}x)") {
                    onDismiss(); onOpenSpeedDialog()
                },
                Triple(Icons.Default.Bedtime, if (sleepTimerState.isActive) "مؤقت النوم (${sleepTimerState.remainingSeconds} ثانية)" else "مؤقت النوم") {
                    onDismiss(); onOpenSleepTimer()
                },
                Triple(Icons.AutoMirrored.Filled.QueueMusic, "قائمة التشغيل الحالية ($queueSize مقطع)") {
                    onDismiss(); onOpenQueue()
                },
                Triple(Icons.Default.Headphones, "مخرج الصوت (سماعات / بلوتوث)") {
                    onDismiss()
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        try {
                            context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        } catch (_: Exception) {}
                    }
                },
                Triple(Icons.Default.EditNote, "تعديل بيانات المقطع الصوتي") {
                    onDismiss(); onEditTagClick()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSpeedDialog(
    playbackSpeed: Float,
    onSetPlaybackSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkSheetBackground,
        scrimColor = DarkSheetScrim,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                        onClick = { onSetPlaybackSpeed(speed); onDismiss() },
                        label = { Text("${speed}x", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
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

@Composable
fun AudioInspectorDialog(
    song: Song,
    audioSpecs: String?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "تفاصيل الملف الصوتي",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text("العنوان", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(song.displayTitle, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Column {
                    Text("الفنان والألبوم", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text("${song.artist}  •  ${song.album}", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                Column {
                    Text("المواصفات والترميز", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(audioSpecs ?: song.audioFormatBadge, style = MaterialTheme.typography.bodyMedium, color = AccentGold)
                }
                Column {
                    Text("المدة الزمنية", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(song.formattedDuration, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
                if (song.size > 0L) {
                    Column {
                        Text("حجم الملف", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        val sizeMb = String.format(java.util.Locale.US, "%.2f MB", song.size / (1024f * 1024f))
                        Text(sizeMb, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    }
                }
                if (song.relativePath.isNotBlank()) {
                    Column {
                        Text("المسار على الذاكرة", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(song.relativePath, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = TextTertiary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = AccentGold, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun AudiophileVinylCover() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        listOf(0.88f, 0.74f, 0.60f, 0.46f).forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxSize(fraction)
                    .clip(CircleShape)
                    .border(1.dp, DarkCardBorder.copy(alpha = 0.6f), CircleShape)
            )
        }

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
