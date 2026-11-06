package com.otium.audio.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.playback.EqualizerState
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.SuccessGreen
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // The sheet is tall enough to reach the status bar, so the grab handle ended up behind the
    // notch. One inset of padding puts it back under the cutout and below the clock.
    val notchInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier.padding(top = notchInset + 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
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
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "المعادل الصوتي ومحرك الاستوديو",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (state.isEnabled) SuccessGreen.copy(alpha = 0.2f) else TextSecondary.copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 0.5.dp)
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
                                    // There is no audio session to attach to until something plays.
                                    "يعمل بعد تشغيل أغنية"
                                },
                                fontSize = 10.sp,
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
                            checkedThumbColor = AccentGold,
                            checkedTrackColor = DarkSurface,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceVariant
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.presets.forEach { preset ->
                    val isSelected = state.currentPreset == preset
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPresetSelected(preset) },
                        label = { Text(preset, fontSize = 11.5.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGold,
                            selectedLabelColor = DarkSurface,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) AccentGold else DarkCardBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pre-Amp Gain Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRE-AMP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = AccentGold,
                    modifier = Modifier.width(64.dp)
                )

                Slider(
                    value = state.preAmpDb,
                    onValueChange = onPreAmpChange,
                    valueRange = -10f..10f,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentGold,
                        activeTrackColor = AccentGold,
                        inactiveTrackColor = DarkCardBorder
                    ),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = String.format(Locale.getDefault(), "%+.1fdB", state.preAmpDb),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.width(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Equalizer Frequency Bands
            Text(
                text = "نطاقات التردد المعادل (${state.bands.size} نطاقات)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = AccentGold
            )

            Spacer(modifier = Modifier.height(6.dp))

            state.bands.forEach { band ->
                val freqLabel = if (band.centerFreqHz >= 1000) {
                    String.format(Locale.getDefault(), "%.1fkHz", band.centerFreqHz / 1000f)
                } else {
                    "${band.centerFreqHz}Hz"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = freqLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 12.5.sp),
                        color = TextPrimary,
                        modifier = Modifier.width(60.dp)
                    )

                    val safeMin = if (band.minDb < band.maxDb) band.minDb else -15f
                    val safeMax = if (band.minDb < band.maxDb) band.maxDb else 15f

                    Slider(
                        value = band.levelDb.coerceIn(safeMin, safeMax),
                        onValueChange = { onBandLevelChange(band.bandIndex, it) },
                        valueRange = safeMin..safeMax,
                        enabled = true,
                        colors = SliderDefaults.colors(
                            thumbColor = if (state.isEnabled) AccentGold else TextSecondary,
                            activeTrackColor = if (state.isEnabled) AccentGold else TextSecondary.copy(alpha = 0.35f),
                            inactiveTrackColor = DarkCardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = String.format(Locale.getDefault(), "%+.1fdB", band.levelDb),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = if (state.isEnabled && band.levelDb != 0f) AccentGold else TextSecondary,
                        modifier = Modifier.width(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Bass Boost & 3D Virtualizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bass Boost
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "BASS BOOST",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentGold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.bassBoostStrength / 10}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Slider(
                        value = state.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostChange(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = true,
                        colors = SliderDefaults.colors(
                            thumbColor = if (state.isEnabled) AccentGold else TextSecondary,
                            activeTrackColor = if (state.isEnabled) AccentGold else TextSecondary.copy(alpha = 0.35f),
                            inactiveTrackColor = DarkCardBorder
                        )
                    )
                }

                // 3D Virtualizer
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "3D VIRTUALIZER",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.virtualizerStrength / 10}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Slider(
                        value = state.virtualizerStrength.toFloat(),
                        onValueChange = { onVirtualizerChange(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = true,
                        colors = SliderDefaults.colors(
                            thumbColor = if (state.isEnabled) AccentCyan else TextSecondary,
                            activeTrackColor = if (state.isEnabled) AccentCyan else TextSecondary.copy(alpha = 0.35f),
                            inactiveTrackColor = DarkCardBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(thickness = 0.5.dp, color = DarkCardBorder.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // DSP Studio Effects & Spatial Acoustics Section
            Text(
                text = "المؤثرات الصوتية (DSP)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                color = AccentGold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. REVERB (تردد فراغي / صدى)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التردد الفراغي (REVERB)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentGold
                    )
                    Text(
                        text = "${state.reverbStrength}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.reverbStrength > 0) AccentGold else TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Reverb Presets
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
                            label = { Text(preset, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentGold,
                                selectedLabelColor = DarkSurface,
                                containerColor = DarkSurfaceContainer,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                Slider(
                    value = state.reverbStrength.toFloat(),
                    onValueChange = { onReverbChange(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (state.isEnabled) AccentGold else TextSecondary,
                        activeTrackColor = if (state.isEnabled) AccentGold else TextSecondary.copy(alpha = 0.35f),
                        inactiveTrackColor = DarkCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. CHORUS (تأثير كورال احترافي متعدد الأصوات)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تأثير الكورال (CHORUS)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = AccentCyan
                    )
                    Text(
                        text = "${state.chorusStrength}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.chorusStrength > 0) AccentCyan else TextSecondary
                    )
                }

                Slider(
                    value = state.chorusStrength.toFloat(),
                    onValueChange = { onChorusChange(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (state.isEnabled) AccentCyan else TextSecondary,
                        activeTrackColor = if (state.isEnabled) AccentCyan else TextSecondary.copy(alpha = 0.35f),
                        inactiveTrackColor = DarkCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. CLARITY & DE-HARSH (دقة الصوت وتنعيم الحدة)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "نقاء الصوت وتنعيم الحدة (CLARITY)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AccentGold
                        )
                        Text(
                            text = "إخماد الترددات الحادة والمؤذية للسمع (6.2kHz) مع إبراز التفاصيل الدقيقة",
                            fontSize = 9.5.sp,
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = "${state.clarityStrength}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (state.clarityStrength > 0) AccentGold else TextSecondary
                    )
                }

                Slider(
                    value = state.clarityStrength.toFloat(),
                    onValueChange = { onClarityChange(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (state.isEnabled) AccentGold else TextSecondary,
                        activeTrackColor = if (state.isEnabled) AccentGold else TextSecondary.copy(alpha = 0.35f),
                        inactiveTrackColor = DarkCardBorder
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
