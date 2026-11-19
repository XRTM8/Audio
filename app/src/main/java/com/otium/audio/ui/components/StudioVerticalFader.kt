package com.otium.audio.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otium.audio.ui.theme.AccentCyan
import com.otium.audio.ui.theme.AccentTitaniumWhite
import com.otium.audio.ui.theme.DarkSheetCard
import com.otium.audio.ui.theme.DarkSheetCardBorder
import com.otium.audio.ui.theme.TextPrimary
import com.otium.audio.ui.theme.TextSecondary
import com.otium.audio.ui.theme.TextTertiary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Studio Vertical Console Fader (عتلة معادل استوديو احترافية عمودية)
 *
 * Replaces stacked horizontal rows with an authentic multi-channel studio mixer fader console:
 * - Vertical travel (top = +15dB boost, bottom = -15dB cut).
 * - Physical 0.0 dB Center Detent with tactile snap & haptic feedback.
 * - Double-tap to zero out the band instantaneously.
 * - Solid Neve/SSL-inspired knurled fader cap with horizontal center registration hairline.
 * - Crisp real-time dB readout at top and frequency badge at bottom.
 */
@Composable
fun StudioVerticalFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    freqLabel: String,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = -15f..15f,
    centerDetent: Float = 0f,
    enabled: Boolean = true,
    activeColor: Color = AccentTitaniumWhite
) {
    val haptic = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }

    val rangeSpan = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
    val clampedValue = value.coerceIn(valueRange.start, valueRange.endInclusive)

    // Fraction from bottom: 0.0 at bottom (minDb), 1.0 at top (maxDb)
    val fractionFromBottom = ((clampedValue - valueRange.start) / rangeSpan).coerceIn(0f, 1f)
    val centerFractionFromBottom = ((centerDetent - valueRange.start) / rangeSpan).coerceIn(0f, 1f)

    val faderCapWidthResting = 28.dp
    val faderCapWidthDragging = 32.dp
    val faderCapWidth by animateDpAsState(
        targetValue = if (isDragging) faderCapWidthDragging else faderCapWidthResting,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "studio_vertical_fader_cap_width"
    )

    val isNonZero = abs(clampedValue - centerDetent) > 0.1f
    val isBoost = clampedValue > centerDetent + 0.1f
    val isCut = clampedValue < centerDetent - 0.1f

    Column(
        modifier = modifier.width(42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Value dB Readout
        Text(
            text = if (isNonZero) {
                String.format(Locale.US, "%+.1f", clampedValue)
            } else {
                "0.0"
            },
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isNonZero) FontWeight.Bold else FontWeight.Medium,
                fontSize = 10.sp,
                letterSpacing = (-0.2).sp
            ),
            color = when {
                !enabled -> TextTertiary
                isBoost -> AccentTitaniumWhite
                isCut -> AccentCyan
                else -> TextSecondary.copy(alpha = 0.7f)
            },
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Vertical Fader Track & Thumb
        BoxWithConstraints(
            modifier = Modifier
                .width(40.dp)
                .height(136.dp)
                .pointerInput(enabled, valueRange) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = {
                            // Instant zero-out on double tap
                            onValueChange(centerDetent)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onTap = { offset ->
                            val thumbHeightPx = 16.dp.toPx()
                            val trackHeight = (size.height - thumbHeightPx).coerceAtLeast(1f)
                            // Invert Y: top is 1.0, bottom is 0.0
                            val clickFraction = 1f - ((offset.y - thumbHeightPx / 2f) / trackHeight).coerceIn(0f, 1f)
                            val newValue = valueRange.start + clickFraction * rangeSpan
                            if (abs(newValue - centerDetent) < (rangeSpan * 0.045f)) {
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
                    var lastWasZero = abs(value - centerDetent) < 0.2f
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, _ ->
                            change.consume()
                            val thumbHeightPx = 16.dp.toPx()
                            val trackHeight = (size.height - thumbHeightPx).coerceAtLeast(1f)
                            val dragFraction = 1f - ((change.position.y - thumbHeightPx / 2f) / trackHeight).coerceIn(0f, 1f)
                            val rawValue = valueRange.start + dragFraction * rangeSpan

                            // Center detent magnetic snap (+/- 0.6 dB window)
                            val isNearZero = abs(rawValue - centerDetent) < (rangeSpan * 0.038f)
                            val newValue = if (isNearZero) centerDetent else rawValue

                            if (isNearZero && !lastWasZero) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                lastWasZero = true
                            } else if (!isNearZero) {
                                lastWasZero = false
                            }

                            onValueChange(newValue)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val thumbHeightPx = with(density) { 16.dp.toPx() }
            val thumbWidthPx = with(density) { faderCapWidth.toPx() }
            val trackHeightPx = (heightPx - thumbHeightPx).coerceAtLeast(1f)

            // Y coordinates: Top is min Y, Bottom is max Y
            val centerY = thumbHeightPx / 2f + (1f - centerFractionFromBottom) * trackHeightPx
            val currentThumbY = thumbHeightPx / 2f + (1f - fractionFromBottom) * trackHeightPx
            val centerX = widthPx / 2f

            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackGrooveWidth = 3.5.dp.toPx()
                val trackRadius = CornerRadius(trackGrooveWidth / 2f, trackGrooveWidth / 2f)

                // 1. Recessed Channel Track Groove
                drawRoundRect(
                    color = Color(0xFF1B1B24),
                    topLeft = Offset(centerX - trackGrooveWidth / 2f, thumbHeightPx / 2f),
                    size = Size(trackGrooveWidth, trackHeightPx),
                    cornerRadius = trackRadius
                )

                // 2. Active Bipolar Fill from 0.0 dB Center Detent
                if (enabled && isNonZero) {
                    val fillTop = min(centerY, currentThumbY)
                    val fillBottom = max(centerY, currentThumbY)
                    val fillHeight = (fillBottom - fillTop).coerceAtLeast(1f)

                    val fillColor = if (isBoost) activeColor else AccentCyan

                    drawRoundRect(
                        color = fillColor,
                        topLeft = Offset(centerX - trackGrooveWidth / 2f, fillTop),
                        size = Size(trackGrooveWidth, fillHeight),
                        cornerRadius = trackRadius
                    )
                }

                // 3. Center Detent 0.0 dB Registration Notch
                val notchWidth = 10.dp.toPx()
                val notchHeight = 1.5.dp.toPx()
                drawRoundRect(
                    color = if (enabled) Color(0x66FFFFFF) else Color(0x2AFFFFFF),
                    topLeft = Offset(centerX - notchWidth / 2f, centerY - notchHeight / 2f),
                    size = Size(notchWidth, notchHeight),
                    cornerRadius = CornerRadius(notchHeight / 2f, notchHeight / 2f)
                )

                // 4. Subtle dB Calibration Marks (-10dB, 0dB, +10dB)
                val tickWidth = 6.dp.toPx()
                val tickHeight = 1.dp.toPx()
                val topTickY = thumbHeightPx / 2f + (1f - ((10f - valueRange.start) / rangeSpan)) * trackHeightPx
                val bottomTickY = thumbHeightPx / 2f + (1f - ((-10f - valueRange.start) / rangeSpan)) * trackHeightPx

                drawRect(
                    color = Color(0x22FFFFFF),
                    topLeft = Offset(centerX - tickWidth / 2f, topTickY - tickHeight / 2f),
                    size = Size(tickWidth, tickHeight)
                )
                drawRect(
                    color = Color(0x22FFFFFF),
                    topLeft = Offset(centerX - tickWidth / 2f, bottomTickY - tickHeight / 2f),
                    size = Size(tickWidth, tickHeight)
                )

                // 5. Studio Console Fader Cap (Tactile Anodized Knob)
                val capLeft = centerX - thumbWidthPx / 2f
                val capTop = currentThumbY - thumbHeightPx / 2f
                val capCorner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                // Cap Outer Shadow / Base
                drawRoundRect(
                    color = Color(0xFF09090D),
                    topLeft = Offset(capLeft, capTop + 1.dp.toPx()),
                    size = Size(thumbWidthPx, thumbHeightPx),
                    cornerRadius = capCorner
                )

                // Cap Metallic Body
                val capColor = if (enabled) {
                    if (isDragging) Color(0xFF323242) else Color(0xFF232330)
                } else {
                    Color(0xFF191922)
                }
                drawRoundRect(
                    color = capColor,
                    topLeft = Offset(capLeft, capTop),
                    size = Size(thumbWidthPx, thumbHeightPx),
                    cornerRadius = capCorner
                )

                // Cap Hairline Rim Border
                val borderColor = if (enabled) {
                    if (isDragging) AccentTitaniumWhite.copy(alpha = 0.8f) else Color(0xFF454558)
                } else {
                    Color(0xFF2C2C38)
                }
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(capLeft, capTop),
                    size = Size(thumbWidthPx, thumbHeightPx),
                    cornerRadius = capCorner,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )

                // Cap Center White Registration Hairline Indicator
                val lineIndicatorColor = when {
                    !enabled -> Color(0x44FFFFFF)
                    isBoost -> activeColor
                    isCut -> AccentCyan
                    else -> Color.White
                }
                val indicatorWidth = thumbWidthPx - 6.dp.toPx()
                val indicatorHeight = 2.dp.toPx()
                drawRoundRect(
                    color = lineIndicatorColor,
                    topLeft = Offset(
                        centerX - indicatorWidth / 2f,
                        currentThumbY - indicatorHeight / 2f
                    ),
                    size = Size(indicatorWidth, indicatorHeight),
                    cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Frequency Badge
        Box(
            modifier = Modifier
                .width(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isNonZero && enabled) Color(0x22FFFFFF) else Color(0x10FFFFFF))
                .border(
                    width = 0.5.dp,
                    color = if (isNonZero && enabled) AccentTitaniumWhite.copy(alpha = 0.35f) else Color(0x1AFFFFFF),
                    shape = RoundedCornerShape(6.dp)
                )
                .padding(vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = freqLabel,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isNonZero && enabled) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 9.5.sp,
                    letterSpacing = (-0.2).sp
                ),
                color = if (isNonZero && enabled) TextPrimary else TextSecondary,
                maxLines = 1
            )
        }
    }
}
