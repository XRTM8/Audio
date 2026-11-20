package com.otium.audio.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Universal tactile spring motion specifications.
 * Calibrated for physical feel with zero artificial lag or frame drops.
 */
object OtiumMotion {
    /**
     * Iconic fluid spring for artwork expansion and relaxation (Apple Music style).
     */
    val ArtworkSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    /**
     * Tactile hardware button compression and rebound spring (Nothing OS / Braun style).
     */
    val ButtonSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val ButtonSpringDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Snappy micro-interaction spring for pills and chips.
     */
    val SnappySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
}

/**
 * High-performance tactile press modifier.
 * Gently cushions the element under the user's touch and rebounds with a subtle spring bounce.
 */
@Composable
fun Modifier.tactilePress(
    pressedScale: Float = 0.92f,
    hapticType: HapticFeedbackType? = HapticFeedbackType.TextHandleMove,
    onClick: (() -> Unit)? = null
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = OtiumMotion.ButtonSpring,
        label = "tactile_scale"
    )
    val haptic = LocalHapticFeedback.current

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (hapticType != null) {
                            haptic.performHapticFeedback(hapticType)
                        }
                        onClick()
                    }
                )
            } else {
                Modifier
            }
        )
}
