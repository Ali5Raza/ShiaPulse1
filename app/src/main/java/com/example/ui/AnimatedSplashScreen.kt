package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnimatedSplashScreen(onSplashFinish: () -> Unit) {
    val leftOffset = remember { Animatable(-200f) }
    val rightOffset = remember { Animatable(200f) }
    val textAlpha = remember { Animatable(0f) }
    val pulseScale = remember { Animatable(1f) }
    val rippleScale = remember { Animatable(0.1f) }
    val rippleAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1: Slide in words from left and right
        launch { textAlpha.animateTo(1f, tween(400)) }
        launch { leftOffset.animateTo(0f, tween(800, easing = FastOutSlowInEasing)) }
        rightOffset.animateTo(0f, tween(800, easing = FastOutSlowInEasing))

        // Give a tiny pause before collision impact (optional)
        delay(50)

        // Step 2: Collision and 'Dharkan' (Heartbeat) + Ripple
        launch {
            // Ripple burst
            rippleAlpha.snapTo(0.6f)
            launch { rippleAlpha.animateTo(0f, tween(800, easing = LinearOutSlowInEasing)) }
            rippleScale.animateTo(5f, tween(800, easing = FastOutSlowInEasing))
        }

        // Heartbeat pulse (lub-dub) on the text
        pulseScale.animateTo(1.3f, tween(150, easing = FastOutSlowInEasing)) // lub
        pulseScale.animateTo(1.0f, tween(150, easing = FastOutLinearInEasing))
        pulseScale.animateTo(1.15f, tween(150, easing = FastOutSlowInEasing)) // dub
        pulseScale.animateTo(1.0f, tween(150, easing = FastOutLinearInEasing))

        // Step 3: Wait a moment then finish
        delay(300)
        onSplashFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        // Ripple burst background effect
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(rippleScale.value)
                .alpha(rippleAlpha.value)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary, // glowing light
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Texts moving towards each other and bumping
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.scale(pulseScale.value)
            ) {
                Text(
                    text = "Shia",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .offset(x = leftOffset.value.dp)
                        .alpha(textAlpha.value)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Pulse",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .offset(x = rightOffset.value.dp)
                        .alpha(textAlpha.value)
                )
            }
        }
    }
}

