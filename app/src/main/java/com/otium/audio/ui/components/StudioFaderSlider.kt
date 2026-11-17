package com.otium.audio.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.otium.audio.ui.theme.AccentTitaniumWhite
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Studio Fader Slider (شريط معادل صوتي استوديو احترافي)
 *
 * المصمم خصيصاً ليحل محل أشرطة Material 3 العريضة والممتلئة التي تتحول إلى كتل بيضاء مصمتة.
 * - مسار نحيف (3.5dp) محفور بعناية داخل لوحة التحكم.
 * - مؤشر نقطة الصفر (0.0 dB Center Detent) للمعادلات ثنائية القطبية.
 * - يمتد التوهج اللوني فقط من نقطة الصفر إلى القيمة الحالية بدقة هندسية حقيقية.
 * - مقبض عتلة دائري من التيتانيوم المصقول مع حلقة داخلية واستجابة لمسية.
 */
@Composable
fun StudioFaderSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isBipolar: Boolean = true,
    centerDetent: Float = 0f,
    activeColor: Color = AccentTitaniumWhite
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }

    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
    val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
    val fraction = ((clampedValue - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
    val centerFraction = if (isBipolar) {
        ((centerDetent - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
    } else 0f

    val thumbRadiusResting = 7.5.dp
    val thumbRadiusDragging = 9.5.dp
    val thumbRadius by animateDpAsState(
        targetValue = if (isDragging) thumbRadiusDragging else thumbRadiusResting,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "studio_fader_thumb_radius"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        val thumbPad = thumbRadiusResting.toPx()
                        val trackWidth = (size.width - thumbPad * 2).coerceAtLeast(1f)
                        val clickFraction = ((offset.x - thumbPad) / trackWidth).coerceIn(0f, 1f)
                        val newValue = valueRange.start + clickFraction * rangeSpan
                        if (isBipolar && abs(newValue - centerDetent) < (rangeSpan * 0.04f)) {
                            onValueChange(centerDetent)
                        } else {
                            onValueChange(newValue)
                        }
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                )
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, _ ->
                        change.consume()
                        val thumbPad = thumbRadiusResting.toPx()
                        val trackWidth = (size.width - thumbPad * 2).coerceAtLeast(1f)
                        val dragFraction = ((change.position.x - thumbPad) / trackWidth).coerceIn(0f, 1f)
                        val newValue = valueRange.start + dragFraction * rangeSpan
                        if (isBipolar && abs(newValue - centerDetent) < (rangeSpan * 0.035f)) {
                            onValueChange(centerDetent)
                        } else {
                            onValueChange(newValue)
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val thumbPadPx = with(density) { thumbRadiusResting.toPx() }
        val trackWidthPx = (widthPx - thumbPadPx * 2).coerceAtLeast(1f)
        val centerY = heightPx / 2f
        val currentThumbX = thumbPadPx + fraction * trackWidthPx

        Canvas(modifier = Modifier.fillMaxSize()) {
            val trackHeightPx = 3.5.dp.toPx()
            val trackCornerRadius = CornerRadius(trackHeightPx / 2f, trackHeightPx / 2f)

            // 1. Recessed groove background
            drawRoundRect(
                color = Color(0xFF242430),
                topLeft = Offset(thumbPadPx, centerY - trackHeightPx / 2f),
                size = Size(trackWidthPx, trackHeightPx),
                cornerRadius = trackCornerRadius
            )

            // 2. Active fill: from 0dB center if bipolar, or from start if unipolar
            if (enabled) {
                if (isBipolar) {
                    val centerX = thumbPadPx + centerFraction * trackWidthPx
                    val fillLeft = min(centerX, currentThumbX)
                    val fillRight = max(centerX, currentThumbX)
                    val fillWidth = (fillRight - fillLeft).coerceAtLeast(0f)
                    if (fillWidth > 1f) {
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(fillLeft, centerY - trackHeightPx / 2f),
                            size = Size(fillWidth, trackHeightPx),
                            cornerRadius = trackCornerRadius
                        )
                    }
                } else {
                    val fillWidth = (currentThumbX - thumbPadPx).coerceAtLeast(0f)
                    if (fillWidth > 1f) {
                        drawRoundRect(
                            color = activeColor,
                            topLeft = Offset(thumbPadPx, centerY - trackHeightPx / 2f),
                            size = Size(fillWidth, trackHeightPx),
                            cornerRadius = trackCornerRadius
                        )
                    }
                }
            }

            // 3. Center detent tick (for bipolar)
            if (isBipolar) {
                val tickX = thumbPadPx + centerFraction * trackWidthPx
                val tickHeightPx = 8.dp.toPx()
                drawLine(
                    color = if (enabled) Color(0x66FFFFFF) else Color(0x22FFFFFF),
                    start = Offset(tickX, centerY - tickHeightPx / 2f),
                    end = Offset(tickX, centerY + tickHeightPx / 2f),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 4. Studio Fader Thumb
            val currentRadiusPx = thumbRadius.toPx()
            val thumbColor = if (enabled) AccentTitaniumWhite else Color(0xFF555562)

            // Subtle drop shadow
            drawCircle(
                color = Color(0x55000000),
                radius = currentRadiusPx + 1.5.dp.toPx(),
                center = Offset(currentThumbX, centerY + 1.dp.toPx())
            )

            // Outer Titanium Circle
            drawCircle(
                color = thumbColor,
                radius = currentRadiusPx,
                center = Offset(currentThumbX, centerY)
            )

            // Inner dark core for tactile feel
            if (enabled) {
                drawCircle(
                    color = Color(0xFF14141A),
                    radius = currentRadiusPx * 0.42f,
                    center = Offset(currentThumbX, centerY)
                )
            }
        }
    }
}
