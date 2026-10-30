package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

private val WiladatDarkColorScheme = darkColorScheme(
    primary = WiladatPrimaryDark,
    onPrimary = WiladatOnPrimaryDark,
    primaryContainer = WiladatPrimaryContainerDark,
    onPrimaryContainer = WiladatOnPrimaryContainerDark,
    secondary = WiladatSecondaryDark,
    onSecondary = WiladatOnSecondaryDark,
    secondaryContainer = WiladatSecondaryContainerDark,
    onSecondaryContainer = WiladatOnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val WiladatLightColorScheme = lightColorScheme(
    primary = WiladatPrimaryLight,
    onPrimary = WiladatOnPrimaryLight,
    primaryContainer = WiladatPrimaryContainerLight,
    onPrimaryContainer = WiladatOnPrimaryContainerLight,
    secondary = WiladatSecondaryLight,
    onSecondary = WiladatOnSecondaryLight,
    secondaryContainer = WiladatSecondaryContainerLight,
    onSecondaryContainer = WiladatOnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

private val ShahadatDarkColorScheme = darkColorScheme(
    primary = ShahadatPrimaryDark,
    onPrimary = ShahadatOnPrimaryDark,
    primaryContainer = ShahadatPrimaryContainerDark,
    onPrimaryContainer = ShahadatOnPrimaryContainerDark,
    secondary = ShahadatSecondaryDark,
    onSecondary = ShahadatOnSecondaryDark,
    secondaryContainer = ShahadatSecondaryContainerDark,
    onSecondaryContainer = ShahadatOnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = ShahadatBackgroundDark,
    onBackground = OnBackgroundDark,
    surface = ShahadatSurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = ShahadatSurfaceVariantDark,
    onSurfaceVariant = ShahadatOnSurfaceVariantDark
)

private val ShahadatLightColorScheme = lightColorScheme(
    primary = ShahadatPrimaryLight,
    onPrimary = ShahadatOnPrimaryLight,
    primaryContainer = ShahadatPrimaryContainerLight,
    onPrimaryContainer = ShahadatOnPrimaryContainerLight,
    secondary = ShahadatSecondaryLight,
    onSecondary = ShahadatOnSecondaryLight,
    secondaryContainer = ShahadatSecondaryContainerLight,
    onSecondaryContainer = ShahadatOnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = ShahadatBackgroundLight,
    onBackground = OnBackgroundLight,
    surface = ShahadatSurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight
)

@Composable
fun ShiaPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    eventType: com.example.data.EventType? = null,
    appFontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
    content: @Composable () -> Unit
) {
    val colorScheme = when (eventType) {
        com.example.data.EventType.WILADAT -> if (darkTheme) WiladatDarkColorScheme else WiladatLightColorScheme
        com.example.data.EventType.SHAHADAT -> if (darkTheme) ShahadatDarkColorScheme else ShahadatLightColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    val finalTypography = if (appFontFamily != null) {
        getTypographyWithFont(appFontFamily)
    } else {
        Typography
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = finalTypography,
            content = content
        )
    }
}
