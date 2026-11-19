package com.otium.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.Icons
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.playback.EqualizerState
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.AccentTitaniumWhite
import com.otium.audio.ui.theme.DarkSheetBackground
import com.otium.audio.ui.theme.DarkSheetCard
import com.otium.audio.ui.theme.DarkSheetCardBorder
import com.otium.audio.ui.theme.DarkSheetScrim
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.LocalOtiumColors
import com.otium.audio.ui.theme.SuccessGreen
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    state: EqualizerState,
    isPlaying: Boolean = true,
    onToggleEnabled: (Boolean) -> Unit,
    onBandLevelChange: (Short, Float) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onPresetSelected: (String) -> Unit,
    onPreAmpChange: (Float) -> Unit = {},
    onAutoPreAmpToggle: (Boolean) -> Unit = {},
    onReverbChange: (Int) -> Unit = {},
    onReverbPresetChange: (String) -> Unit = {},
    onChorusChange: (Int) -> Unit = {},
    onClarityChange: (Int) -> Unit = {},
    onToggleCrossfeed: (Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notchInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

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
        modifier = modifier.padding(top = notchInset + 6.dp)
    ) {
        val isDark = LocalOtiumColors.current.isDark
        val switchCheckedTrack = if (isDark) Color(0xFF2E2E3C) else Color(0xFFD1D5DB)
        val switchUncheckedTrack = if (isDark) Color(0xFF1B1B24) else Color(0xFFE2E8F0)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            // ── 1. Header Row: DSP Mastering Console ──────────────────────────────
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
                            .background(DarkSheetCard)
                            .border(1.dp, DarkSheetCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = AccentTitaniumWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "المعادل الصوتي ومحرك الاستوديو",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp
                            ),
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (state.isEnabled) SuccessGreen.copy(alpha = 0.18f)
                                        else TextSecondary.copy(alpha = 0.15f)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    text = if (state.isEnabled) "ACTIVE" else "BYPASS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isEnabled) SuccessGreen else TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "10 نطاقات Biquad • محرك Rust SIMD",
                                fontSize = 10.5.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = state.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentTitaniumWhite,
                            checkedTrackColor = switchCheckedTrack,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = switchUncheckedTrack
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 2. Presets & Instant Flat Reset Strip ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Instant Reset / Flat Action Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSheetCard)
                        .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(12.dp))
                        .clickable { onPresetSelected("Flat") }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Flat",
                            tint = AccentTitaniumWhite,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "تصفير (Flat)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentTitaniumWhite
                        )
                    }
                }

                // Studio Acoustic Presets
                val presetLabels = mapOf(
                    "Flat" to "مستوي (Flat)",
                    "Warm" to "دافئ (Warm)",
                    "Bass" to "جهير (Bass)",
                    "Vocal" to "صوت نقي (Vocal)",
                    "Bright" to "بلوري (Bright)",
                    "Loudness" to "إشباع (Loudness)",
                    "Custom" to "مخصص (Custom)"
                )

                state.presets.forEach { preset ->
                    val isSelected = state.currentPreset == preset
                    val label = presetLabels[preset] ?: preset

                    FilterChip(
                        selected = isSelected,
                        onClick = { onPresetSelected(preset) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentTitaniumWhite,
                            selectedLabelColor = DarkSurface,
                            containerColor = DarkSheetCard,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) AccentTitaniumWhite else DarkSheetCardBorder
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 3. Mastering Response Curve & Real-Time Spectrum Card ─────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (state.isEnabled) SuccessGreen else TextTertiary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "منحنى الاستجابة ومحلل الطيف (Curve & FFT)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = AccentTitaniumWhite
                        )
                    }
                    Text(
                        text = "+15 dB / -15 dB",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                StudioMasteringVisualizer(
                    bands = state.bands,
                    bassBoostStrength = state.bassBoostStrength,
                    isPlaying = isPlaying,
                    isEnabled = state.isEnabled,
                    accentColor = AccentTitaniumWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 4. Multi-Channel 10-Band Studio Mixing Console Card ───────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                    .padding(top = 12.dp, bottom = 10.dp, start = 8.dp, end = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "وحدة عتلات الاستوديو (10-Band Console)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = AccentTitaniumWhite
                    )
                    Text(
                        text = "اسحب عمودياً • انقر مرتين للتصفير",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 10-Channel Horizontal Scroll Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.bands.forEach { band ->
                        val freqLabel = if (band.centerFreqHz >= 1000) {
                            if (band.centerFreqHz % 1000 == 0) {
                                "${band.centerFreqHz / 1000}k"
                            } else {
                                String.format(Locale.US, "%.1fk", band.centerFreqHz / 1000f)
                            }
                        } else {
                            "${band.centerFreqHz}"
                        }

                        val safeMin = if (band.minDb < band.maxDb) band.minDb else -15f
                        val safeMax = if (band.minDb < band.maxDb) band.maxDb else 15f

                        StudioVerticalFader(
                            value = band.levelDb.coerceIn(safeMin, safeMax),
                            onValueChange = { onBandLevelChange(band.bandIndex, it) },
                            freqLabel = freqLabel,
                            valueRange = safeMin..safeMax,
                            centerDetent = 0f,
                            enabled = state.isEnabled,
                            activeColor = AccentTitaniumWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 5. Master Pre-Amp & Dynamic Headroom Card ─────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "مستوى الكسب التمهيدي (PRE-AMP GAIN)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = AccentTitaniumWhite
                    )
                    Text(
                        text = String.format(Locale.US, "%+.1f dB", state.preAmpDb),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        ),
                        color = if (state.preAmpDb != 0f) AccentTitaniumWhite else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                StudioFaderSlider(
                    value = state.preAmpDb,
                    onValueChange = onPreAmpChange,
                    valueRange = -10f..10f,
                    enabled = state.isEnabled,
                    isBipolar = true,
                    centerDetent = 0f,
                    activeColor = AccentTitaniumWhite
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DarkSheetCardBorder.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))

                // Auto-Headroom Protection Switch & Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                        Text(
                            text = "حماية تلقائية من التشويه (Auto-Headroom)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = if (state.isAutoPreAmpEnabled && state.effectivePreAmpDb != state.preAmpDb) {
                                String.format(
                                    Locale.US,
                                    "حماية نشطة: الكسب الفعلي %+.1f dB (تجنب تجاوز 0 dBFS)",
                                    state.effectivePreAmpDb
                                )
                            } else {
                                "يخفض الكسب تلقائياً لتفادي التقطيع الرقمي (Clipping) عند رفع الترددات"
                            },
                            fontSize = 10.sp,
                            color = if (state.isAutoPreAmpEnabled && state.effectivePreAmpDb != state.preAmpDb) AccentCyan else TextTertiary
                        )
                    }

                    Switch(
                        checked = state.isAutoPreAmpEnabled,
                        onCheckedChange = onAutoPreAmpToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentTitaniumWhite,
                            checkedTrackColor = switchCheckedTrack,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = switchUncheckedTrack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 6. Dynamic Acoustics & Spatial Processing Card ────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bass Boost Card (75Hz Sub-Bass Peaking Filter)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSheetCard)
                        .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BASS BOOST",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentTitaniumWhite
                        )
                        Text(
                            text = "${state.bassBoostStrength / 10}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.bassBoostStrength > 0) AccentTitaniumWhite else TextSecondary
                        )
                    }
                    Text(
                        text = "فلتر 75Hz الفرعي",
                        fontSize = 9.sp,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StudioFaderSlider(
                        value = state.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostChange(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = state.isEnabled,
                        isBipolar = false,
                        activeColor = AccentTitaniumWhite
                    )
                }

                // 3D Spatial Virtualizer Card (Mid-Side Stereo Widening)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSheetCard)
                        .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3D SPATIAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentCyan
                        )
                        Text(
                            text = "${state.virtualizerStrength / 10}%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.virtualizerStrength > 0) AccentCyan else TextSecondary
                        )
                    }
                    Text(
                        text = "توسيع المسرح الصوتي (Mid-Side)",
                        fontSize = 9.sp,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StudioFaderSlider(
                        value = state.virtualizerStrength.toFloat(),
                        onValueChange = { onVirtualizerChange(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = state.isEnabled,
                        isBipolar = false,
                        activeColor = AccentCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 7. Mastering FX & Room Acoustics Card ─────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "المؤثرات الفراغية وتقليل الإجهاد (Mastering FX)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = AccentTitaniumWhite
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 1. REVERB (التردد الفراغي والصدى)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التردد الفراغي والصدى (REVERB)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${state.reverbStrength}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.reverbStrength > 0) AccentTitaniumWhite else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Studio", "Room", "Hall", "Plate").forEach { preset ->
                        val isSelected = state.reverbPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = { onReverbPresetChange(preset) },
                            label = { Text(preset, fontSize = 10.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentTitaniumWhite,
                                selectedLabelColor = DarkSurface,
                                containerColor = if (isDark) Color(0x18FFFFFF) else Color(0xFFEBEEF2),
                                labelColor = TextSecondary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                StudioFaderSlider(
                    value = state.reverbStrength.toFloat(),
                    onValueChange = { onReverbChange(it.toInt()) },
                    valueRange = 0f..100f,
                    enabled = state.isEnabled,
                    isBipolar = false,
                    activeColor = AccentTitaniumWhite
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DarkSheetCardBorder.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))

                // 2. CHORUS (الكورال وتكثيف النغمات)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تأثير الكورال (CHORUS)",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${state.chorusStrength}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.chorusStrength > 0) AccentCyan else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                StudioFaderSlider(
                    value = state.chorusStrength.toFloat(),
                    onValueChange = { onChorusChange(it.toInt()) },
                    valueRange = 0f..100f,
                    enabled = state.isEnabled,
                    isBipolar = false,
                    activeColor = AccentCyan
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DarkSheetCardBorder.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(8.dp))

                // 3. CLARITY & DE-HARSH (إخماد الحدة المؤذية 6.2kHz)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "نقاء الصوت وتنعيم الحدة (CLARITY)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "إخماد الترددات الحادة والمؤذية للسمع (6.2kHz) لتقليل إجهاد الأذن",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                    Text(
                        text = "${state.clarityStrength}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.clarityStrength > 0) AccentTitaniumWhite else TextSecondary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                StudioFaderSlider(
                    value = state.clarityStrength.toFloat(),
                    onValueChange = { onClarityChange(it.toInt()) },
                    valueRange = 0f..100f,
                    enabled = state.isEnabled,
                    isBipolar = false,
                    activeColor = AccentTitaniumWhite
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DarkSheetCardBorder.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(10.dp))

                // 4. NATURAL ACOUSTIC CROSSFEED (Headphone Staging)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "المسرح الصوتي للسماعات (CROSSFEED)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "فلتر Chu Moy / Linkwitz لمزج القنوات طبيعياً للحد من إرهاق السماعات",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                    Switch(
                        checked = state.isCrossfeedEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked && !state.isEnabled) {
                                onToggleEnabled(true)
                            }
                            onToggleCrossfeed(isChecked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentTitaniumWhite,
                            checkedTrackColor = switchCheckedTrack,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = switchUncheckedTrack
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
