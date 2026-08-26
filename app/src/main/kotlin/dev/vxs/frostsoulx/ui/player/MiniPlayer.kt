/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import dev.vxs.frostsoulx.ui.frostsoul.FSText as Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vxs.frostsoulx.LocalPlayerConnection
import dev.vxs.frostsoulx.R
import androidx.datastore.preferences.core.booleanPreferencesKey
import dev.vxs.frostsoulx.extensions.togglePlayPause
import dev.vxs.frostsoulx.ui.player.frostsoul.FSGlassCard
import dev.vxs.frostsoulx.ui.player.frostsoul.FSIconButton
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulOnSurface
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulOnSurfaceMuted
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulPage
import dev.vxs.frostsoulx.ui.player.frostsoul.FrostSoulTrack
import dev.vxs.frostsoulx.ui.player.frostsoul.FSMiniPlayer
import dev.vxs.frostsoulx.ui.player.frostsoul.rememberFrostSoulPalette
import dev.vxs.frostsoulx.utils.rememberPreference
import kotlin.math.abs

internal enum class MiniPlayerPeekState {
    Collapsed,
    Peeked,
}

private const val MiniPlayerCollapsedHeight = 72
private const val MiniPlayerPeekHeight = 110
private const val MiniPlayerCollapsedArtwork = 48
private const val MiniPlayerPeekArtwork = 64
private const val SmartPeekTimeoutMs = 2_000L
private const val ResumePeekThresholdMs = 1_500L

