package com.otium.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.otium.audio.ui.theme.AccentTitaniumWhite
import com.otium.audio.ui.theme.DarkSheetBackground
import com.otium.audio.ui.theme.DarkSheetCard
import com.otium.audio.ui.theme.DarkSheetCardBorder
import com.otium.audio.ui.theme.DarkSheetScrim
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.SuccessGreen
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    state: EqualizerState,
    onToggleEnabled: (Boolean) -> Unit,
    onBandLevelChange: (Short, Float) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onPresetSelected: (String) -> Unit,
    onPreAmpChange: (Float) -> Unit = {},
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
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
                                fontSize = 16.sp
                            ),
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (state.isEnabled) SuccessGreen.copy(alpha = 0.2f) else TextSecondary.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (state.isEnabled) "ACTIVE" else "BYPASS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.isEnabled) SuccessGreen else TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isBound) {
                                    "معادل ${state.bands.size} نطاقات"
                                } else {
                                    "يعمل بعد تشغيل مسار صوتي"
                                },
                                fontSize = 11.sp,
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
                            checkedTrackColor = Color(0xFF2E2E3C),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = Color(0xFF1B1B24)
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Presets Horizontal Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.presets.forEach { preset ->
                    val isSelected = state.currentPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPresetSelected(preset) },
                        label = {
                            Text(
                                text = preset,
                                fontSize = 11.5.sp,
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

            Spacer(modifier = Modifier.height(14.dp))

            // Live Studio FFT Spectrum Analyzer Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "محلل الطيف الصوتي (FFT SPECTRUM)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = AccentTitaniumWhite
                    )
                    Text(
                        text = "16 نطاقاً في الوقت الفعلي",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                StudioSpectrumVisualizer(
                    isPlaying = state.isEnabled,
                    accentColor = AccentTitaniumWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Master Pre-Amp Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PRE-AMP GAIN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = AccentTitaniumWhite
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = String.format(Locale.getDefault(), "%+.1f dB", state.preAmpDb),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (state.preAmpDb != 0f) AccentTitaniumWhite else TextSecondary
                        )
                        if (state.isAutoPreAmpEnabled && state.effectivePreAmpDb != state.preAmpDb) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = String.format(Locale.getDefault(), "(فعلي: %+.1f)", state.effectivePreAmpDb),
                                fontSize = 9.sp,
                                color = AccentCyan
                            )
                        }
                    }
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
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Equalizer Frequency Bands Container Card
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
                        text = "نطاقات التردد المعادل (${state.bands.size} نطاقات)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        ),
                        color = AccentTitaniumWhite
                    )
                    Text(
                        text = "نقطة التعادل 0.0 dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                state.bands.forEachIndexed { index, band ->
                    val freqLabel = if (band.centerFreqHz >= 1000) {
                        String.format(Locale.getDefault(), "%.1fkHz", band.centerFreqHz / 1000f)
                    } else {
                        "${band.centerFreqHz}Hz"
                    }

                    val safeMin = if (band.minDb < band.maxDb) band.minDb else -15f
                    val safeMax = if (band.minDb < band.maxDb) band.maxDb else 15f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Frequency Badge
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x14FFFFFF))
                                .padding(vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = freqLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                ),
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Professional Precision Studio Fader
                        StudioFaderSlider(
                            value = band.levelDb.coerceIn(safeMin, safeMax),
                            onValueChange = { onBandLevelChange(band.bandIndex, it) },
                            valueRange = safeMin..safeMax,
                            enabled = state.isEnabled,
                            isBipolar = true,
                            centerDetent = 0f,
                            activeColor = AccentTitaniumWhite,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Gain dB readout
                        Text(
                            text = String.format(Locale.getDefault(), "%+.1fdB", band.levelDb),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (band.levelDb != 0f) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            color = if (state.isEnabled && band.levelDb != 0f) AccentTitaniumWhite else TextSecondary,
                            modifier = Modifier.width(52.dp)
                        )
                    }

                    if (index < state.bands.size - 1) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = DarkSheetCardBorder.copy(alpha = 0.25f),
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bass Boost & 3D Virtualizer Grouped Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Card
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

                // 3D Virtualizer Card
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

            Spacer(modifier = Modifier.height(14.dp))

            // DSP Studio Effects Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "المؤثرات الصوتية الاحترافية (Studio DSP)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                    color = AccentTitaniumWhite
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 1. REVERB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التردد الفراغي (REVERB)",
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
                                containerColor = Color(0x18FFFFFF),
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

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DarkSheetCardBorder.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(10.dp))

                // 2. CHORUS
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

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DarkSheetCardBorder.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(10.dp))

                // 3. CLARITY & DE-HARSH
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
                            text = "إخماد الترددات الحادة والمؤذية للسمع (6.2kHz)",
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

                Spacer(modifier = Modifier.height(12.dp))
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
                            text = "محاكاة مكبرات الصوت الطبيعية للحد من إرهاق السماعات وتوسيع المسرح الصوتي",
                            fontSize = 10.sp,
                            color = TextTertiary
                        )
                    }
                    Switch(
                        checked = state.isCrossfeedEnabled,
                        onCheckedChange = onToggleCrossfeed,
                        enabled = state.isEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentTitaniumWhite,
                            checkedTrackColor = Color(0xFF2E2E3C),
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = Color(0xFF1B1B24)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
