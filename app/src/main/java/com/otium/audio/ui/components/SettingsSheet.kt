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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Check
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.Icons
import androidx.compose.material3.BottomSheetDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.otium.audio.ui.theme.DarkSheetBackground
import com.otium.audio.ui.theme.DarkSheetCard
import com.otium.audio.ui.theme.DarkSheetCardBorder
import com.otium.audio.ui.theme.DarkSheetScrim
import com.otium.audio.ui.theme.AccentTitaniumWhite
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
    isSkipSilenceEnabled: Boolean = false,
    onToggleSkipSilence: (Boolean) -> Unit = {},
    isHiResFloatEnabled: Boolean = false,
    onToggleHiResFloat: (Boolean) -> Unit = {},
    isAutoPreAmpEnabled: Boolean = true,
    onToggleAutoPreAmp: (Boolean) -> Unit = {},
    isLoudnessNormalizationEnabled: Boolean = false,
    onToggleLoudnessNormalization: (Boolean) -> Unit = {},
    crossfadeSeconds: Int = 0,
    onSetCrossfadeSeconds: (Int) -> Unit = {},
    isResumeOnHeadsetConnect: Boolean = false,
    onToggleResumeOnHeadsetConnect: (Boolean) -> Unit = {},
    minDurationSeconds: Int = 0,
    onSetMinDurationSeconds: (Int) -> Unit = {},
    onExportBackup: ((String?) -> Unit) -> Unit = {},
    onImportBackup: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    isScanning: Boolean = false,
    onRescanLibrary: () -> Unit = {},
    onOpenEqualizer: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var updateProblem by remember { mutableStateOf<String?>(null) }
    var updateResultText by remember { mutableStateOf<String?>(null) }
    var latestFoundUpdate by remember { mutableStateOf(availableUpdate) }
    var backupStatusText by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val jsonString = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader().use { it.readText() }
                    }
                }.getOrNull()

                if (jsonString.isNullOrBlank()) {
                    backupStatusText = "تعذّر قراءة ملف النسخة الاحتياطية"
                } else {
                    onImportBackup(jsonString) { success ->
                        backupStatusText = if (success) "تمت استعادة القوائم والمفضلة بنجاح ✓" else "ملف النسخة الاحتياطية غير صالح"
                    }
                }
            }
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
                color = TextSecondary.copy(alpha = 0.4f),
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
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = AccentTitaniumWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "الإعدادات ومحرك الصوت",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // App Version Badge Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSheetCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSheetCardBorder)
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
                colors = CardDefaults.cardColors(containerColor = DarkSheetCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSheetCardBorder)
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
                                        text = "إصدار جديد متوفر: ${update.versionName}",
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

            // Section: Library Management
            Text(
                text = "إدارة وسائط المكتبة",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AccentGold
            )
            Spacer(modifier = Modifier.height(8.dp))

            val infiniteTransition = rememberInfiniteTransition(label = "rescan_rotation")
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing)
                ),
                label = "rescan_angle"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSheetCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSheetCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "فهرسة الملفات الصوتية",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$trackCount مسار صوتي مفهرس في المكتبة",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }

                        Button(
                            onClick = onRescanLibrary,
                            enabled = !isScanning,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainer),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .then(if (isScanning) Modifier.rotate(rotationAngle) else Modifier),
                                tint = AccentGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isScanning) "جاري الفحص..." else "فحص الآن",
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Minimum Duration Filter Chips
                    Text(
                        text = "تصفية المقاطع الصوتية القصيرة",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "إخفاء الملاحظات الصوتية والمؤثرات الأقصر من هذه المدة تلقائياً",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val durationOptions = listOf(
                        0 to "الكل (0ث)",
                        10 to "10 ثوانٍ",
                        30 to "30 ثانية",
                        60 to "60 ثانية"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        durationOptions.forEach { (sec, label) ->
                            val isSelected = minDurationSeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else DarkSurfaceContainer)
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentGold else DarkSheetCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onSetMinDurationSeconds(sec)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) AccentGold else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Backup & Restore Row
                    Text(
                        text = "النسخ الاحتياطي للقوائم والمفضلة",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "تصدير أو استعادة جميع قوائم التشغيل والأغاني المفضلة بصيغة ملف JSON",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onExportBackup { json ->
                                    if (!json.isNullOrBlank()) {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "otium_backup.json")
                                            putExtra(Intent.EXTRA_TEXT, json)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة أو حفظ النسخة الاحتياطية"))
                                        backupStatusText = "تم تصدير النسخة الاحتياطية بنجاح ✓"
                                    } else {
                                        backupStatusText = "تعذّر تصدير النسخة الاحتياطية"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AccentGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير النسخة", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }

                        Button(
                            onClick = {
                                filePickerLauncher.launch("application/json")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceContainer),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة النسخة", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                        }
                    }

                    if (backupStatusText != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = backupStatusText ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (backupStatusText!!.contains("✓")) SuccessGreen else ErrorRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Audio DSP & Playback
            Text(
                text = "معالجة الصوت الاحترافية (Audiophile DSP)",
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(imageVector = Icons.Default.Equalizer, contentDescription = null, tint = AccentGold)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("المعادل الصوتي والمؤثرات", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("المعادل الصوتي، تضخيم البيس، الصدى، والكورال", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Auto-Preamp Headroom Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "الحماية التلقائية من التشويه (Auto-Preamp)",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "خفض الـ Pre-Amp تلقائياً عند رفع المعادل لحماية سقف 0 dBFS ومنع الكبت الديناميكي",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isAutoPreAmpEnabled,
                            onCheckedChange = onToggleAutoPreAmp,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentTitaniumWhite,
                                checkedTrackColor = Color(0xFF2E2E3C),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0xFF1B1B24)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Silence Skipping Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "تخطي الصمت التلقائي (Skip Silence)",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "تجاوز فترات الصمت الرقمي في بدايات ونهايات المسارات دون تقطيع",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isSkipSilenceEnabled,
                            onCheckedChange = onToggleSkipSilence,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentTitaniumWhite,
                                checkedTrackColor = Color(0xFF2E2E3C),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0xFF1B1B24)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Hi-Res 32-Bit Float Audio Output Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "إخراج صوت عالي الدقة (32-Bit Float)",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "إرسال تيار الصوت بصيغة Float نقية لمنع تشويه التكميم وتفادي تقليص عينات 24-bit إلى 16-bit",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isHiResFloatEnabled,
                            onCheckedChange = onToggleHiResFloat,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentTitaniumWhite,
                                checkedTrackColor = Color(0xFF2E2E3C),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0xFF1B1B24)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Loudness Normalization Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "تسوية علو الصوت (Loudness Normalization)",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "موازنة ديناميكية لحجم الصوت لتجنب الارتفاع أو الانخفاض المفاجئ بين المسارات المختلفة",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isLoudnessNormalizationEnabled,
                            onCheckedChange = onToggleLoudnessNormalization,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentTitaniumWhite,
                                checkedTrackColor = Color(0xFF2E2E3C),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0xFF1B1B24)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "التلاشي المتقاطع بين المقاطع (Crossfade)",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "انتقال متداخل ناعم ومتلاشٍ بين المسارات لتفادي الانقطاع المفاجئ للصوت",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val crossfadeOptions = listOf(
                        0 to "معطل (0ث)",
                        2 to "2 ثوانٍ",
                        4 to "4 ثوانٍ",
                        6 to "6 ثوانٍ"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        crossfadeOptions.forEach { (sec, label) ->
                            val isSelected = crossfadeSeconds == sec
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else DarkSurfaceContainer)
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentGold else DarkSheetCardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onSetCrossfadeSeconds(sec)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) AccentGold else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DarkCardBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Resume on Headset Connect Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "استئناف التشغيل عند توصيل السماعات",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "استئناف الصوت تلقائياً عند اقتران سماعات البلوتوث أو إدخال كابل السماعة السلكية",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = isResumeOnHeadsetConnect,
                            onCheckedChange = onToggleResumeOnHeadsetConnect,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AccentTitaniumWhite,
                                checkedTrackColor = Color(0xFF2E2E3C),
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0xFF1B1B24)
                            )
                        )
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
                colors = CardDefaults.cardColors(containerColor = DarkSheetCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSheetCardBorder)
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
                colors = CardDefaults.cardColors(containerColor = DarkSheetCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkSheetCardBorder)
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
