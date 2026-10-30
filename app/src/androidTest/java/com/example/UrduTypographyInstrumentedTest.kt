package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ui.theme.NotoNastaliqUrduFont
import com.example.ui.theme.ShiaPulseTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UrduTypographyInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test(timeout = 15_000)
    fun nastaliqDoesNotInflateStandardUiTextHeight() {
        composeRule.setContent {
            Column {
                ShiaPulseTheme(appFontFamily = FontFamily.SansSerif) {
                    Text(
                        text = "رہنمائی اہلبیت",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        modifier = Modifier.testTag("standard_ui_text")
                    )
                }
                ShiaPulseTheme(
                    appFontFamily = NotoNastaliqUrduFont,
                    layoutDirection = LayoutDirection.Rtl
                ) {
                    Text(
                        text = "رہنمائی اہلبیت",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        modifier = Modifier.testTag("urdu_ui_text")
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val standardBounds = composeRule.onNodeWithTag("standard_ui_text")
            .getUnclippedBoundsInRoot()
        val urduBounds = composeRule.onNodeWithTag("urdu_ui_text")
            .getUnclippedBoundsInRoot()
        val standardHeight = standardBounds.bottom - standardBounds.top
        val urduHeight = urduBounds.bottom - urduBounds.top

        assertTrue(
            "Urdu UI text expanded from $standardHeight to $urduHeight",
            urduHeight <= standardHeight + 1.dp
        )
    }
}
