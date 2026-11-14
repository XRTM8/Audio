package com.otium.audio.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.update.UpdateInfo
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
import com.otium.audio.ui.viewmodel.LibraryTab

@Composable
fun HomeTopBar(
    songCount: Int,
    isEqualizerEnabled: Boolean,
    availableUpdate: UpdateInfo?,
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSort: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 10.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "مكتبتك",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$songCount أغنية",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenSort) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (availableUpdate != null) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Update Available",
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            IconButton(onClick = onOpenEqualizer) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "Equalizer",
                    tint = if (isEqualizerEnabled) AccentGold else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun HomeSearchBar(
    query: String,
    songCount: Int,
    onQueryChanged: (String) -> Unit,
    focusManager: FocusManager,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .height(48.dp),
        placeholder = {
            Text(
                text = "بحث في $songCount مقطعاً، فناناً، ألبوماً...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontSize = 12.5.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceContainer,
            unfocusedContainerColor = DarkSurfaceContainer,
            focusedBorderColor = AccentGold.copy(alpha = 0.5f),
            unfocusedBorderColor = DarkCardBorder,
            cursorColor = AccentGold,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
        ),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
    )
}

@Composable
fun HomeFilterPills(
    selectedTab: LibraryTab,
    counts: List<Int>,
    onTabSelected: (LibraryTab) -> Unit,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filterPills = remember(counts) {
            listOf(
                Triple(LibraryTab.TRACKS, "جميع المقاطع", counts.getOrElse(0) { 0 }),
                Triple(LibraryTab.FAVORITES, "المفضلة", counts.getOrElse(1) { 0 }),
                Triple(LibraryTab.FOLDERS, "المجلدات", counts.getOrElse(2) { 0 }),
                Triple(LibraryTab.RECENTLY_ADDED, "المضافة حديثاً", counts.getOrElse(3) { 0 }),
                Triple(LibraryTab.PLAYLISTS, "قوائم التشغيل", counts.getOrElse(4) { 0 }),
                Triple(LibraryTab.MOST_PLAYED, "الأكثر استماعاً", counts.getOrElse(5) { 0 }),
                Triple(LibraryTab.HISTORY, "سجل التشغيل", counts.getOrElse(6) { 0 })
            )
        }

        filterPills.forEach { (tab, label, _) ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFF242430) else DarkSurfaceContainer)
                    .border(1.dp, if (isSelected) Color(0x33FFFFFF) else Color.Transparent, RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(tab)
                    }
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp
                    ),
                    color = if (isSelected) Color.White else TextSecondary
                )
            }
        }
    }
}
