package com.otium.audio.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.update.UpdateInfo
import com.otium.audio.data.update.UpdateChecker
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    trackCount: Int,
    availableUpdate: UpdateInfo?,
    onOpenEqualizer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateResultText by remember { mutableStateOf<String?>(null) }
    var latestFoundUpdate by remember { mutableStateOf(availableUpdate) }

    var autoUpdateEnabled by remember { mutableStateOf(true) }
    var highResAudioEnabled by remember { mutableStateOf(true) }
    var ignoreShortAudio by remember { mutableStateOf(true) }
    var smooth120HzEnabled by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Settings & Engine",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // App Version Badge Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Otium Audio",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentGold.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "v1.5.0",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Poweramp Audiophile Engine • 32-Bit Float • v1.5.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Automatic Updates
            Text(
                text = "AUTOMATIC UPDATES (التحديث التلقائي)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Auto-check toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Check for Updates Automatically",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )
                            Text(
                                text = "التحقق من التحديثات تلقائياً في الخلفية",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = autoUpdateEnabled,
                            onCheckedChange = { autoUpdateEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentGold,
                                checkedTrackColor = DarkSurface
                            )
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = DarkCardBorder.copy(alpha = 0.6f)
                    )

                    // Manual Check Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isCheckingUpdate = true
                                    updateResultText = null
                                    val result = UpdateChecker.checkLatestRelease("1.5.0")
                                    isCheckingUpdate = false
                                    if (result != null) {
                                        latestFoundUpdate = result
                                        updateResultText = "تحديث جديد متوفر: ${result.versionName}"
                                    } else {
                                        updateResultText = "أنت تستخدم أحدث إصدار متوفر بالفعل (v1.5.0) ✓"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainer),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isCheckingUpdate
                        ) {
                            if (isCheckingUpdate) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentGold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("جاري الفحص...", color = TextPrimary)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = AccentGold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فحص التحديثات الآن", color = TextPrimary)
                            }
                        }

                        if (updateResultText != null) {
                            Text(
                                text = updateResultText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (latestFoundUpdate != null) AccentGold else AccentCyan,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    // Found Update Banner
                    AnimatedVisibility(visible = latestFoundUpdate != null) {
                        latestFoundUpdate?.let { update ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AccentGold.copy(alpha = 0.15f))
                                    .border(1.dp, AccentGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "⚡ إصدار جديد متوفر: ${update.versionName}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = AccentGold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = update.releaseNotes.take(160) + if (update.releaseNotes.length > 160) "..." else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            UpdateChecker.openDownload(context, update.downloadUrl)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = DarkSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تنزيل وتثبيت التحديث مباشرة", color = DarkSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Audio DSP & Playback
            Text(
                text = "AUDIO ENGINE & DSP (محرك الصوت)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Open Equalizer Shortcut
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onOpenEqualizer()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Equalizer, contentDescription = null, tint = AccentGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Equalizer & Presets", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("فتح الموازن الصوتي ومؤثرات 3D والـ Bass", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkCardBorder.copy(alpha = 0.6f))

                    // Hi-Res Audio 32-bit Float Processing
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("32-Bit High-Res Processing", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("معالجة الصوت بدقة فائقة لمنع التشويش والقص", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = highResAudioEnabled,
                            onCheckedChange = { highResAudioEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = DarkSurface)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = DarkCardBorder.copy(alpha = 0.6f))

                    // Filter short tracks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Ignore Short Audio (< 30s)", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text("استبعاد التسجيلات الصوتية والنغمات القصيرة", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = ignoreShortAudio,
                            onCheckedChange = { ignoreShortAudio = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentGold, checkedTrackColor = DarkSurface)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Performance & Display
            Text(
                text = "PERFORMANCE & DISPLAY (الأداء ومعدل الإطارات)",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("120Hz Fluid Physics", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AccentCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentCyan)
                                }
                            }
                            Text("حركة سلسة ومحسنة للشاشات السريعة بدون تقطيع", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                        Switch(
                            checked = smooth120HzEnabled,
                            onCheckedChange = { smooth120HzEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentCyan, checkedTrackColor = DarkSurface)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: About & Project
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/XRTM8/Otium-Music"))
                        context.startActivity(browserIntent)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Otium Audio on GitHub", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("مستودع المشروع والتحديثات الرسمية", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, tint = AccentGold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
