package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CompactPrayerItem(
    prayerName: String,
    status: String,
    onStatusChange: (String) -> Unit,
    currentLang: String,
    isObligatory: Boolean
) {
    val localizedName = getLocalizedPrayerName(prayerName, currentLang)
    
    val boxColor = when (prayerName) {
        "Fajr" -> Color(0xFF4FC3F7) 
        "Dhuhr" -> Color(0xFFFFD54F) 
        "Asr" -> Color(0xFFFF8A65) 
        "Maghrib" -> Color(0xFF7986CB) 
        "Isha" -> Color(0xFF1E88E5) 
        else -> Color(0xFF90A4AE) 
    }
    
    Card(
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icons on the left
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Not Prayed (Reset)
                IconButton(
                    onClick = { onStatusChange("NotPrayed") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Not Prayed",
                        tint = if (status == "NotPrayed") MaterialTheme.colorScheme.error else Color.LightGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (isObligatory) {
                    // Jamaat
                    IconButton(
                        onClick = { onStatusChange(if (status == "Jamaat") "NotPrayed" else "Jamaat") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "Jamaat",
                            tint = if (status == "Jamaat") boxColor else Color.Gray,
                            modifier = Modifier.size(if (status == "Jamaat") 32.dp else 24.dp)
                        )
                    }
                }
                
                // Individual (Prayed)
                IconButton(
                    onClick = { onStatusChange(if (status == "Prayed") "NotPrayed" else "Prayed") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Individual",
                        tint = if (status == "Prayed") boxColor else Color.Gray,
                        modifier = Modifier.size(if (status == "Prayed") 32.dp else 24.dp)
                    )
                }
                
                // Qada
                IconButton(
                    onClick = { onStatusChange(if (status == "Qada") "NotPrayed" else "Qada") },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Qada",
                        tint = if (status == "Qada") Color.Red else Color.Gray,
                        modifier = Modifier.size(if (status == "Qada") 32.dp else 24.dp)
                    )
                }
            }
            
            // Colored Name Block on the right
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(110.dp)
                    .background(boxColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = localizedName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}
