package com.example.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R

/** Bundled open-licensed Urdu Nastaliq; no network download is needed at runtime. */
val NotoNastaliqUrduFont = FontFamily(
    Font(R.font.noto_nastaliq_urdu, weight = FontWeight.Normal)
)

/** Classical Arabic faces used by Qur'an, Dua and Cards Maker content. */
val AmiriFont = FontFamily(
    Font(R.font.amiri_regular, weight = FontWeight.Normal),
    Font(R.font.amiri_bold, weight = FontWeight.Bold)
)

val NotoNaskhArabicFont = FontFamily(
    Font(R.font.noto_naskh_arabic, weight = FontWeight.Normal)
)

// Keep the existing Indo-Pak preference working with the bundled open Naskh face.
val NoorehudaFont = NotoNaskhArabicFont

fun getAppFontFamily(fontName: String): FontFamily? {
    return when (fontName.trim().lowercase()) {
        "amiri" -> AmiriFont
        "noto naskh", "noto naskh arabic" -> NotoNaskhArabicFont
        "nastaliq", "nafees nastaleeq", "jameel noori", "urdu typesetting" -> NotoNastaliqUrduFont
        "noorehuda", "indo-pak" -> NoorehudaFont
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "sansserif", "sans-serif", "roboto" -> FontFamily.SansSerif
        else -> FontFamily.Default
    }
}

/**
 * Resolves the active app font without overriding the language's script needs.
 * Urdu always receives Nastaliq; Arabic defaults to readable Naskh unless Amiri
 * was explicitly selected. Existing font preferences remain supported.
 */
fun getAppFontFamily(fontName: String, languageCode: String?): FontFamily {
    return when (languageCode) {
        "ur" -> NotoNastaliqUrduFont
        "ar" -> if (fontName.equals("amiri", ignoreCase = true)) AmiriFont else NotoNaskhArabicFont
        else -> getAppFontFamily(fontName) ?: FontFamily.Default
    }
}
