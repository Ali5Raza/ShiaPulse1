package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Arabic & English premium font families
val ArabicFontFamily = NotoNaskhArabicFont
val EnglishFontFamily = FontFamily.SansSerif

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 50.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = ArabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.15).sp
    ),
    titleMedium = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = EnglishFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.15.sp
    )
)

/**
 * Applies an app font without allowing large script font metrics to inflate UI controls.
 * Nastaliq remains available for Urdu, while explicit content line heights stay unchanged.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
fun Typography.withAppFont(fontFamily: FontFamily): Typography {
    val isScriptFont = fontFamily == NotoNastaliqUrduFont ||
        fontFamily == NotoNaskhArabicFont ||
        fontFamily == AmiriFont
    val platformStyle = androidx.compose.ui.text.PlatformTextStyle(
        includeFontPadding = false
    )
    val lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
        alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
        trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
    )

    fun TextStyle.withSelectedFont(): TextStyle = copy(
        fontFamily = fontFamily,
        letterSpacing = if (isScriptFont) 0.sp else letterSpacing,
        platformStyle = platformStyle,
        lineHeightStyle = lineHeightStyle
    )

    return copy(
        displayLarge = displayLarge.withSelectedFont(),
        displayMedium = displayMedium.withSelectedFont(),
        displaySmall = displaySmall.withSelectedFont(),
        headlineLarge = headlineLarge.withSelectedFont(),
        headlineMedium = headlineMedium.withSelectedFont(),
        headlineSmall = headlineSmall.withSelectedFont(),
        titleLarge = titleLarge.withSelectedFont(),
        titleMedium = titleMedium.withSelectedFont(),
        titleSmall = titleSmall.withSelectedFont(),
        bodyLarge = bodyLarge.withSelectedFont(),
        bodyMedium = bodyMedium.withSelectedFont(),
        bodySmall = bodySmall.withSelectedFont(),
        labelLarge = labelLarge.withSelectedFont(),
        labelMedium = labelMedium.withSelectedFont(),
        labelSmall = labelSmall.withSelectedFont()
    )
}

fun getTypographyWithFont(fontFamily: FontFamily): Typography = Typography.withAppFont(fontFamily)
