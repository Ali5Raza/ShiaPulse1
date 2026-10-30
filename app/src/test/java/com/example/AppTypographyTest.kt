package com.example

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import com.example.ui.theme.NotoNastaliqUrduFont
import com.example.ui.theme.Typography as AppTypography
import com.example.ui.theme.getAppFontFamily
import com.example.ui.theme.getTypographyWithFont
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalTextApi::class)
class AppTypographyTest {
    @Test
    fun nastaliqDoesNotInflateAnyStandardTextStyle() {
        val styled = getTypographyWithFont(NotoNastaliqUrduFont)

        AppTypography.allStyles().zip(styled.allStyles()).forEachIndexed { index, (base, actual) ->
            assertEquals("Font size at style $index", base.fontSize, actual.fontSize)
            assertEquals("Line height at style $index", base.lineHeight, actual.lineHeight)
            assertEquals("Font family at style $index", NotoNastaliqUrduFont, actual.fontFamily)
            assertEquals(
                "Line-height trimming at style $index",
                LineHeightStyle.Trim.Both,
                actual.lineHeightStyle?.trim
            )
        }
    }

    @Test
    fun urduStillUsesBundledNastaliq() {
        assertEquals(
            NotoNastaliqUrduFont,
            getAppFontFamily(fontName = "system", languageCode = "ur")
        )
    }

    private fun Typography.allStyles(): List<TextStyle> = listOf(
        displayLarge,
        displayMedium,
        displaySmall,
        headlineLarge,
        headlineMedium,
        headlineSmall,
        titleLarge,
        titleMedium,
        titleSmall,
        bodyLarge,
        bodyMedium,
        bodySmall,
        labelLarge,
        labelMedium,
        labelSmall
    )
}
