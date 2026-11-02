package com.otium.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otium.audio.playback.EqualizerState
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceVariant
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurfaceContainer,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
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
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Audio Equalizer",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
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
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        label = { Text(preset) },
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

            Spacer(modifier = Modifier.height(20.dp))

            // Bands Sliders
            Text(
                text = "FREQUENCY BANDS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            state.bands.forEach { band ->
                val freqLabel = if (band.centerFreqHz >= 1000) {
                    String.format(Locale.getDefault(), "%.1fkHz", band.centerFreqHz / 1000f)
                } else {
                    "${band.centerFreqHz}Hz"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = freqLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextPrimary,
                        modifier = Modifier.width(60.dp)
                    )

                    Slider(
                        value = band.levelDb,
                        onValueChange = { onBandLevelChange(band.bandIndex, it) },
                        valueRange = band.minDb..band.maxDb,
                        enabled = state.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGold,
                            activeTrackColor = AccentGold,
                            inactiveTrackColor = DarkCardBorder,
                            disabledThumbColor = TextSecondary.copy(alpha = 0.4f),
                            disabledActiveTrackColor = TextSecondary.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = String.format(Locale.getDefault(), "%+.1fdB", band.levelDb),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.width(52.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bass Boost & Virtualizer Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Bass Boost
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "BASS BOOST (${state.bassBoostStrength / 10}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                    Slider(
                        value = state.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostChange(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = state.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGold,
                            activeTrackColor = AccentGold,
                            inactiveTrackColor = DarkCardBorder
                        )
                    )
                }

                // 3D Virtualizer
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3D VIRTUALIZER (${state.virtualizerStrength / 10}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary
                    )
                    Slider(
                        value = state.virtualizerStrength.toFloat(),
                        onValueChange = { onVirtualizerChange(it.toInt()) },
                        valueRange = 0f..1000f,
                        enabled = state.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentGold,
                            activeTrackColor = AccentGold,
                            inactiveTrackColor = DarkCardBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}
