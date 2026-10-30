package com.example.ui.academy.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.withAppFont

val AcademyCyan = Color(0xFF00BCD4)
val AcademyOrange = Color(0xFFFF9800)
val AcademyGreen = Color(0xFF8BC34A)
val AcademyPink = Color(0xFFF06292)
val AcademyPurple = Color(0xFFAB47BC)

val AcademyLightColors = lightColorScheme(
    primary = Color(0xFF007C91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F7FA),
    onPrimaryContainer = Color(0xFF006064),
    secondary = Color(0xFFB65D00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3E0),
    onSecondaryContainer = Color(0xFF8A4300),
    tertiary = Color(0xFF527A22),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF1F8E9),
    onTertiaryContainer = Color(0xFF33691E),
    error = Color(0xFFBA1A1A),
    background = Color(0xFFFDFDFD),
    surface = Color.White
)

private val AcademyDarkColors = darkColorScheme(
    primary = Color(0xFF78DBED),
    secondary = Color(0xFFFFB86B),
    tertiary = Color(0xFFBBD994),
    background = Color(0xFF10171C),
    surface = Color(0xFF192229),
    surfaceVariant = Color(0xFF29343C),
    onBackground = Color(0xFFE6EDF2),
    onSurface = Color(0xFFE6EDF2),
    onSurfaceVariant = Color(0xFFC0CBD3)
)

object AcademyGradients {
    val magicalSky = Brush.verticalGradient(colors = listOf(Color(0xFFE1F5FE), Color(0xFFB3E5FC)))
    val sunsetWarm = Brush.verticalGradient(colors = listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3)))
    val peacefulNature = Brush.verticalGradient(colors = listOf(Color(0xFFF1F8E9), Color(0xFFDCEDC8)))
    val royalLearning = Brush.verticalGradient(colors = listOf(Color(0xFFF3E5F5), Color(0xFFE1BEE7)))
    val gentlePink = Brush.verticalGradient(colors = listOf(Color(0xFFFCE4EC), Color(0xFFF8BBD0)))
}

val AcademyTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 42.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp, letterSpacing = 0.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 27.sp, letterSpacing = 0.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp)
)

val AcademyShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(48.dp)
)

@Composable
fun AcademyTheme(
    currentLang: String = "en",
    appFontFamily: FontFamily = FontFamily.Default,
    content: @Composable () -> Unit
) {
    // Detect the surrounding ShiaPulse theme before replacing it with the child-friendly palette.
    val useDarkColors = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val direction = if (currentLang == "ur" || currentLang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = AcademyTypography.withAppFont(appFontFamily)
    CompositionLocalProvider(LocalLayoutDirection provides direction) {
        MaterialTheme(
            colorScheme = if (useDarkColors) AcademyDarkColors else AcademyLightColors,
            typography = typography,
            shapes = AcademyShapes,
            content = content
        )
    }
}
