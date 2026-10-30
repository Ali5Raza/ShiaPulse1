package com.example.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.QiblaCalculator
import com.example.utils.LocalizationUtility
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@Composable
fun CompassScreen(
    viewModel: PrayerViewModel,
    azimuth: Float,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val currentLang by viewModel.appLanguage.collectAsState()
    var tabIndex by remember { mutableStateOf(0) }
    var showCalibrationDialog by remember { mutableStateOf(false) }
    var selectedDesign by remember { mutableIntStateOf(0) }

    val qiblaBearing = remember(selectedLocation) {
        QiblaCalculator.calculateQiblaBearing(selectedLocation.lat, selectedLocation.lon)
    }

    // Relative angle matches target relative to top
    val relativeAngle = ((qiblaBearing - azimuth + 360f) % 360f).toFloat()

    val smoothAngle by animateFloatAsState(
        targetValue = relativeAngle,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val isAligned = abs(relativeAngle) <= 4.0 || abs(relativeAngle - 360f) <= 4.0

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Qibla Compass", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize().clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Status top Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isAligned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isAligned) LocalizationUtility.get("aligned_kaaba", currentLang) else LocalizationUtility.get("qibla_compass", currentLang),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isAligned) LocalizationUtility.get("aligned_success", currentLang) else LocalizationUtility.get("slow_rotate", currentLang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isAligned) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom Premium Choice Segment switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { tabIndex = 0 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tabIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = LocalizationUtility.get("compass_dial", currentLang),
                    color = if (tabIndex == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = { tabIndex = 1 },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (tabIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = LocalizationUtility.get("map_pin_view", currentLang),
                    color = if (tabIndex == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (tabIndex == 0) {
            // Visual digital compass Dial
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = MaterialTheme.colorScheme.primary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 20.dp.toPx()

                    if (selectedDesign == 0) {
                        // 1. Premium Dark Emerald & Gold
                        val dialBg = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color(0xFF122C22), Color(0xFF04120D)),
                            center = center,
                            radius = radius * 1.2f
                        )
                        drawCircle(brush = dialBg, radius = radius + 8.dp.toPx(), center = center)
                        
                        val rimGlow = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(Color(0xFFFFD54F), Color(0xFFFBC02D), Color(0xFFFFD54F)),
                            center = center
                        )
                        drawCircle(brush = rimGlow, radius = radius + 8.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx()))
                        drawCircle(color = Color(0xFFFFF59D).copy(alpha = 0.2f), radius = radius + 11.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))

                        rotate(degrees = -azimuth, pivot = center) {
                            drawSubTics(center, radius, Color(0xFFE0E0E0)) // Bright tics
                        }
                        rotate(degrees = smoothAngle, pivot = center) {
                            val needleLength = radius * 0.85f
                            drawLine(
                                color = (if (isAligned) Color(0xFF00E676) else Color(0xFFFFD54F)).copy(alpha = 0.8f),
                                start = center,
                                end = Offset(center.x, center.y - needleLength),
                                strokeWidth = 2.dp.toPx()
                            )
                            val arrowPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(center.x, center.y - needleLength + 10.dp.toPx())
                                lineTo(center.x - 8.dp.toPx(), center.y - needleLength + 30.dp.toPx())
                                lineTo(center.x + 8.dp.toPx(), center.y - needleLength + 30.dp.toPx())
                                close()
                            }
                            drawPath(path = arrowPath, color = if (isAligned) Color(0xFF00E676) else Color(0xFFFFD54F))

                            // Premium Kaaba icon
                            val kaabaSize = 34.dp.toPx()
                            val kLeft = center.x - kaabaSize / 2f
                            val kTop = center.y - needleLength - kaabaSize
                            drawRect(color = Color(0xFF0A0A0A), topLeft = Offset(kLeft, kTop), size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize))
                            drawRect(color = Color(0xFFFFD54F), topLeft = Offset(kLeft, kTop + 6.dp.toPx()), size = androidx.compose.ui.geometry.Size(kaabaSize, 4.dp.toPx()))
                            val doorW = 7.dp.toPx()
                            val doorH = 12.dp.toPx()
                            drawRect(color = Color(0xFFFFD54F), topLeft = Offset(center.x - doorW / 2f, kTop + kaabaSize - doorH - 4.dp.toPx()), size = androidx.compose.ui.geometry.Size(doorW, doorH))
                        }
                        // Jeweled Center Hub
                        drawCircle(color = Color(0xFFFFD54F), radius = 10.dp.toPx(), center = center)
                        drawCircle(color = Color(0xFF122C22), radius = 5.dp.toPx(), center = center)
                    } else if (selectedDesign == 1) {
                        // 2. Vibrant Ruby Compass
                        val rubyPrimary = Color(0xFFE53935)
                        val rubyDark = Color(0xFFB71C1C)
                        val bgGradient = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(Color(0xFF212121), Color(0xFF121212)),
                            center = center,
                            radius = radius
                        )
                        drawCircle(brush = bgGradient, radius = radius + 6.dp.toPx(), center = center)
                        drawCircle(
                            color = rubyPrimary,
                            radius = radius + 6.dp.toPx(),
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                        rotate(degrees = -azimuth, pivot = center) {
                            drawSubTics(center, radius, rubyPrimary.copy(alpha = 0.8f))
                        }
                        rotate(degrees = smoothAngle, pivot = center) {
                            val needleLength = radius * 0.85f
                            val arrow = androidx.compose.ui.graphics.Path().apply {
                                moveTo(center.x, center.y - needleLength)
                                lineTo(center.x - 14.dp.toPx(), center.y + 24.dp.toPx())
                                lineTo(center.x, center.y + 12.dp.toPx())
                                lineTo(center.x + 14.dp.toPx(), center.y + 24.dp.toPx())
                                close()
                            }
                            drawPath(path = arrow, brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(rubyPrimary, rubyDark),
                                startY = center.y - needleLength,
                                endY = center.y + 24.dp.toPx()
                            ))
                            
                            // Kaaba icon at tip
                            val kaabaSize = 30.dp.toPx()
                            val kLeft = center.x - kaabaSize / 2f
                            val kTop = center.y - needleLength - kaabaSize
                            drawRect(color = Color(0xFF0F0F0F), topLeft = Offset(kLeft, kTop), size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize))
                            drawRect(color = Color(0xFFFFD54F), topLeft = Offset(kLeft, kTop + 5.dp.toPx()), size = androidx.compose.ui.geometry.Size(kaabaSize, 3.dp.toPx()))
                            val doorW = 6.dp.toPx()
                            val doorH = 10.dp.toPx()
                            drawRect(color = Color(0xFFFFD54F), topLeft = Offset(center.x - doorW / 2f, kTop + kaabaSize - doorH - 3.dp.toPx()), size = androidx.compose.ui.geometry.Size(doorW, doorH))
                        }
                        drawCircle(color = rubyPrimary, radius = 12.dp.toPx(), center = center)
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = center)
                    } else if (selectedDesign == 2) {
                        // 3. Ultra-Minimalist White/Silver Modern
                        val silverGradient = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(Color(0xFFE0E0E0), Color(0xFFFFFFFF), Color(0xFFBDBDBD), Color(0xFFFFFFFF), Color(0xFFE0E0E0)),
                            center = center
                        )
                        drawCircle(brush = androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color.White, Color(0xFFF5F5F5)), center, radius), radius = radius + 10.dp.toPx(), center = center)
                        drawCircle(brush = silverGradient, radius = radius + 10.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx()))
                        drawCircle(color = Color.Gray, radius = radius + 6.dp.toPx(), center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                        
                        rotate(degrees = -azimuth, pivot = center) {
                            drawSubTics(center, radius - 12.dp.toPx(), Color(0xFF424242))
                        }
                        rotate(degrees = smoothAngle, pivot = center) {
                            val needleLength = radius * 0.75f
                            val needleWidth = 14.dp.toPx()
                            val arrowPathNord = androidx.compose.ui.graphics.Path().apply {
                                moveTo(center.x, center.y - needleLength)
                                lineTo(center.x - needleWidth, center.y)
                                lineTo(center.x + needleWidth, center.y)
                                close()
                            }
                            val arrowPathSud = androidx.compose.ui.graphics.Path().apply {
                                moveTo(center.x, center.y + needleLength)
                                lineTo(center.x - needleWidth, center.y)
                                lineTo(center.x + needleWidth, center.y)
                                close()
                            }
                            drawPath(path = arrowPathNord, color = Color(0xFF1976D2)) // Blue north for mecca! Actually usually red is north.
                            drawPath(path = arrowPathSud, color = Color(0xFF9E9E9E))  // Silver south
                            
                            // Kaaba icon at tip
                            val kaabaSize = 28.dp.toPx()
                            val kLeft = center.x - kaabaSize / 2f
                            val kTop = center.y - needleLength - kaabaSize - 5.dp.toPx()
                            drawRect(color = Color(0xFF151515), topLeft = Offset(kLeft, kTop), size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize))
                            drawRect(color = Color(0xFFFFD54F), topLeft = Offset(kLeft, kTop + 5.dp.toPx()), size = androidx.compose.ui.geometry.Size(kaabaSize, 3.dp.toPx()))
                            val doorW = 5.dp.toPx()
                            val doorH = 9.dp.toPx()
                            drawRect(color = Color(0xFFFFD54F), topLeft = Offset(center.x - doorW / 2f, kTop + kaabaSize - doorH - 2.dp.toPx()), size = androidx.compose.ui.geometry.Size(doorW, doorH))
                        }
                        // Jewel
                        drawCircle(color = Color(0xFF424242), radius = 10.dp.toPx(), center = center)
                        drawCircle(color = Color(0xFFE0E0E0), radius = 5.dp.toPx(), center = center)
                    }
                }

                // Central layout reading
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(top = 170.dp)
                ) {
                    Text(
                        text = "${relativeAngle.toInt()}°",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = LocalizationUtility.get("to_kaaba", currentLang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Design selector inspired by user request
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                items(3) { index ->
                    val isSelected = selectedDesign == index
                    val primaryColor = MaterialTheme.colorScheme.primary
                    
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) primaryColor else Color.Gray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .background(Color.White.copy(alpha = if (isSelected) 0.1f else 0.05f))
                            .clickable { selectedDesign = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(50.dp)) {
                            val r = size.width / 2f
                            val c = Offset(size.width/2f, size.height/2f)
                            
                            // Draw mini version based on index
                            if (index == 0) {
                                // Mini Premium Dark Emerald & Gold
                                drawCircle(brush = androidx.compose.ui.graphics.Brush.radialGradient(listOf(Color(0xFF122C22), Color(0xFF04120D)), c, r), radius = r)
                                drawCircle(color = Color(0xFFFFD54F), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                                drawLine(color = Color(0xFFFFD54F), start = c, end = Offset(c.x, c.y - r * 0.7f), strokeWidth = 1.dp.toPx())
                                drawRect(color = Color(0xFF0A0A0A), topLeft = Offset(c.x - 3.dp.toPx(), c.y - r * 0.7f - 6.dp.toPx()), size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 6.dp.toPx()))
                                drawCircle(color = Color(0xFFFFD54F), radius = 2.dp.toPx(), center = c)
                            } else if (index == 1) {
                                // Mini Vibrant Ruby
                                val ruby = Color(0xFFE53935)
                                drawCircle(color = Color(0xFF121212), radius = r)
                                drawCircle(color = ruby, radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                                val p = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(c.x, c.y - r * 0.7f); lineTo(c.x - 3.dp.toPx(), c.y + 4.dp.toPx()); lineTo(c.x, c.y + 2.dp.toPx()); lineTo(c.x + 3.dp.toPx(), c.y + 4.dp.toPx()); close()
                                }
                                drawPath(path = p, color = ruby)
                                drawRect(color = Color(0xFF0A0A0A), topLeft = Offset(c.x - 3.dp.toPx(), c.y - r * 0.7f - 6.dp.toPx()), size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 6.dp.toPx()))
                            } else if (index == 2) {
                                // Mini White/Silver
                                drawCircle(color = Color(0xFFF5F5F5), radius = r)
                                drawCircle(color = Color(0xFFBDBDBD), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                                val pn = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(c.x, c.y - r * 0.65f); lineTo(c.x - 3.dp.toPx(), c.y); lineTo(c.x + 3.dp.toPx(), c.y); close()
                                }
                                val ps = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(c.x, c.y + r * 0.65f); lineTo(c.x - 3.dp.toPx(), c.y); lineTo(c.x + 3.dp.toPx(), c.y); close()
                                }
                                drawPath(path = pn, color = Color(0xFF1976D2))
                                drawPath(path = ps, color = Color(0xFF9E9E9E))
                                drawRect(color = Color(0xFF0A0A0A), topLeft = Offset(c.x - 3.dp.toPx(), c.y - r * 0.65f - 6.dp.toPx()), size = androidx.compose.ui.geometry.Size(6.dp.toPx(), 6.dp.toPx()))
                                drawCircle(color = Color(0xFF424242), radius = 2.dp.toPx(), center = c)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Specs Panel
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = LocalizationUtility.get("geographic_calcs", currentLang),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(LocalizationUtility.get("selected_reference", currentLang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selectedLocation.city, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(LocalizationUtility.get("kaaba_direction", currentLang), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${qiblaBearing.toInt()}° " + LocalizationUtility.get("east_north", currentLang), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        } else {
            // 2. Qibla Map Pin View
            val kmDistance = remember(selectedLocation) {
                // Haversine formula
                val R = 6371.0
                val lat1Rad = Math.toRadians(selectedLocation.lat)
                val lon1Rad = Math.toRadians(selectedLocation.lon)
                val lat2Rad = Math.toRadians(21.4225)
                val lon2Rad = Math.toRadians(39.8262)
                
                val dLat = lat2Rad - lat1Rad
                val dLon = lon2Rad - lon1Rad
                
                val a = sin(dLat / 2) * sin(dLat / 2) +
                        cos(lat1Rad) * cos(lat2Rad) *
                        sin(dLon / 2) * sin(dLon / 2)
                val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
                R * c
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = LocalizationUtility.get("global_qibla_map", currentLang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${selectedLocation.city} ➔ " + LocalizationUtility.get("mecca_kaaba", currentLang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = String.format("%,.1f KM", kmDistance),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Interactive Geographic Radar Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val mapPrimary = MaterialTheme.colorScheme.primary
                        val mapSecondary = MaterialTheme.colorScheme.secondary
                        val mapOnSurface = MaterialTheme.colorScheme.onSurface
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val mapCenter = Offset(size.width / 2f, size.height / 2f)
                            val mapRadius = size.width / 2.2f - 20.dp.toPx()
                            
                            // 1. Draw concentric global coordinate rings
                            drawCircle(
                                color = mapOnSurface.copy(alpha = 0.05f),
                                radius = mapRadius,
                                center = mapCenter
                            )
                            drawCircle(
                                color = mapOnSurface.copy(alpha = 0.1f),
                                radius = mapRadius * 0.6f,
                                center = mapCenter,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            )
                            drawCircle(
                                color = mapOnSurface.copy(alpha = 0.15f),
                                radius = mapRadius * 0.3f,
                                center = mapCenter,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.dp.toPx()
                                )
                            )
                            
                            // 2. Draw Latitude/Longitude grid lines
                            drawLine(
                                color = mapOnSurface.copy(alpha = 0.1f),
                                start = Offset(0f, mapCenter.y),
                                end = Offset(size.width, mapCenter.y),
                                strokeWidth = 1.dp.toPx()
                            )
                            drawLine(
                                color = mapOnSurface.copy(alpha = 0.1f),
                                start = Offset(mapCenter.x, 0f),
                                end = Offset(mapCenter.x, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                            
                            // 3. Draw Mecca / Kaaba Target position relative to the correct bearing
                            val radAngle = Math.toRadians(qiblaBearing.toDouble() - 90.0) // Rotate -90 so 0 is up
                            val kx = mapCenter.x + mapRadius * cos(radAngle).toFloat()
                            val ky = mapCenter.y + mapRadius * sin(radAngle).toFloat()
                            val kOffset = Offset(kx, ky)
                            
                            // Draw connecting line with glowing gradient
                            drawLine(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(mapSecondary, mapPrimary)
                                ),
                                start = mapCenter,
                                end = kOffset,
                                strokeWidth = 3.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                            )
                            
                            // 4. Draw User Position Pin (Pulse ring + center pin)
                            drawCircle(
                                color = mapSecondary.copy(alpha = 0.3f),
                                radius = 14.dp.toPx(),
                                center = mapCenter
                            )
                            drawCircle(
                                color = mapSecondary,
                                radius = 6.dp.toPx(),
                                center = mapCenter
                            )
                            
                            // 5. Draw Kaaba Location Target Pin with beautiful graphic
                            drawCircle(
                                color = mapPrimary.copy(alpha = 0.2f),
                                radius = 18.dp.toPx(),
                                center = kOffset
                            )
                            drawCircle(
                                color = mapPrimary,
                                radius = 10.dp.toPx(),
                                center = kOffset
                            )
                            
                            // Small Gold/Black Kaaba Cube symbol at target
                            val kaabaW = 12.dp.toPx()
                            drawRect(
                                color = Color(0xFF212121),
                                topLeft = Offset(kx - kaabaW / 2, ky - kaabaW / 2),
                                size = androidx.compose.ui.geometry.Size(kaabaW, kaabaW)
                            )
                            // Kiswah Gold band
                            drawLine(
                                color = Color(0xFFFFD54F),
                                start = Offset(kx - kaabaW / 2, ky - kaabaW / 4),
                                end = Offset(kx + kaabaW / 2, ky - kaabaW / 4),
                                strokeWidth = 1.5f.dp.toPx()
                            )
                        }
                        
                        // Small overlays inside Map Row
                        Text(
                            text = LocalizationUtility.get("reference_pin", currentLang) + "\n(${selectedLocation.city})",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = 22.dp)
                        )
                        
                        Text(
                            text = LocalizationUtility.get("holy_kaaba", currentLang) + "\n(" + LocalizationUtility.get("mecca_sa", currentLang) + ")",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = mapPrimary,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(
                                    x = (110 * cos(Math.toRadians(qiblaBearing.toDouble() - 90.0))).dp,
                                    y = (110 * sin(Math.toRadians(qiblaBearing.toDouble() - 90.0)) - 22.0).dp
                                )
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .clickable { showCalibrationDialog = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Guidance",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Tap here to view Compass Calibration steps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
        }
        
        if (showCalibrationDialog) {
            AlertDialog(
                onDismissRequest = { showCalibrationDialog = false },
                title = {
                    Text(
                        text = "Compass Calibration",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "To ensure the Qibla direction is highly accurate, calibrate your device's compass sensor:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape).size(24.dp), contentAlignment = Alignment.Center) {
                                Text("1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Hold your device comfortably in your hand.", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape).size(24.dp), contentAlignment = Alignment.Center) {
                                Text("2", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Move the device in a figure 8 motion (∞) smoothly through the air.", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape).size(24.dp), contentAlignment = Alignment.Center) {
                                Text("3", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("Repeat 3 to 4 times until the compass needle moves steadily.", style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Text(
                            text = "Note: Keep away from metal surfaces, magnets, or electronics that may cause magnetic interference.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCalibrationDialog = false }) {
                        Text("Got it", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSubTics(
    center: Offset,
    radius: Float,
    color: Color,
    drawLabels: Boolean = true
) {
    val textPaint = Paint().apply {
        this.color = color.toArgb()
        textSize = 16.dp.toPx()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    for (angleDeg in 0 until 360 step 15) {
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val isCardinal = angleDeg % 90 == 0
        val tickLength = if (isCardinal) 12.dp.toPx() else 6.dp.toPx()
        val tickWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx()

        val startX = center.x + (radius - tickLength) * sin(angleRad).toFloat()
        val startY = center.y - (radius - tickLength) * cos(angleRad).toFloat()

        val endX = center.x + radius * sin(angleRad).toFloat()
        val endY = center.y - radius * cos(angleRad).toFloat()

        drawLine(
            color = if (isCardinal) color else color.copy(alpha = 0.4f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = tickWidth
        )

        if (drawLabels && isCardinal) {
            val labelStr = when(angleDeg) {
                0 -> "N"
                90 -> "E"
                180 -> "S"
                270 -> "W"
                else -> ""
            }
            if (labelStr.isNotEmpty()) {
                val labelDistance = radius - 30.dp.toPx()
                val lx = center.x + labelDistance * sin(angleRad).toFloat()
                val ly = center.y - labelDistance * cos(angleRad).toFloat() + 6.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(labelStr, lx, ly, textPaint)
            }
        }
    }
}
