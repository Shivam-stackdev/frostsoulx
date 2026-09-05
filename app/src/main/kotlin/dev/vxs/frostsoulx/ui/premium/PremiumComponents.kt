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
) {
    PremiumCard(
        modifier = modifier,
        shape = FrostSoulTheme.shapes.large,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            artworkUrl?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(FrostSoulTheme.effects.backdropBlurRadius)
                        .alpha(0.36f),
                )
            }
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(
                            FrostSoulTheme.colors.background.copy(alpha = 0.10f),
                            FrostSoulTheme.colors.background.copy(alpha = 0.90f),
                        ),
                    ),
                ),
            )
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.fillMaxSize().padding(20.dp),
            ) {
                Text(
                    text = "NOW PLAYING",
                    style = FrostSoulTheme.typography.overline,
                    color = FrostSoulTheme.colors.accentBright,
                )
                Spacer(modifier = Modifier.height(FrostSoulTheme.spacing.small))
                Text(
                    text = title,
                    style = FrostSoulTheme.typography.display.copy(fontSize = 23.sp, lineHeight = 28.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = FrostSoulTheme.typography.body,
                    color = Color.White.copy(alpha = 0.76f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (positionLabel != null && durationLabel != null) {
                    Spacer(modifier = Modifier.height(FrostSoulTheme.spacing.medium))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(FrostSoulTheme.shapes.pill)
                                .background(Color.White.copy(alpha = 0.28f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                                    .height(3.dp)
                                    .background(Color.White, FrostSoulTheme.shapes.pill),
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(positionLabel, style = FrostSoulTheme.typography.label, color = Color.White.copy(alpha = 0.68f))
                            Text(durationLabel, style = FrostSoulTheme.typography.label, color = Color.White.copy(alpha = 0.68f))
                        }
                    }
                }
                onPlayPause?.let { onClick ->
                    Spacer(modifier = Modifier.height(FrostSoulTheme.spacing.medium))
                    PremiumIconAvatar(
                        painter = androidx.compose.ui.res.painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        size = 44.dp,
                        tint = FrostSoulTheme.colors.onSurface,
                        containerColor = FrostSoulTheme.colors.accentBright,
                        modifier = Modifier.clickable(onClick = onClick),
                    )
                }
            }
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
