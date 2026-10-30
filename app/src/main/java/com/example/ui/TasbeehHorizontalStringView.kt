package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun TasbeehHorizontalStringView(
    currentCount: Int,
    currentStyle: BeadStyleObj,
    onBeadClick: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    
    val animatedOffset by animateFloatAsState(
        targetValue = currentCount.toFloat(),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "TasbeehOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { dragAccumulator = 0f }
                ) { change, dragAmount ->
                    change.consume()
                    dragAccumulator += dragAmount
                    if (dragAccumulator < -25f) { // Swipe left -> advance bead
                        onBeadClick()
                        dragAccumulator = 0f
                    } else if (dragAccumulator > 25f) { // Swipe right -> previous bead
                        onDecrement()
                        dragAccumulator = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onBeadClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            
            val beadRadius = 24.dp.toPx()
            val spacing = beadRadius * 1.65f
            val halfGap = beadRadius * 2.4f // Center gap where active bead passes
            
            // Draw continuous Thread across entire width
            drawLine(
                color = Color(0xFF424242),
                start = Offset(0f, h / 2f),
                end = Offset(w, h / 2f),
                strokeWidth = 3.dp.toPx()
            )
            drawLine(
                color = Color(0xFF757575),
                start = Offset(0f, h / 2f - 0.5.dp.toPx()),
                end = Offset(w, h / 2f - 0.5.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
            
            // Dynamic window of beads around the current animated offset.
            // This guarantees beads NEVER run out or disappear regardless of count!
            val currentBase = Math.floor(animatedOffset.toDouble()).toInt()
            val beadsToDraw = mutableListOf<Pair<Float, Float>>() // Pair(t, x)
            
            for (k in (currentBase - 12)..(currentBase + 12)) {
                val t = k - animatedOffset
                val x = when {
                    t >= 1f -> cx + halfGap + (t - 1f) * spacing
                    t <= 0f -> cx - halfGap + t * spacing
                    else -> cx - halfGap + t * (2f * halfGap)
                }
                
                // Draw all beads within and just outside view bounds for seamless scrolling
                if (x >= -beadRadius * 2f && x <= w + beadRadius * 2f) {
                    beadsToDraw.add(Pair(t, x))
                }
            }
            
            // Draw outer beads first, active moving bead last so foreground z-order is pristine
            beadsToDraw.sortByDescending { Math.abs(it.first - 0.5f) }
            
            for (bead in beadsToDraw) {
                drawRealisticBead(Offset(bead.second, h / 2f), beadRadius, currentStyle)
            }
        }
    }
}
