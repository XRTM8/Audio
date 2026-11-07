package com.otium.audio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.model.Song
import com.otium.audio.ui.theme.DarkCardBorder
import com.otium.audio.ui.theme.DarkSurface
import com.otium.audio.ui.theme.DarkSurfaceElevated
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import kotlin.math.roundToInt

private fun extractFirstIndexChar(title: String): String {
    val trimmed = title.trim()
    var idx = 0
    while (idx < trimmed.length && (trimmed[idx] == '(' || trimmed[idx] == '[' || trimmed[idx] == '{' || trimmed[idx] == '"' || trimmed[idx] == '\'' || trimmed[idx] == ' ')) {
        idx++
    }
    if (idx >= trimmed.length) return "#"
    val ch = trimmed[idx]
    return when {
        ch in 'A'..'Z' || ch in 'a'..'z' -> ch.uppercaseChar().toString()
        ch in '\u0622'..'\u0625' || ch == '\u0627' -> "ا"
        ch in '\u0600'..'\u06FF' -> ch.toString()
        else -> "#"
    }
}

@Composable
fun FastAlphabetScroller(
    songs: List<Song>,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.size < 15) return // Only for long lists

    val haptic = LocalHapticFeedback.current

    // Dynamically generate alphabet strictly matching what is in the library
    val (alphabet, letterIndexMap) = remember(songs) {
        val map = LinkedHashMap<String, Int>()
        val arabicLetters = mutableSetOf<String>()
        val latinLetters = mutableSetOf<String>()

        songs.forEachIndexed { index, song ->
            val key = extractFirstIndexChar(song.title)
            if (!map.containsKey(key)) {
                map[key] = index
            }
            if (key.isNotEmpty()) {
                val c = key[0]
                if (c in 'A'..'Z') latinLetters.add(key)
                else if (c in '\u0600'..'\u06FF') arabicLetters.add(key)
            }
        }

        // Ordered alphabet
        val alphaList = mutableListOf("#")
        if (arabicLetters.isNotEmpty()) {
            listOf("ا", "ب", "ت", "ث", "ج", "ح", "خ", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ع", "غ", "ف", "ق", "ك", "ل", "م", "ن", "هـ", "و", "ي")
                .filter { map.containsKey(it) }
                .forEach { alphaList.add(it) }
        }
        if (latinLetters.isNotEmpty()) {
            ('A'..'Z').map { it.toString() }
                .filter { map.containsKey(it) }
                .forEach { alphaList.add(it) }
        }

        Pair(alphaList, map)
    }

    if (alphabet.size < 3) return

    var isDragging by remember { mutableStateOf(false) }
    var selectedLetter by remember { mutableStateOf("") }
    var columnHeightPx by remember { mutableFloatStateOf(1f) }
    var currentTouchY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                columnHeightPx = coordinates.size.height.toFloat()
            }
            .pointerInput(alphabet, letterIndexMap) {
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
        // Floating Letter Bubble in center-start during drag
        AnimatedVisibility(
            visible = isDragging && selectedLetter.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset {
                    IntOffset(
                        x = (-44).dp.roundToPx(),
                        y = (currentTouchY - 24.dp.toPx()).coerceIn(0f, (columnHeightPx - 48.dp.toPx()).coerceAtLeast(0f)).roundToInt()
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(12.dp, CircleShape, spotColor = Color.White)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = Color.Black
                )
            }
        }

        // Discrete, ultra-slim alphabet strip
        Column(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x3314141E))
                .border(0.5.dp, DarkCardBorder, CircleShape)
                .padding(vertical = 4.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { letter ->
                val isSel = selectedLetter == letter
                Text(
                    text = letter,
                    fontSize = 9.sp,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSel) Color.White else TextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }
        }
    }
}
