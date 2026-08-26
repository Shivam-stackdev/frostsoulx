/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.frostsoul

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Search-specific tokens for the low-profile full-page search surface. */
object SearchTheme {
    val SearchBarBackground = Color(0xFF121212)
    val SearchBarBackgroundLight = Color(0xFFF4F5F4)
    val InputTextStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = Color.White,
    )
}

internal val SearchInputTextStyle = SearchTheme.InputTextStyle
internal val SearchInputTextStyleLight = SearchTheme.InputTextStyle.copy(color = Color(0xFF18201C))
