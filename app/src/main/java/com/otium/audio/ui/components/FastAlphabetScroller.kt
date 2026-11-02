package com.otium.audio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceVariant
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary

@Composable
fun FastAlphabetScroller(
    songs: List<Song>,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.size < 15) return // Only display for sizable libraries

    val haptic = LocalHapticFeedback.current
    val alphabet = remember { listOf("#") + ('A'..'Z').map { it.toString() } }
    var isDragging by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf("") }
    var columnHeightPx by remember { mutableStateOf(1f) }

    // Map letter to first occurrence index
    val letterIndexMap = remember(songs) {
        val map = mutableMapOf<String, Int>()
        songs.forEachIndexed { index, song ->
            val firstChar = song.title.firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        map
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                columnHeightPx = coordinates.size.height.toFloat()
            }
            .pointerInput(songs) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        val fraction = (offset.y / columnHeightPx).coerceIn(0f, 0.999f)
                        val letterIndex = (fraction * alphabet.size).toInt()
                        val letter = alphabet.getOrElse(letterIndex) { "#" }
                        if (letter != selectedLetter) {
                            selectedLetter = letter
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            letterIndexMap[letter]?.let { onIndexSelected(it) }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        selectedLetter = ""
                    },
                    onDragCancel = {
                        isDragging = false
                        selectedLetter = ""
                    },
                    onVerticalDrag = { change, _ ->
                        val fraction = (change.position.y / columnHeightPx).coerceIn(0f, 0.999f)
                        val letterIndex = (fraction * alphabet.size).toInt()
                        val letter = alphabet.getOrElse(letterIndex) { "#" }
                        if (letter != selectedLetter) {
                            selectedLetter = letter
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            letterIndexMap[letter]?.let { onIndexSelected(it) }
                        }
                    }
                )
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Floating Letter Bubble
            AnimatedVisibility(
                visible = isDragging && selectedLetter.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = (-16).dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedLetter,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DarkSurface
                    )
                }
            }

            // Alphabet Column
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                alphabet.forEach { letter ->
                    Text(
                        text = letter,
                        fontSize = 9.sp,
                        fontWeight = if (selectedLetter == letter) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedLetter == letter) AccentGold else TextSecondary,
                        modifier = Modifier.padding(vertical = 0.5.dp)
                    )
                }
            }
        }
    }
}
