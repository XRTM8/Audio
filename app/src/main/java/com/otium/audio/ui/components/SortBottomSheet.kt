package com.otium.audio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.Icons
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.model.SortOrder
import com.otium.audio.ui.theme.AccentTitaniumWhite
import com.otium.audio.ui.theme.DarkSheetBackground
import com.otium.audio.ui.theme.DarkSheetCard
import com.otium.audio.ui.theme.DarkSheetCardBorder
import com.otium.audio.ui.theme.DarkSheetScrim
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSortOrder: SortOrder,
    onSelectSortOrder: (SortOrder) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSheetCard)
                        .border(1.dp, DarkSheetCardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = null,
                        tint = AccentTitaniumWhite,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "ترتيب المقاطع",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "اختر المعيار المناسب لفرز مكتبتك الصوتية",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Grouped Options Container Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSheetCard)
                    .border(1.dp, DarkSheetCardBorder, RoundedCornerShape(16.dp))
            ) {
                val entries = SortOrder.entries
                entries.forEachIndexed { index, order ->
                    val isSelected = order == currentSortOrder
                    val icon = getSortIcon(order)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isSelected) AccentTitaniumWhite.copy(alpha = 0.08f) else DarkSheetCard)
                            .clickable {
                                onSelectSortOrder(order)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) AccentTitaniumWhite else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = order.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                ),
                                color = if (isSelected) AccentTitaniumWhite else TextSecondary
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(AccentTitaniumWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = DarkSheetBackground,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    if (index < entries.size - 1) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = DarkSheetCardBorder.copy(alpha = 0.35f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(bottom = 24.dp))
        }
    }
}

private fun getSortIcon(order: SortOrder): ImageVector = when (order) {
    SortOrder.DATE_ADDED_DESC, SortOrder.DATE_ADDED_ASC -> Icons.Default.Schedule
    SortOrder.TITLE_ASC, SortOrder.TITLE_DESC -> Icons.Default.TextFields
    SortOrder.ARTIST_ASC -> Icons.Default.Person
    SortOrder.DURATION_DESC, SortOrder.DURATION_ASC -> Icons.Default.HourglassBottom
}
