package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepNightBlue
import com.example.ui.theme.ShiaPulseTheme

/**
 * A themed preview wrapper designed to automatically inherit ShiaPulse theme tokens and
 * apply deep night background washes. Essential to wrap all system Composables in Android Studio previews.
 */
@Composable
fun PreviewWrapper(
    darkTheme: Boolean = true,
    eventType: com.example.data.EventType? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    ShiaPulseTheme(darkTheme = darkTheme, eventType = eventType) {
        val bgBrush = if (darkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    DeepNightBlue,
                    Color(0xFF070A11) // Deeper rich dark finish
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8F9FA),
                    Color(0xFFEDEFF2)
                )
            )
        }
        
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(16.dp)
        ) {
            content()
        }
    }
}
