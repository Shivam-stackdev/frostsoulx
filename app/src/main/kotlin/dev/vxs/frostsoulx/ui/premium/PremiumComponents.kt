/*
 * FrostSoulX premium component language.
 * These primitives intentionally reuse FrostSoulTheme tokens so Home, Settings,
 * Library, and quick-action surfaces share one visual vocabulary.
 */
package dev.vxs.frostsoulx.ui.premium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.vxs.frostsoulx.R
import dev.vxs.frostsoulx.ui.frostsoul.FSAlbumArt
import dev.vxs.frostsoulx.ui.frostsoul.FrostSoulTheme
import dev.vxs.frostsoulx.ui.frostsoul.frostSoulGlow

@Composable
fun PremiumTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FrostSoulTheme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = FrostSoulTheme.spacing.page, vertical = FrostSoulTheme.spacing.large),
    ) {
        navigationIcon?.invoke()
        Column(modifier = Modifier.weight(1f)) {
            eyebrow?.let {
                Text(
                    text = it.uppercase(),
                    style = FrostSoulTheme.typography.overline,
                    color = FrostSoulTheme.colors.accentMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(FrostSoulTheme.spacing.micro))
            }
            Text(
                text = title,
                style = FrostSoulTheme.typography.display.copy(fontSize = 30.sp, lineHeight = 36.sp),
                color = FrostSoulTheme.colors.onBackground,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = FrostSoulTheme.typography.body,
                    color = FrostSoulTheme.colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailingContent?.invoke(this)
    }
}

@Composable
fun PremiumSegmentedTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FrostSoulTheme.spacing.small),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = FrostSoulTheme.spacing.page, vertical = FrostSoulTheme.spacing.small),
    ) {
        labels.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val selectedTextColor =
                if (FrostSoulTheme.colors.accentBright.luminance() > 0.5f) Color.Black else Color.White
            Text(
                text = label,
                style = FrostSoulTheme.typography.label,
                color = if (selected) selectedTextColor else FrostSoulTheme.colors.onSurfaceMuted,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier
                    .clip(FrostSoulTheme.shapes.pill)
                    .background(
                        if (selected) FrostSoulTheme.colors.accentBright else Color.Transparent,
                        FrostSoulTheme.shapes.pill,
                    )
                    .clickable(onClick = { onSelected(index) })
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
fun PremiumIconAvatar(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    iconSize: Dp = size * 0.56f,
    tint: Color = FrostSoulTheme.colors.accentBright,
    containerColor: Color = FrostSoulTheme.colors.surfaceRaised,
    shape: Shape = CircleShape,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor, shape)
            .frostSoulGlow(tint, alpha = 0.08f),
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    shape: Shape = FrostSoulTheme.shapes.medium,
    contentPadding: PaddingValues = PaddingValues(FrostSoulTheme.spacing.large),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(FrostSoulTheme.colors.surfaceGlassStrong, shape)
            .border(1.dp, FrostSoulTheme.colors.outline.copy(alpha = 0.52f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun PremiumListRow(
    title: String,
    subtitle: String? = null,
    artworkUrl: String? = null,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = FrostSoulTheme.shapes.medium
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FrostSoulTheme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (isActive) FrostSoulTheme.colors.accent.copy(alpha = 0.12f) else Color.Transparent,
                shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = FrostSoulTheme.spacing.medium, vertical = FrostSoulTheme.spacing.small),
    ) {
        if (leading != null) {
            leading()
        } else if (artworkUrl != null) {
            FSAlbumArt(
                artworkUrl = artworkUrl,
                contentDescription = title,
                modifier = Modifier.size(54.dp),
                shape = FrostSoulTheme.shapes.small,
            )
        } else {
            PremiumIconAvatar(
                painter = androidx.compose.ui.res.painterResource(R.drawable.music_note),
                contentDescription = null,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = FrostSoulTheme.typography.body,
                color = if (isActive) FrostSoulTheme.colors.accentBright else FrostSoulTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = FrostSoulTheme.typography.bodyMuted,
                    color = FrostSoulTheme.colors.onSurfaceMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 2.dp),
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun PremiumHeroBanner(
    artworkUrl: String?,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    progress: Float = 0f,
    positionLabel: String? = null,
    durationLabel: String? = null,
    onPlayPause: (() -> Unit)? = null,
    isLiked: Boolean = false,
    shuffleEnabled: Boolean = false,
    canSkipPrevious: Boolean = false,
    canSkipNext: Boolean = false,
    onPrevious: () -> Unit = {},
    onNext: () -> Unit = {},
    onShuffle: () -> Unit = {},
    onLike: () -> Unit = {},
    onLyrics: (() -> Unit)? = null,
    onMore: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().clip(shape)
            .background(Color(0xFF17191F))
            .border(1.dp, Brush.linearGradient(listOf(Color(0xFF9B8060), Color(0xFF353743))), shape),
    ) {
        val artworkSize = (maxWidth * 0.27f).coerceIn(72.dp, 120.dp)
        AsyncImage(
            model = artworkUrl, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().blur(28.dp).alpha(0.70f),
        )
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(
            listOf(Color(0xFF694A2C).copy(alpha = 0.50f), Color(0xFF101522).copy(alpha = 0.84f)),
        )))
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("NOW PLAYING", color = Color.White.copy(alpha = 0.8f), fontSize = 8.sp,
                    letterSpacing = 1.sp, modifier = Modifier.weight(1f))
                onLyrics?.let { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.heightIn(min = 48.dp).clickable(onClick = action).padding(horizontal = 6.dp),
                    ) {
                        Icon(androidx.compose.ui.res.painterResource(R.drawable.music_note), null,
                            tint = Color.White, modifier = Modifier.size(13.dp))
                        Text("Lyrics", color = Color.White, fontSize = 10.sp)
                    }
                }
                onMore?.let { action ->
                    IconButton(onClick = action) {
                        Icon(androidx.compose.ui.res.painterResource(R.drawable.more_vert), "Song options",
                            tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = FrostSoulTheme.typography.display.copy(fontFamily = FontFamily.Serif),
                        color = Color.White, fontSize = 23.sp, lineHeight = 27.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, color = Color.White.copy(alpha = 0.68f), fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.30f))) {
                        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(2.dp).background(Color(0xFFF7DEAF)))
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(positionLabel.orEmpty(), color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                        Text(durationLabel.orEmpty(), color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        // Equal flexible slots keep five accessible actions within narrow cards.
                        HeroTransport(R.drawable.shuffle, "Toggle shuffle", onShuffle, Modifier.weight(1f),
                            tint = if (shuffleEnabled) Color(0xFFF7DEAF) else Color.White)
                        HeroTransport(R.drawable.skip_previous, "Previous track", onPrevious, Modifier.weight(1f), enabled = canSkipPrevious)
                        HeroTransport(if (isPlaying) R.drawable.pause else R.drawable.play,
                            if (isPlaying) "Pause" else "Play", { onPlayPause?.invoke() }, Modifier.weight(1f),
                            enabled = onPlayPause != null, outlined = true)
                        HeroTransport(R.drawable.skip_next, "Next track", onNext, Modifier.weight(1f), enabled = canSkipNext)
                        HeroTransport(if (isLiked) R.drawable.favorite else R.drawable.favorite_border,
                            if (isLiked) "Remove from favorites" else "Add to favorites", onLike, Modifier.weight(1f),
                            tint = if (isLiked) Color(0xFFFF747C) else Color.White)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(artworkSize)) {
                    FSAlbumArt(artworkUrl = artworkUrl, contentDescription = "Artwork for $title",
                        modifier = Modifier.size(artworkSize), shape = RoundedCornerShape(10.dp))
                    Text("Some songs stay forever.", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f),
                        style = FrostSoulTheme.typography.body.copy(fontFamily = FontFamily.Serif),
                        modifier = Modifier.padding(top = 7.dp))
                }
            }
        }
    }
}

@Composable
private fun HeroTransport(
    icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    tint: Color = Color.White,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier.height(48.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).then(
            if (outlined) Modifier.background(Color.White.copy(alpha = 0.10f), CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.65f), CircleShape) else Modifier,
        )) {
            Icon(androidx.compose.ui.res.painterResource(icon), description,
                tint = tint.copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PremiumSearchBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search songs, albums, artists...",
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(FrostSoulTheme.spacing.medium),
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(FrostSoulTheme.shapes.pill)
            .background(FrostSoulTheme.colors.surfaceRaised, FrostSoulTheme.shapes.pill)
            .border(1.dp, FrostSoulTheme.colors.outline.copy(alpha = 0.62f), FrostSoulTheme.shapes.pill)
            .clickable(onClick = onClick)
            .padding(horizontal = FrostSoulTheme.spacing.large),
    ) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(R.drawable.search),
            contentDescription = null,
            tint = FrostSoulTheme.colors.onSurfaceMuted,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = placeholder,
            style = FrostSoulTheme.typography.body,
            color = FrostSoulTheme.colors.onSurfaceMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
