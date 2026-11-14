package com.otium.audio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.data.lyrics.TrackLyrics
import com.otium.audio.ui.theme.AccentGold
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.StateFlow

@Composable
fun LyricsSheet(
    lyrics: TrackLyrics,
    positionFlow: StateFlow<Long>,
    onSeekTo: (Long) -> Unit,
    activeColor: Color = AccentGold,
    modifier: Modifier = Modifier
) {
    if (lyrics.lines.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "لا توجد كلمات متزامنة لهذا المقطع",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ضع ملف .lrc في مجلد المقطع أو اتصل بالإنترنت للبحث التلقائي",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextTertiary
                )
            }
        }
        return
    }

    val activeIndex by remember(lyrics, positionFlow) {
        positionFlow.map { lyrics.findActiveIndex(it) }.distinctUntilChanged()
    }.collectAsState(initial = lyrics.findActiveIndex(positionFlow.value))
    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && !listState.isScrollInProgress) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(32.dp)) }

        itemsIndexed(
            items = lyrics.lines,
            key = { index, line -> "${index}_${line.timeMs}" }
        ) { index, line ->
            val isActive = index == activeIndex
            val textColor by animateColorAsState(
                targetValue = when {
                    isActive -> activeColor
                    index < activeIndex -> TextSecondary.copy(alpha = 0.6f)
                    else -> TextTertiary
                },
                animationSpec = tween(250),
                label = "lyric_color"
            )

            val fontSize = if (isActive) 24.sp else 18.sp
            val fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium

            Text(
                text = line.text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = textColor,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSeekTo(line.timeMs) }
                    .padding(vertical = 4.dp, horizontal = 8.dp)
            )
        }

        item { Spacer(modifier = Modifier.height(60.dp)) }
    }
}
