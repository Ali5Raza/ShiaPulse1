package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.utils.LocalizationUtility
import com.example.utils.PrayerTimeCalculator
import java.util.Locale

private val PremiumHeroNight = Color(0xFF071B17)
private val PremiumHeroForest = Color(0xFF0D3D31)
private val PremiumHeroEmerald = Color(0xFF176451)
private val PremiumGold = Color(0xFFE8CB86)
private val PremiumGoldBright = Color(0xFFF4DFAC)
private val PremiumWhite = Color(0xFFFDFEFD)

internal val LocalHomeFeatureAccent = compositionLocalOf<Color?> { null }

/**
 * A composed, modern Home hierarchy: product header, daily verse and one focused
 * prayer card. Data sources, labels and actions remain those of the dashboard.
 */
@Composable
fun PremiumHomeHeader(
    viewModel: PrayerViewModel,
    times: PrayerTimeCalculator.PrayerTimes,
    countdownInfo: NextPrayerInfo,
    activeKey: String,
    currentTick: Long,
    currentLang: String,
    arabicUrduFontScale: Float,
    baseAppFontFamily: FontFamily,
    showDetailedTimings: Boolean,
    onDetailedTimingsChange: (Boolean) -> Unit,
    onLocationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onTaqeebatClick: () -> Unit,
    eventType: com.example.data.EventType? = null,
    modifier: Modifier = Modifier
) {
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val use24HourFormat by viewModel.use24HourFormat.collectAsState()
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumHomeBrandBar(
            currentLang = currentLang,
            isLoggedIn = isLoggedIn,
            userDisplayName = userDisplayName,
            onProfileClick = onProfileClick
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.72f))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = LocalizationUtility.get("verse_text", currentLang),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = (16 * arabicUrduFontScale).sp,
                lineHeight = (23 * arabicUrduFontScale).sp,
                fontWeight = FontWeight.Medium,
                fontFamily = baseAppFontFamily,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = LocalizationUtility.get("verse_source", currentLang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        PremiumPrayerFocusCard(
            times = times,
            countdownInfo = countdownInfo,
            activeKey = activeKey,
            currentTick = currentTick,
            currentLang = currentLang,
            arabicUrduFontScale = arabicUrduFontScale,
            baseAppFontFamily = baseAppFontFamily,
            locationName = selectedLocation.city,
            use24HourFormat = use24HourFormat,
            showDetailedTimings = showDetailedTimings,
            onDetailedTimingsChange = onDetailedTimingsChange,
            onLocationClick = onLocationClick,
            onTaqeebatClick = onTaqeebatClick,
            eventType = eventType,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun PremiumHomeBrandBar(
    currentLang: String,
    isLoggedIn: Boolean,
    userDisplayName: String?,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.shia_pulse_refined_1780463967811),
            contentDescription = "Shia Pulse Logo",
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Shia Pulse",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.2).sp,
                maxLines = 1
            )
            Text(
                text = LocalizationUtility.get("slogan", currentLang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    1.dp,
                    if (isLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    RoundedCornerShape(14.dp)
                )
                .clickable(onClick = onProfileClick)
                .testTag("profile_button_top"),
            contentAlignment = Alignment.Center
        ) {
            if (isLoggedIn) {
                Text(
                    text = (userDisplayName ?: "H").take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Sign In Profile Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumPrayerFocusCard(
    times: PrayerTimeCalculator.PrayerTimes,
    countdownInfo: NextPrayerInfo,
    activeKey: String,
    currentTick: Long,
    currentLang: String,
    arabicUrduFontScale: Float,
    baseAppFontFamily: FontFamily,
    locationName: String,
    use24HourFormat: Boolean,
    showDetailedTimings: Boolean,
    onDetailedTimingsChange: (Boolean) -> Unit,
    onLocationClick: () -> Unit,
    onTaqeebatClick: () -> Unit,
    eventType: com.example.data.EventType?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val heroAccent = if (eventType == com.example.data.EventType.SHAHADAT) {
        Color(0xFFE3B5B0)
    } else {
        PremiumGold
    }
    val heroAccentBright = if (eventType == com.example.data.EventType.SHAHADAT) {
        Color(0xFFF3D8D4)
    } else {
        PremiumGoldBright
    }
    val heroColors = when (eventType) {
        com.example.data.EventType.WILADAT -> listOf(
            Color(0xFF06231F), Color(0xFF075248), Color(0xFF00897B)
        )
        com.example.data.EventType.SHAHADAT -> listOf(
            Color(0xFF140E10), Color(0xFF2B2225), Color(0xFF4A3A3F)
        )
        else -> listOf(PremiumHeroNight, PremiumHeroForest, PremiumHeroEmerald)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(heroColors))
        ) {
            Image(
                painter = painterResource(id = R.drawable.mosque_background_1780218634256),
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.045f),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable(onClick = onLocationClick)
                            .padding(horizontal = 10.dp, vertical = 7.dp)
                            .testTag("location_indicator_header"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = heroAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = locationName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PremiumWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = countdownInfo.nextPrayerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "-${countdownInfo.countdownText}",
                            color = PremiumWhite,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 34.sp,
                            lineHeight = 40.sp,
                            letterSpacing = (-1).sp,
                            maxLines = 1
                        )
                    }
                    if (countdownInfo.nextPrayerTime.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = heroAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = LocalizationUtility.formatTimeUI(countdownInfo.nextPrayerTime, use24HourFormat),
                                color = heroAccentBright,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val timingList = remember(times, currentLang, use24HourFormat) {
                    listOf(
                        Triple(
                            "fajr",
                            LocalizationUtility.get("fajr", currentLang),
                            LocalizationUtility.formatTimeUI(times.fajrString, use24HourFormat)
                        ),
                        Triple(
                            "dhuhr",
                            LocalizationUtility.get("dhuhr", currentLang),
                            LocalizationUtility.formatTimeUI(times.dhuhrString, use24HourFormat)
                        ),
                        Triple(
                            "maghrib",
                            LocalizationUtility.get("maghrib", currentLang),
                            LocalizationUtility.formatTimeUI(times.maghribString, use24HourFormat)
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    timingList.forEach { (key, prayerName, prayerTime) ->
                        val nextName = countdownInfo.nextPrayerName
                        val activeMatch = when (key) {
                            "fajr" -> nextName.contains("Fajr", true) || nextName.contains("Sunrise", true)
                            "dhuhr" -> nextName.contains("Dhuhr", true) || nextName.contains("Asr", true) || nextName.contains("Sunset", true)
                            else -> nextName.contains("Maghrib", true) || nextName.contains("Isha", true) || nextName.contains("Midnight", true)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeMatch) Color.White.copy(alpha = 0.07f) else Color.Transparent)
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (activeMatch) heroAccent else Color.Transparent)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = prayerName,
                                fontSize = (11 * arabicUrduFontScale).sp,
                                fontWeight = if (activeMatch) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeMatch) heroAccentBright else Color.White.copy(alpha = 0.62f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = prayerTime,
                                fontSize = (15 * arabicUrduFontScale).sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = PremiumWhite,
                                maxLines = 1
                            )
                        }
                    }
                }

                val showTaqeebatButton = remember(activeKey) {
                    activeKey == "fajr" || activeKey == "dhuhr" || activeKey == "asr" ||
                        activeKey == "maghrib" || activeKey == "isha"
                }

                if (showTaqeebatButton) {
                    val prayerLabel = when (activeKey) {
                        "fajr" -> if (currentLang == "ur") "فجر" else if (currentLang == "ar") "الفجر" else "Fajr"
                        "dhuhr" -> if (currentLang == "ur") "ظہر" else if (currentLang == "ar") "الظهر" else "Zuhr"
                        "asr" -> if (currentLang == "ur") "عصر" else if (currentLang == "ar") "العصر" else "Asr"
                        "maghrib" -> if (currentLang == "ur") "مغرب" else if (currentLang == "ar") "المغرب" else "Maghrib"
                        "isha" -> if (currentLang == "ur") "عشاء" else if (currentLang == "ar") "العشاء" else "Isha"
                        else -> ""
                    }
                    val buttonText = when (currentLang) {
                        "ur" -> "$prayerLabel کے تعقیبات پڑھیں"
                        "ar" -> "اقرأ تعقيبات $prayerLabel"
                        else -> "Read $prayerLabel Taqeebat"
                    }
                    val todayStr = remember(currentTick) {
                        java.text.SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(java.util.Date())
                    }
                    var isTaqeebatCompleted by remember(activeKey, todayStr) {
                        mutableStateOf(
                            context.getSharedPreferences("taqeebat_prefs", Context.MODE_PRIVATE)
                                .getBoolean("taqeebat_completed_${todayStr}_${activeKey}", false)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 46.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (isTaqeebatCompleted) Color(0xFF359372)
                                else heroAccent
                            )
                            .clickable(onClick = onTaqeebatClick)
                            .padding(vertical = 11.dp, horizontal = 14.dp)
                            .testTag("read_taqeebat_quick_access_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isTaqeebatCompleted) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = if (isTaqeebatCompleted) Color.White else PremiumHeroNight,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buttonText,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isTaqeebatCompleted) Color.White else PremiumHeroNight,
                                fontFamily = baseAppFontFamily,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                            if (isTaqeebatCompleted) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentLang == "ur") "(مکمل)" else "(Completed)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.055f))
                        .clickable { onDetailedTimingsChange(!showDetailedTimings) }
                        .padding(vertical = 12.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showDetailedTimings) "Hide Detailed Timings" else "Show Detailed Shia Astronomical Timings",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Icon(
                        imageVector = if (showDetailedTimings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle Detailed Times",
                        tint = heroAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }

                AnimatedVisibility(
                    visible = showDetailedTimings,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    PremiumDetailedTimings(
                        times = times,
                        activeKey = activeKey,
                        currentLang = currentLang,
                        use24HourFormat = use24HourFormat,
                        arabicUrduFontScale = arabicUrduFontScale,
                        heroAccent = heroAccent,
                        heroAccentBright = heroAccentBright
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumDetailedTimings(
    times: PrayerTimeCalculator.PrayerTimes,
    activeKey: String,
    currentLang: String,
    use24HourFormat: Boolean,
    arabicUrduFontScale: Float,
    heroAccent: Color,
    heroAccentBright: Color
) {
    val details = remember(times, currentLang, use24HourFormat) {
        listOf(
            Triple("imsak", LocalizationUtility.get("imsak", currentLang), LocalizationUtility.formatTimeUI(times.imsakString, use24HourFormat)),
            Triple("fajr", LocalizationUtility.get("fajr", currentLang), LocalizationUtility.formatTimeUI(times.fajrString, use24HourFormat)),
            Triple("sunrise", LocalizationUtility.get("sunrise", currentLang), LocalizationUtility.formatTimeUI(times.sunriseString, use24HourFormat)),
            Triple("dhuhr", LocalizationUtility.get("dhuhr", currentLang), LocalizationUtility.formatTimeUI(times.dhuhrString, use24HourFormat)),
            Triple("asr", LocalizationUtility.get("asr", currentLang), LocalizationUtility.formatTimeUI(times.asrString, use24HourFormat)),
            Triple("sunset", LocalizationUtility.get("sunset", currentLang), LocalizationUtility.formatTimeUI(times.sunsetString, use24HourFormat)),
            Triple("maghrib", LocalizationUtility.get("maghrib", currentLang), LocalizationUtility.formatTimeUI(times.maghribString, use24HourFormat)),
            Triple("isha", LocalizationUtility.get("isha", currentLang), LocalizationUtility.formatTimeUI(times.ishaString, use24HourFormat)),
            Triple("midnight", LocalizationUtility.get("midnight", currentLang), LocalizationUtility.formatTimeUI(times.midnightString, use24HourFormat))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.055f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        details.forEach { (key, label, value) ->
            val isActive = key == activeKey
            val (timingIcon, iconColor) = when (key) {
                "imsak" -> Pair(Icons.Default.Alarm, heroAccent)
                "fajr" -> Pair(Icons.Default.WbTwilight, Color(0xFF9FC8E8))
                "sunrise" -> Pair(Icons.Default.WbSunny, Color(0xFFF3BD72))
                "dhuhr" -> Pair(Icons.Default.LightMode, heroAccent)
                "asr" -> Pair(Icons.Default.WbSunny, Color(0xFFE8AA63))
                "sunset" -> Pair(Icons.Default.WbTwilight, Color(0xFFE89B82))
                "maghrib" -> Pair(Icons.Default.NightsStay, Color(0xFFC2B2E2))
                "isha" -> Pair(Icons.Default.Bedtime, Color(0xFFA7B4DE))
                "midnight" -> Pair(Icons.Default.DarkMode, Color(0xFF96A3D2))
                else -> Pair(Icons.Default.AccessTime, Color.White)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isActive) Color.White.copy(alpha = 0.07f) else Color.Transparent)
                    .padding(horizontal = 7.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = timingIcon,
                        contentDescription = label,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = label + if (isActive) {
                            if (currentLang == "ur") " (موجودہ)" else if (currentLang == "ar") " (الحالي)" else " (Active)"
                        } else "",
                        fontSize = ((if (isActive) 14 else 13) * arabicUrduFontScale).sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) heroAccentBright else Color.White.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    fontSize = ((if (isActive) 14 else 13) * arabicUrduFontScale).sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isActive) heroAccentBright else Color.White.copy(alpha = 0.88f),
                    maxLines = 1
                )
            }
        }

        Text(
            text = "Note: Shia timings calculate Fajr at 16° twilight, Asr with shadow rules, and Maghrib precisely 4° (Eastern redness vanish) past sunset.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.44f),
            textAlign = TextAlign.Center,
            lineHeight = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 5.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun PremiumHomeCategoryMenu(
    worshipTitle: String,
    calculationsTitle: String,
    knowledgeTitle: String,
    baseFontFamily: FontFamily,
    onWorshipClick: () -> Unit,
    onCalculationsClick: () -> Unit,
    onKnowledgeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PremiumHomeCategoryRow(
            title = worshipTitle,
            icon = Icons.Default.Favorite,
            accent = colors.primary,
            baseFontFamily = baseFontFamily,
            onClick = onWorshipClick
        )
        PremiumHomeCategoryRow(
            title = calculationsTitle,
            icon = Icons.Default.Calculate,
            accent = colors.tertiary,
            baseFontFamily = baseFontFamily,
            onClick = onCalculationsClick
        )
        PremiumHomeCategoryRow(
            title = knowledgeTitle,
            icon = Icons.Default.Book,
            accent = colors.secondary,
            baseFontFamily = baseFontFamily,
            onClick = onKnowledgeClick
        )
    }
}

@Composable
private fun PremiumHomeCategoryRow(
    title: String,
    icon: ImageVector,
    accent: Color,
    baseFontFamily: FontFamily,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rowScale by animateFloatAsState(
        targetValue = if (isPressed) 0.985f else 1f,
        animationSpec = tween(140),
        label = "category_row_press"
    )
    val shape = RoundedCornerShape(22.dp)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(rowScale)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = if (isPressed) 0.34f else 0.16f)
        ),
        shadowElevation = if (isPressed) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.065f),
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .heightIn(min = 76.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accent.copy(alpha = 0.13f))
                    .border(1.dp, accent.copy(alpha = 0.10f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(23.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontFamily = baseFontFamily,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 26.sp,
                letterSpacing = 0.sp
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.09f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.90f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PremiumHomeCategoryPage(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    currentLang: String,
    baseFontFamily: FontFamily,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = LocalizationUtility.get("back", currentLang),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontFamily = baseFontFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = baseFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        CompositionLocalProvider(LocalHomeFeatureAccent provides accent) {
            content()
        }
        // Keep the final destination clear of the floating bottom navigation.
        Spacer(modifier = Modifier.height(104.dp))
    }
}

private data class PremiumNavItem(
    val icon: ImageVector,
    val contentDescription: String,
    val label: String
)

@Composable
fun PremiumBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    currentLang: String,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        PremiumNavItem(
            Icons.Default.Home,
            "Home",
            if (currentLang == "ur") "ہوم" else if (currentLang == "ar") "الرئيسية" else "Home"
        ),
        PremiumNavItem(
            Icons.Default.Favorite,
            "Worship",
            if (currentLang == "ur") "عبادات" else if (currentLang == "ar") "العبادات" else "Worship"
        ),
        PremiumNavItem(
            Icons.Default.MenuBook,
            "Quran",
            if (currentLang == "ur") "قرآن" else if (currentLang == "ar") "القرآن" else "Quran"
        ),
        PremiumNavItem(
            Icons.Default.Calculate,
            "Calculations",
            if (currentLang == "ur") "حسابات" else if (currentLang == "ar") "الحسابات" else "Calculations"
        ),
        PremiumNavItem(
            Icons.Default.Book,
            "Knowledge",
            if (currentLang == "ur") "معلومات" else if (currentLang == "ar") "المعرفة" else "Knowledge"
        )
    )

    Surface(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        shape = RoundedCornerShape(25.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 14.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEachIndexed { index, item ->
                val selected = selectedTab == index
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val itemScale by animateFloatAsState(
                    targetValue = if (pressed) 0.94f else 1f,
                    animationSpec = tween(140),
                    label = "nav_press_$index"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(250),
                    label = "nav_color_$index"
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .scale(itemScale)
                        .clip(RoundedCornerShape(19.dp))
                        .selectable(
                            selected = selected,
                            interactionSource = interactionSource,
                            indication = null,
                            role = Role.Tab
                        ) { onTabSelected(index) }
                        .padding(horizontal = 2.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 46.dp, height = 29.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.contentDescription,
                            tint = contentColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
