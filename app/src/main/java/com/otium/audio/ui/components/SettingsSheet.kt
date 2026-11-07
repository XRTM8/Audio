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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.BuildConfig
import com.otium.audio.data.update.UpdateChecker
import com.otium.audio.data.update.UpdateInfo
import com.otium.audio.data.update.UpdateInstaller
import com.otium.audio.playback.BatteryOptimizationHelper
import com.otium.audio.ui.theme.SuccessGreen
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.ErrorRed
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
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var updateProblem by remember { mutableStateOf<String?>(null) }
    var updateResultText by remember { mutableStateOf<String?>(null) }
    var latestFoundUpdate by remember { mutableStateOf(availableUpdate) }


    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceElevated,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
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
                        text = "الإعدادات ومحرك الصوت",
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
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "مشغّل موسيقى محلي • v${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Automatic Updates
            Text(
                text = "تحديثات التطبيق",
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
                                    val result = UpdateChecker.checkLatestRelease(BuildConfig.VERSION_NAME)
                                    isCheckingUpdate = false
                                    if (result != null) {
                                        latestFoundUpdate = result
                                        updateResultText = "تحديث جديد متوفر: ${result.versionName}"
                                    } else {
                                        updateResultText = "أنت تستخدم أحدث إصدار متوفر بالفعل (v${BuildConfig.VERSION_NAME}) ✓"
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
                                            if (!UpdateInstaller.canInstallPackages(context)) {
                                                // Android 8+ requires a per-app grant before we may install.
                                                UpdateInstaller.requestInstallPermission(context)
                                                updateProblem = "اسمح للتطبيق بتثبيت الحزم ثم أعد المحاولة"
                                            } else {
                                                scope.launch {
                                                    isDownloading = true
                                                    downloadProgress = 0
                                                    updateProblem = null
                                                    val apk = UpdateInstaller.download(context, update.downloadUrl) { percent ->
                                                        downloadProgress = percent
                                                    }
                                                    isDownloading = false
                                                    if (apk == null) {
                                                        updateProblem = "تعذّر التنزيل أو أن الملف غير موقّع بمفتاحك"
                                                    } else if (!UpdateInstaller.install(context, apk)) {
                                                        updateProblem = "تعذّر فتح المثبّت"
                                                    }
                                                }
                                            }
                                        },
                                        enabled = !isDownloading,
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
                                        Text(
                                            if (isDownloading) "جاري التنزيل… $downloadProgress٪"
                                            else "تحديث الآن",
                                            color = DarkSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    updateProblem?.let { message ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(message, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
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
                text = "معالجة الصوت (DSP)",
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
                                Text("المعادل الصوتي والمؤثرات", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("المعادل الصوتي، تضخيم البيس، الصدى، والكورال", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Battery Optimization & Uninterrupted Playback
            Text(
                text = "استقرار التشغيل في الخلفية",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val isBatteryExempt = remember { BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!isBatteryExempt) {
                                BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
                            }
                        }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBatteryExempt) "محمي من الإيقاف في الخلفية ✓" else "السماح بالتشغيل الدائم في الخلفية",
                            fontWeight = FontWeight.Bold,
                            color = if (isBatteryExempt) SuccessGreen else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isBatteryExempt) "التطبيق مستثنى من توفير الطاقة ولن يتوقف الصوت في الخلفية." else "اضغط هنا لإلغاء قيود بطارية النظام ومنع توقف الصوت عند إغلاق الشاشة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (!isBatteryExempt) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(18.dp)
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
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/XRTM8/Audio"))
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
                        Text("مشروع Otium Audio على GitHub", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("مستودع المشروع والتحديثات الرسمية", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = AccentGold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
