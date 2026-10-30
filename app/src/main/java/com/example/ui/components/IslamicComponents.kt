package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassmorphicSurface
import com.example.ui.theme.IslamicGold
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.SpiritualGreen

/**
 * Modern High-Aesthetic Reusable Core Components for Shia Islamic Application context.
 * Complies with Material Design 3 spacing, interactive feedback rules, and rich color schema.
 */

/**
 * 1. IslamicCard
 * A premium Glassmorphic surface container with optional golden/green radial/linear gradient borders.
 */
@Composable
fun IslamicCard(
    modifier: Modifier = Modifier,
    hasGradientBorder: Boolean = true,
    shape: CornerBasedShape = RoundedCornerShape(20.dp),
    borderGradient: Brush = Brush.linearGradient(
        colors = listOf(
            IslamicGold.copy(alpha = 0.5f),
            Color.White.copy(alpha = 0.08f),
            SpiritualGreen.copy(alpha = 0.3f)
        )
    ),
    borderWidth: Dp = 1.2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = GlassmorphicSurface,
        shadowElevation = 0.dp
    ) {
        val borderModifier = if (hasGradientBorder) {
            Modifier.border(borderWidth, borderGradient, shape)
        } else {
            Modifier
        }
        
        Column(
            modifier = Modifier
                .then(borderModifier)
                .padding(20.dp),
            content = content
        )
    }
}

/**
 * 2. PrayerTimeItem
 * Displays individual prayer highlight schedules. Animates with pulse effects if currently running.
 */
@Composable
fun PrayerTimeItem(
    name: String,
    time: String,
    isNextPrayer: Boolean,
    modifier: Modifier = Modifier,
    highlightColor: Color = IslamicGold
) {
    val containerBg = if (isNextPrayer) {
        highlightColor.copy(alpha = 0.15f)
    } else {
        Color.White.copy(alpha = 0.04f)
    }
    
    val borderBrush = if (isNextPrayer) {
        Brush.linearGradient(listOf(highlightColor, highlightColor.copy(alpha = 0.3f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.05f), Color.White.copy(alpha = 0.05f)))
    }

    val itemModifier = if (isNextPrayer) {
        modifier.pulseEffect(minScale = 0.98f, maxScale = 1.01f)
    } else {
        modifier
    }

    Surface(
        modifier = itemModifier,
        shape = RoundedCornerShape(16.dp),
        color = containerBg
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, borderBrush, RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Star/point status emblem
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color = if (isNextPrayer) highlightColor else SoftGrey.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isNextPrayer) FontWeight.Bold else FontWeight.Medium,
                    color = if (isNextPrayer) highlightColor else Color.White
                )
            }
            Text(
                text = time,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isNextPrayer) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (isNextPrayer) highlightColor else SoftGrey
            )
        }
    }
}

/**
 * 3. GridMenuItem
 * Adaptive interactive grid launcher buttons supporting custom vector icons and gold touch ink.
 */
@Composable
fun GridMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = IslamicGold
) {
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .rippleGold(onClick),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.05f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(accentColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 4. ListItemWithIcon
 * Categorical row element designed for Dua catalog lists, Quran indices, or track records.
 */
@Composable
fun ListItemWithIcon(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .rippleGold(onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(IslamicGold.copy(alpha = 0.10f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SoftGrey
                    )
                }
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Navigate",
                    tint = SoftGrey.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 5. GlassTopBar
 * Sleek blur/transparent upper navigation headers. Adds deep premium context to application bounds.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.background(Color.Transparent),
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
            navigationIconContentColor = Color.White,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

/**
 * 6. BottomNavBar
 * Glassmorphic Spring-animated Tab Selector Bar. Smoothly highlights selected options.
 */
@Composable
fun BottomNavBar(
    items: List<NavigationTabItem>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0x3B070A12),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(vertical = 10.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedTabIndex == index
                
                val animatedScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "BottomNavSpring"
                )

                val contentColor = if (isSelected) IslamicGold else SoftGrey

                Column(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = animatedScale
                            scaleY = animatedScale
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(index) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = contentColor
                    )
                }
            }
        }
    }
}

/**
 * Bottom Nav Auxiliary container.
 */
data class NavigationTabItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * 7. LoadingShimmer
 * Standardized loading state templates. Facilitates high-quality layout transitions.
 */
@Composable
fun LoadingShimmer(
    visible: Boolean,
    modifier: Modifier = Modifier,
    shape: CornerBasedShape = RoundedCornerShape(16.dp),
    content: @Composable () -> Unit
) {
    if (visible) {
        val transition = rememberInfiniteTransition(label = "ShimmerTransition")
        val translateAnim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ShimmerTranslate"
        )

        val shimmerBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.04f),
                Color.White.copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.04f)
            ),
            start = androidx.compose.ui.geometry.Offset(translateAnim - 250f, translateAnim - 250f),
            end = androidx.compose.ui.geometry.Offset(translateAnim + 250f, translateAnim + 250f)
        )

        Box(
            modifier = modifier
                .background(Color.White.copy(alpha = 0.03f), shape)
                .background(shimmerBrush, shape)
        )
    } else {
        content()
    }
}

/**
 * 8. IslamicDivider
 * An elegantly spaced line divider centered with an overlapping 8-point geometric Islamic star.
 */
@Composable
fun IslamicDivider(
    modifier: Modifier = Modifier,
    color: Color = IslamicGold.copy(alpha = 0.4f),
    thickness: Dp = 1.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(thickness)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, color)
                    )
                )
        )
        Canvas(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(16.dp)
        ) {
            val starSize = size.minDimension
            val radius = starSize / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f

            val path1 = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy - radius)
                lineTo(cx + radius * 0.707f, cy - radius * 0.707f)
                lineTo(cx + radius, cy)
                lineTo(cx + radius * 0.707f, cy + radius * 0.707f)
                lineTo(cx, cy + radius)
                lineTo(cx - radius * 0.707f, cy + radius * 0.707f)
                lineTo(cx - radius, cy)
                lineTo(cx - radius * 0.707f, cy - radius * 0.707f)
                close()
            }

            drawPath(
                path = path1,
                color = color,
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawCircle(
                color = color.copy(alpha = 0.8f),
                radius = 2.5f.dp.toPx(),
                center = center
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(thickness)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color, Color.Transparent)
                    )
                )
        )
    }
}