@Composable
fun MiniPlayer(
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier,
    isPairedWithNavigation: Boolean = false,
    onQueueClick: (() -> Unit)? = null,
    onOpenFullPlayer: () -> Unit = {},
    onSmartPeekChanged: (Boolean) -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val metadata = mediaMetadata ?: return
    val palette = rememberFrostSoulPalette(metadata.thumbnailUrl)
    val lifecycleOwner = LocalLifecycleOwner.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val smartPeekPreferenceKey = remember { booleanPreferencesKey("mini_player_smart_peek_enabled") }
    val (smartPeekEnabled, setSmartPeekEnabled) = rememberPreference(smartPeekPreferenceKey, true)

    var peekState by remember(metadata.id) { mutableStateOf(MiniPlayerPeekState.Collapsed) }
    var quickMenuVisible by remember { mutableStateOf(false) }
    var userInteracting by remember { mutableStateOf(false) }
    var autoCollapseGeneration by remember { mutableIntStateOf(0) }
    var resumeEvent by remember { mutableIntStateOf(0) }
    var previousMetadataId by remember { mutableStateOf<String?>(null) }
    var previousIsPlaying by remember { mutableStateOf(false) }
    var pauseStartedAt by remember { mutableLongStateOf(0L) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragDistanceX by remember { mutableFloatStateOf(0f) }
    var dragDistanceY by remember { mutableFloatStateOf(0f) }

    fun triggerSmartPeek() {
        if (!smartPeekEnabled) return
        peekState = MiniPlayerPeekState.Peeked
        autoCollapseGeneration += 1
    }

    LaunchedEffect(metadata.id, isPlaying, smartPeekEnabled) {
        val now = System.currentTimeMillis()
        val newSong = previousMetadataId != null && previousMetadataId != metadata.id
        val resumedAfterPause =
            isPlaying &&
                !previousIsPlaying &&
                pauseStartedAt > 0L &&
                now - pauseStartedAt >= ResumePeekThresholdMs
        if (smartPeekEnabled && (previousMetadataId == null || newSong || resumedAfterPause)) {
            triggerSmartPeek()
        }
        if (!isPlaying) pauseStartedAt = now
        previousMetadataId = metadata.id
        previousIsPlaying = isPlaying
    }

    DisposableEffect(lifecycleOwner, isPlaying) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME && isPlaying) resumeEvent += 1
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeEvent, smartPeekEnabled) {
        if (resumeEvent > 0 && isPlaying) triggerSmartPeek()
    }

    LaunchedEffect(peekState, autoCollapseGeneration, smartPeekEnabled) {
        if (peekState == MiniPlayerPeekState.Peeked && smartPeekEnabled) {
            kotlinx.coroutines.delay(SmartPeekTimeoutMs)
            if (!userInteracting) peekState = MiniPlayerPeekState.Collapsed
        }
    }

    LaunchedEffect(peekState) {
        onSmartPeekChanged(peekState == MiniPlayerPeekState.Peeked)
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            userInteracting = true
            if (peekState == MiniPlayerPeekState.Collapsed) triggerSmartPeek()
        } else if (userInteracting) {
            userInteracting = false
            if (peekState == MiniPlayerPeekState.Peeked && smartPeekEnabled) autoCollapseGeneration += 1
        }
    }

    val peeked = peekState == MiniPlayerPeekState.Peeked
    val animatedHeight by animateDpAsState(
        targetValue = if (peeked) MiniPlayerPeekHeight.dp else MiniPlayerCollapsedHeight.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f),
        label = "frostsoul-mini-player-height",
    )
    val animatedArtworkSize by animateDpAsState(
        targetValue = if (peeked) MiniPlayerPeekArtwork.dp else MiniPlayerCollapsedArtwork.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 460f),
        label = "frostsoul-mini-player-artwork-size",
    )
    val settledOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f),
        label = "frostsoul-mini-player-swipe-offset",
    )
    val shape =
        if (isPairedWithNavigation) {
            RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        } else {
            RoundedCornerShape(16.dp)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .graphicsLayer { translationX = settledOffsetX }
                .pointerInput(metadata.id, isPlaying) {
                    detectDragGestures(
                        onDragStart = {
                            dragDistanceX = 0f
                            dragDistanceY = 0f
                            userInteracting = true
                            if (peekState == MiniPlayerPeekState.Collapsed) triggerSmartPeek()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDistanceX += dragAmount.x
                            dragDistanceY += dragAmount.y
                            dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-220f, 220f)
                        },
                        onDragEnd = {
                            val horizontalGesture = abs(dragDistanceX) > maxOf(abs(dragDistanceY), 52f)
                            val verticalDismiss =
                                dragDistanceY > 64f && abs(dragDistanceY) > abs(dragDistanceX) && !isPlaying
                            if (horizontalGesture) {
                                if (dragDistanceX < 0f) playerConnection.seekToNext() else playerConnection.seekToPrevious()
                            } else if (verticalDismiss) {
                                peekState = MiniPlayerPeekState.Collapsed
                                onSmartPeekChanged(false)
                                playerConnection.service.stopAndClearPlayback(clearPersistentState = true)
                            }
                            dragOffsetX = 0f
                            dragDistanceX = 0f
                            dragDistanceY = 0f
                            userInteracting = false
                            if (peekState == MiniPlayerPeekState.Peeked && smartPeekEnabled) autoCollapseGeneration += 1
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                            dragDistanceX = 0f
                            dragDistanceY = 0f
                            userInteracting = false
                        },
                    )
                },
    ) {
        FSMiniPlayer(
            track = FrostSoulTrack.from(metadata, currentSong?.song?.liked == true),
            positionMs = position,
            durationMs = duration,
            isPlaying = isPlaying,
            palette = palette,
            height = animatedHeight,
            artworkSize = animatedArtworkSize,
            peeked = peeked,
            shape = shape,
            interactionSource = interactionSource,
            onCardClick = {
                onSmartPeekChanged(false)
                onOpenFullPlayer()
            },
            onLongPress = { quickMenuVisible = true },
            onTogglePlayPause = { playerConnection.player.togglePlayPause() },
            onToggleLike = playerConnection::toggleLike,
            onQueueClick = onQueueClick,
        )

        AnimatedVisibility(
            visible = quickMenuVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            FSGlassCard(
                accent = palette.accent,
                modifier = Modifier.width(224.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(
                        text = "QUICK MENU",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                    )
                    MiniPlayerMenuAction(
                        label = if (currentSong?.song?.liked == true) "Remove favorite" else "Add to favorites",
                        onClick = {
                            playerConnection.toggleLike()
                            quickMenuVisible = false
                        },
                    )
                    MiniPlayerMenuAction(
                        label = "Open queue",
                        onClick = {
                            onQueueClick?.invoke()
                            quickMenuVisible = false
                        },
                    )
                    MiniPlayerMenuAction(
                        label = if (smartPeekEnabled) "Disable Smart Peek" else "Enable Smart Peek",
                        onClick = {
                            val enabled = !smartPeekEnabled
                            setSmartPeekEnabled(enabled)
                            if (enabled) triggerSmartPeek()
                            quickMenuVisible = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerMenuAction(
    label: String,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = FrostSoulOnSurface,
        fontSize = 13.sp,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 9.dp),
    )
}
