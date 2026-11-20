package com.otium.audio.ui.components.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.otium.audio.ui.theme.LocalOtiumColors
import com.otium.audio.ui.theme.ErrorRed
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
import com.otium.audio.ui.viewmodel.LibraryTab

/**
 * Sleek circular action button for the top navigation bar.
 * Replaces loose, cluttered icon buttons with calibrated studio glass controls.
 */
@Composable
private fun TopBarActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary,
    isActive: Boolean = false,
    showIndicatorDot: Boolean = false,
    indicatorColor: Color = AccentGold
) {
    val isDark = LocalOtiumColors.current.isDark
    val bg = if (isActive) {
        if (isDark) Color(0x24FFFFFF) else Color(0xFF0F1115)
    } else {
        if (isDark) Color(0x0EFFFFFF) else Color.White
    }
    val border = if (isActive) {
        if (isDark) Color(0x40FFFFFF) else Color(0xFF0F1115)
    } else {
        DarkCardBorder
    }
    val iconTint = if (isActive) {
        if (isDark) TextPrimary else Color.White
    } else {
        if (isDark) tint else TextPrimary
    }

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                width = 0.75.dp,
                color = border,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )

        if (showIndicatorDot) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, end = 4.dp)
                    .size(6.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(indicatorColor)
            )
        }
    }
}

@Composable
fun HomeTopBar(
    songCount: Int,
    isEqualizerEnabled: Boolean,
    availableUpdate: UpdateInfo?,
    isDarkMode: Boolean = true,
    onToggleDarkMode: () -> Unit = {},
    onOpenEqualizer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSort: () -> Unit = {},
    onStartSelection: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clean single-line track count (no wrapping, clutter-free)
        Text(
            text = "$songCount مقطع صوتي",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = (-0.2).sp
            ),
            color = TextPrimary,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        // Clutter-free unified studio control cluster
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Dark / Light Mode Switcher ("ايقونة بالاعلى تحويل بين الوضع المظلم والنهاري")
            TopBarActionButton(
                icon = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = if (isDarkMode) "التبديل إلى الوضع النهاري" else "التبديل إلى الوضع المظلم",
                onClick = onToggleDarkMode
            )

            // 2. Batch selection mode
            TopBarActionButton(
                icon = Icons.Default.Check,
                contentDescription = "Selection Mode",
                onClick = onStartSelection
            )

            // 3. Sort order sheet
            TopBarActionButton(
                icon = Icons.AutoMirrored.Filled.Sort,
                contentDescription = "Sort",
                onClick = onOpenSort
            )

            // 4. Audio Equalizer (with active glow detent)
            TopBarActionButton(
                icon = Icons.Default.Equalizer,
                contentDescription = "Equalizer",
                onClick = onOpenEqualizer,
                isActive = isEqualizerEnabled,
                showIndicatorDot = isEqualizerEnabled,
                indicatorColor = AccentGold
            )

            // 5. Application settings (shows indicator dot if update is ready)
            TopBarActionButton(
                icon = Icons.Default.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                showIndicatorDot = availableUpdate != null,
                indicatorColor = AccentGold
            )
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
    onPlayNext: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close Selection",
                    tint = TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = if (selectedCount == 0) "اختر مقاطع" else "تم تحديد $selectedCount",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    maxLines = 1
                )
                if (totalCount > 0) {
                    Text(
                        text = "من إجمالي $totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            // Select All
            IconButton(onClick = onSelectAll, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Select All",
                    tint = if (selectedCount == totalCount && totalCount > 0) AccentGold else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Add to Playlist
            IconButton(
                onClick = onAddToPlaylist,
                enabled = selectedCount > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                    contentDescription = "Add to Playlist",
                    tint = if (selectedCount > 0) AccentGold else TextTertiary.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Play Next
            IconButton(
                onClick = onPlayNext,
                enabled = selectedCount > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Play Next",
                    tint = if (selectedCount > 0) AccentGold else TextTertiary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Share
            IconButton(
                onClick = onShareSelected,
                enabled = selectedCount > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = if (selectedCount > 0) AccentGold else TextTertiary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Delete
            IconButton(
                onClick = onDeleteSelected,
                enabled = selectedCount > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Selected",
                    tint = if (selectedCount > 0) ErrorRed else TextTertiary.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
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
        val isDark = LocalOtiumColors.current.isDark
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(46.dp),
            placeholder = {
                Text(
                    text = "بحث في $songCount مقطعاً، فناناً، ألبوماً...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary.copy(alpha = 0.65f),
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
            shape = RoundedCornerShape(23.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = if (isDark) Color(0xFF0C0C10) else Color(0xFFFFFFFF),
                unfocusedContainerColor = if (isDark) Color(0xFF0C0C10) else Color(0xFFFFFFFF),
                focusedBorderColor = if (isDark) Color(0x38FFFFFF) else Color(0x35000000),
                unfocusedBorderColor = DarkCardBorder,
                cursorColor = TextPrimary,
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
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filterPills = remember(counts) {
            listOf(
                Triple(LibraryTab.TRACKS, "جميع المقاطع", counts.getOrElse(0) { 0 }),
                Triple(LibraryTab.ARTISTS, "الفنانين", counts.getOrElse(1) { 0 }),
                Triple(LibraryTab.ALBUMS, "الألبومات", counts.getOrElse(2) { 0 }),
                Triple(LibraryTab.FAVORITES, "المفضلة", counts.getOrElse(3) { 0 }),
                Triple(LibraryTab.WEEKLY_ROTATION, "التدوير الأسبوعي", counts.getOrElse(4) { 0 }),
                Triple(LibraryTab.FORGOTTEN_GEMS, "جواهر منسية", counts.getOrElse(5) { 0 }),
                Triple(LibraryTab.FOLDERS, "المجلدات", counts.getOrElse(6) { 0 }),
                Triple(LibraryTab.RECENTLY_ADDED, "المضافة حديثاً", counts.getOrElse(7) { 0 }),
                Triple(LibraryTab.PLAYLISTS, "قوائم التشغيل", counts.getOrElse(8) { 0 }),
                Triple(LibraryTab.MOST_PLAYED, "الأكثر استماعاً", counts.getOrElse(9) { 0 }),
                Triple(LibraryTab.HISTORY, "سجل التشغيل", counts.getOrElse(10) { 0 })
            )
        }

        val isDark = LocalOtiumColors.current.isDark

        filterPills.forEach { (tab, label, _) ->
            val isSelected = selectedTab == tab

            val pillBg by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) Color.White else Color(0xFF0F1115)
                } else {
                    if (isDark) Color(0x0EFFFFFF) else Color(0xFFEFF2F7)
                },
                animationSpec = tween(140),
                label = "pill_bg"
            )

            val pillBorderColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) Color.White else Color(0xFF0F1115)
                } else {
                    DarkCardBorder
                },
                animationSpec = tween(140),
                label = "pill_border"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (isDark) Color.Black else Color.White
                } else TextSecondary,
                animationSpec = tween(140),
                label = "pill_text"
            )

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBg)
                    .border(0.75.dp, pillBorderColor, CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTabSelected(tab)
                    }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = textColor
                )
            }
        }
    }
}
