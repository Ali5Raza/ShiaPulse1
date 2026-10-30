package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.HijriCalendarHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ToolbarCalendarWidget(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkBackground: Boolean = true,
    compact: Boolean = false
) {
    val context = LocalContext.current
    var isPastMaghrib by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isPastMaghrib = HijriCalendarHelper.isPastMaghribToday(context)
    }

    val cal = remember { Calendar.getInstance() }
    val gregFormat = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val gregDateStr = remember { gregFormat.format(cal.time) }

    val currentExtraOffset = if (isPastMaghrib) 1 else 0
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)

    val hijriDate = remember(HijriCalendarHelper.globalOffsetDays, isPastMaghrib) {
        HijriCalendarHelper.convertGregorianToHijri(
            year, month, day,
            HijriCalendarHelper.globalOffsetDays + currentExtraOffset
        )
    }
    val hijriDateStr = "${hijriDate.day} ${hijriDate.monthName} ${hijriDate.year} AH"

    val eventInfo = remember(hijriDate) {
        HijriCalendarHelper.getUpcomingShiaEvent(context, currentExtraOffset)
    }

    if (compact) {
        // Sticky compact toolbar bar when scrolled
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            shadowElevation = 6.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("toolbar_calendar_compact")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🌙 $hijriDateStr",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${eventInfo.displayText}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (eventInfo.isWiladat) Color(0xFF00C853) else if (eventInfo.isShahadat) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Open Islamic Calendar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    } else {
        // Full Widget style card (matches home-screen widget layout 1:1)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isDarkBackground) Color.White.copy(alpha = 0.16f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
                .border(
                    width = 1.dp,
                    color = if (isDarkBackground) Color.White.copy(alpha = 0.35f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onClick)
                .padding(14.dp)
                .testTag("toolbar_calendar_widget_card")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header: Gregorian Date & Shia Pulse Live Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = gregDateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkBackground) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CALENDAR",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkBackground) Color(0xFF80CBC4) else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Open Calendar",
                            tint = if (isDarkBackground) Color(0xFF80CBC4) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Prominent Hijri Date (Matches widget_hijri_date)
                Text(
                    text = "🌙 $hijriDateStr",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isDarkBackground) Color.White else MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Upcoming Shia Event / Occasion Banner (Matches widget_event_text)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isDarkBackground) Color.Black.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = eventInfo.displayText,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkBackground) {
                            if (eventInfo.isWiladat) Color(0xFFA5D6A7)
                            else if (eventInfo.isShahadat) Color(0xFFFFCC80)
                            else Color(0xFFFFE082)
                        } else {
                            if (eventInfo.isWiladat) Color(0xFF2E7D32)
                            else if (eventInfo.isShahadat) Color(0xFFC62828)
                            else MaterialTheme.colorScheme.primary
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
