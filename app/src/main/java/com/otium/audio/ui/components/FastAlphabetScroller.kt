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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceContainer
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun FastAlphabetScroller(
    songs: List<Song>,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.size < 12) return // Only display for libraries with enough tracks

    val haptic = LocalHapticFeedback.current

    // Dynamically detect alphabet index needed based on song library contents
    val alphabet = remember(songs) {
        val hasArabic = songs.any { song ->
            val firstChar = song.title.firstOrNull() ?: ' '
            firstChar in '\u0600'..'\u06FF'
        }
        val base = listOf("#") + ('A'..'Z').map { it.toString() }
        if (hasArabic) {
            base + listOf("ا", "ب", "ت", "ج", "ح", "خ", "د", "ر", "س", "ش", "ص", "ع", "ف", "ق", "ك", "ل", "م", "ن", "هـ", "ي")
        } else {
            base
        }
    }

    var isDragging by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf("") }
    var columnHeightPx by remember { mutableFloatStateOf(1f) }
    var currentTouchY by remember { mutableFloatStateOf(0f) }

    // Map letter to first occurrence index
    val letterIndexMap = remember(songs, alphabet) {
        val map = mutableMapOf<String, Int>()
        songs.forEachIndexed { index, song ->
            val firstChar = song.title.firstOrNull() ?: '#'
            val key = when {
                firstChar.uppercaseChar() in 'A'..'Z' -> firstChar.uppercaseChar().toString()
                firstChar in '\u0600'..'\u06FF' -> firstChar.toString()
                else -> "#"
            }
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
                            currentTouchY = offset.y
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
                            currentTouchY = change.position.y
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
            // Floating Letter Bubble positioned next to the finger
            AnimatedVisibility(
                visible = isDragging && selectedLetter.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            x = (-48).dp.roundToPx(),
                            y = (currentTouchY - 26.dp.toPx()).coerceIn(0f, columnHeightPx - 52.dp.toPx()).roundToInt()
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .shadow(12.dp, CircleShape, spotColor = AccentGold)
                        .clip(CircleShape)
                        .background(AccentGold),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedLetter,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = DarkSurface
                    )
                }
            }

            // Alphabet Column Strip
            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated.copy(alpha = 0.80f))
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                alphabet.forEach { letter ->
                    Text(
                        text = letter,
                        fontSize = if (alphabet.size > 30) 7.5.sp else 8.5.sp,
                        fontWeight = if (selectedLetter == letter) FontWeight.Bold else FontWeight.Normal,
                        color = if (selectedLetter == letter) AccentGold else TextSecondary.copy(alpha = 0.65f),
                        modifier = Modifier.padding(vertical = 0.2.dp)
                    )
                }
            }
        }

}
