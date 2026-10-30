@file:Suppress("DEPRECATION")
package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ui.theme.IslamicGold

/**
 * Reusable animation modifiers for modern, dynamic Islamic visual design.
 */

/**
 * Applies a gentle pulse/breathing scale and alpha animation.
 * Ideal for highlight components, live pray states, next-prayer indicators.
 */
fun Modifier.pulseEffect(
    minScale: Float = 0.96f,
    maxScale: Float = 1.04f,
    durationMillis: Int = 1800
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val opacity by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = opacity
    }
}

/**
 * Smooth entrance animation applying screen elements with a clean fade-in and slide-up transition.
 */
fun Modifier.fadeInSlideUp(
    delayMillis: Int = 0,
    durationMillis: Int = 600
): Modifier = composed {
    val animState = remember { Animatable(0f) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        animState.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
                easing = EaseOutQuart
            )
        )
    }

    this.graphicsLayer {
        alpha = animState.value
        translationY = (50f * (1f - animState.value))
    }
}

/**
 * Reusable Gold Ripple Click Modifier.
 * Replaces standard ripples with a glowing IslamicGold feedback color on user tap.
 */
fun Modifier.rippleGold(
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this.clickable(
        interactionSource = interactionSource,
        indication = ripple(
            bounded = true,
            color = IslamicGold
        ),
        onClick = onClick
    )
}
