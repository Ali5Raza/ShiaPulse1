package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Calendar

@Composable
fun TrackerHeaderCalendar(
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    // Top Image Header
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.praying_person_tracker_1784715520706),
            contentDescription = "Tracker Header",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Month and Year Text
        val monthYearFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        val monthYearStr = monthYearFormat.format(selectedDate.time)
        
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = monthYearStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }
    }

    // Weekly Calendar Row
    // Generate the week days for the week containing selectedDate
    val currentCal = selectedDate.clone() as Calendar
    currentCal.firstDayOfWeek = Calendar.MONDAY
    currentCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    
    val weekDates = (0..6).map { 
        val cal = currentCal.clone() as Calendar
        cal.add(Calendar.DAY_OF_YEAR, it)
        cal 
    }
    
    Card(
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dayFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
            
            weekDates.forEach { dateObj ->
                val isSelected = dateObj.get(Calendar.DAY_OF_YEAR) == selectedDate.get(Calendar.DAY_OF_YEAR) &&
                                 dateObj.get(Calendar.YEAR) == selectedDate.get(Calendar.YEAR)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDateSelected(dateObj) }
                        .padding(4.dp)
                ) {
                    Text(
                        text = dayFormat.format(dateObj.time).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Text(
                            text = "${dateObj.get(Calendar.DAY_OF_MONTH)}",
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
