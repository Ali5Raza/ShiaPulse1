package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** A responsive, real-time bead ring. The center content remains native Compose text. */
@Composable
fun TasbeehCircleView(
    progress: Float,
    beadPosition: Int,
    currentStyle: BeadStyleObj,
    onBeadClick: () -> Unit,
    onDecrement: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    counterDescription: String = "Count dhikr",
    content: @Composable BoxScope.() -> Unit = {}
) {
    var pressed by remember { mutableStateOf(false) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tasbeeh_press_scale"
    )
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "tasbeeh_progress"
    )
    val animatedBeadPosition by animateFloatAsState(
        targetValue = beadPosition.toFloat(),
        animationSpec = tween(150, easing = FastOutSlowInEasing),
        label = "tasbeeh_bead_position"
    )

    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val innerSurface = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(pressScale)
            .semantics {
                this.contentDescription = counterDescription
                onClick(label = counterDescription) {
                    if (enabled) onBeadClick()
                    true
                }
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectHorizontalDragGestures(
                        onDragEnd = { dragAccumulator = 0f }
                    ) { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                        when {
                            dragAccumulator < -30f -> {
                                onBeadClick()
                                dragAccumulator = 0f
                            }
                            dragAccumulator > 30f -> {
                                onDecrement()
                                dragAccumulator = 0f
                            }
                        }
                    }
                }
            }
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                pressed = false
                            }
                        },
                        onTap = { onBeadClick() }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringRadius = side * 0.34f
            val beadOrbit = side * 0.43f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.10f), Color.Transparent),
                    center = center,
                    radius = side * 0.49f
                ),
                radius = side * 0.49f,
                center = center
            )
            drawCircle(
                color = track,
                radius = ringRadius,
                center = center,
                style = Stroke(width = 5.dp.toPx())
            )
            if (animatedProgress > 0.001f) {
                drawArc(
                    color = primary,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            val beadCount = 24
            val degreesPerBead = 360f / beadCount
            for (index in 0 until beadCount) {
                val angleDegrees = index * degreesPerBead - 90f - animatedBeadPosition * degreesPerBead
                val angle = Math.toRadians(angleDegrees.toDouble())
                val beadCenter = Offset(
                    x = center.x + beadOrbit * cos(angle).toFloat(),
                    y = center.y + beadOrbit * sin(angle).toFloat()
                )
                val isLeader = index == 0
                if (isLeader) {
                    drawCircle(
                        color = primary.copy(alpha = 0.24f),
                        radius = 12.dp.toPx(),
                        center = beadCenter
                    )
                }
                drawRealisticBead(
                    center = beadCenter,
                    radius = if (isLeader) 8.5.dp.toPx() else 5.8.dp.toPx(),
                    style = currentStyle
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize(0.64f)
                .clip(CircleShape)
                .background(innerSurface)
                .border(1.dp, primary.copy(alpha = 0.10f), CircleShape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}
