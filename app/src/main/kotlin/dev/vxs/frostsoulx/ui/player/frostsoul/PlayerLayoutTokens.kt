/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */
package dev.vxs.frostsoulx.ui.player.frostsoul

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Stable geometry and typography tokens for the full-screen FrostSoul player. */
internal object PlayerLayoutTokens {
    val MasterHorizontalPadding = 22.dp
    val VinylDiscSize = 310.dp
    val CenterAlbumArtSize = 190.dp

    /** Turntable geometry — the platter is inset inside the deck card, QQ-style. */
    val TurntableCardSize = 330.dp
    val TurntablePlatterSize = 262.dp
    val TurntablePolaroidOuterSize = 128.dp
    val TurntablePolaroidInnerSize = 104.dp
    val TurntableTonearmMountSize = 34.dp
    val TurntableTonearmElbowSize = 11.dp
    val TurntableRestPegSize = 13.dp
    val TurntableSpindleSize = 11.dp

    /** Artwork-blur header height, kept full-bleed so it melts into the page. Base dimension is
     * 342.dp; +42.dp accounts for the collapse-row height now reclaimed by the pager on the
     * Immersive main player page (see FrostSoulPlayer's isImmersiveArtworkMainPage), so the
     * artwork's top edge extends to the true screen top while its bottom edge — and everything
     * below it — stays exactly where it was before. */
    val ArtworkBlurHeaderHeight = 342.dp + 42.dp

    /** Lyrics typography and rhythm, tuned against the QQ Music lyric sheet. */
    val LyricsActiveFontSize = 21.sp
    val LyricsInactiveFontSize = 18.sp
    val LyricsLineHeight = 27.sp
    val LyricsLineSpacing = 15.dp
    val LyricsTextStartInset = 2.dp
    val LyricsTextEndInset = 16.dp
    val LyricsBottomControlsReserve = 128.dp

    val TrackTitleStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp,
        color = Color.White,
    )

    val ArtistSubtitleStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = Color.White.copy(alpha = 0.60f),
    )

    val TimelineTimeStyle = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        color = Color.White.copy(alpha = 0.40f),
    )
}
