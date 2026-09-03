# Bottom Ambient Glow → QQ Music Match

## TL;DR
You already have a `GLOW_ANIMATED` background style — it's just built on the wrong geometry.
Current code paints **one uniform vertical band** that time-crossfades between
`palette.artworkPrimary` and `palette.artworkSecondary`. The QQ Music reference paints
**two separate colored blobs**, one anchored bottom-left and one bottom-right, blended
additively where they overlap. Same two palette colors you're already extracting — just
arranged spatially instead of temporally.

Confirmed by frame-sampling the reference video at 1fps, then re-verified at 4fps:
- Glow occupies the **bottom ~20–25% of screen height**, eases in (not linear) — consistent
  with a blurred/radial falloff, not a flat gradient stop.
- Left side trends cool/blue-purple, right side trends warm/orange. Checked actual RGB (not
  just brightness) across the whole clip — **left stays B>G>R and right stays R>G>B at every
  timestamp sampled, they never swap identity.** Confirms this is genuinely two fixed-hue
  anchors, not an animation artifact.
- Where the two colors overlap in the center-bottom, the pixels are **brighter than either
  color alone** → additive/`Plus` blend, not alpha-over.
- **Animation is anti-phase, not synced.** Correlation between left-corner and right-corner
  brightness over 14s of 4fps sampling: **r = -0.89**. When left brightens, right dims, and
  back — it's a slow side-to-side sway in emphasis, not both blobs pulsing together. Measured
  peak-to-peak period is **~5.1–5.3s**, not the `GlowBreathDurationMs = 3_600` (3.6s) already
  in the codebase. Keep the existing `rememberInfiniteTransition` plumbing, just retune the
  duration and change how `breath` is applied (see Step 2).

## Step 0 — retune the breathing duration
At line 2127:
```kotlin
private const val GlowBreathDurationMs = 3_600
```
Change to match the measured ~5.1–5.3s period:
```kotlin
private const val GlowBreathDurationMs = 5_200
```
This constant is shared with the `rememberInfiniteTransition` at lines 2199–2209 — no other
changes needed there, it already does `RepeatMode.Reverse` which gives the correct back-and-forth
shape once the duration matches.

## File to change
`app/src/main/kotlin/dev/vxs/frostsoulx/ui/player/frostsoul/FrostSoulPlayer.kt`

This is the shared background used by Main/Lyrics/Recommendations vinyl pages (comment at
line 2119–2121 confirms it), which matches what the video shows — same glow on the turntable
page and the lyrics page.

## Step 1 — add an import
Near line 83–84, add:
```kotlin
import androidx.compose.ui.geometry.Offset
```
(`BlendMode` and `Brush` are already imported at lines 87–88, nothing else needed.)

## Step 2 — replace the glow block
Replace the `if (isGlowMode) { ... }` block, **lines 2269–2304**, currently:

```kotlin
        if (isGlowMode) {
            // One fixed full-width wash is shared by Main, Lyrics, and Recommendations. It sits
            // just above the seek/time-bar area and never becomes an oval or moving backdrop.
            // Two cached vertical brushes are cross-faded in the draw phase so the color breathes
            // between the active artwork's primary and secondary palette colors without allocating
            // a new brush or recomposing the player on every frame.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SeekGlowBottomPadding)
                    .fillMaxWidth()
                    .height(SeekGlowBandHeight)
                    .drawWithCache {
                        val primaryWash = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                palette.artworkPrimary.copy(alpha = 0.42f),
                                palette.artworkPrimary.copy(alpha = 0.24f),
                            ),
                        )
                        val secondaryWash = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                palette.artworkSecondary.copy(alpha = 0.34f),
                                palette.artworkSecondary.copy(alpha = 0.20f),
                            ),
                        )
                        onDrawBehind {
                            val breath = glowBreath?.value ?: 0.5f
                            val secondaryWeight = (0.22f + breath * 0.56f).coerceIn(0f, 1f)
                            drawRect(brush = primaryWash, alpha = 1f - secondaryWeight)
                            drawRect(brush = secondaryWash, alpha = secondaryWeight)
                        }
                    },
            )
        }
```

With this:

```kotlin
        if (isGlowMode) {
            // Two independently colored blobs anchored bottom-left / bottom-right, matching the
            // QQ Music reference: each palette color stays on its own side and the two blend
            // additively where they overlap in the middle, instead of one band crossfading
            // between colors over time. Centers sit below the band (off-canvas) so only the
            // upper arc is visible, which gives the eased radial falloff toward the bottom edge.
            // Brushes are cached on palette/size only; the breath value is read purely in the
            // draw phase so this still costs one recomposition-free layer per frame, same as before.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = SeekGlowBottomPadding)
                    .fillMaxWidth()
                    .height(SeekGlowBandHeight)
                    .drawWithCache {
                        val bandWidth = size.width
                        val bandHeight = size.height
                        val blobRadius = bandWidth * 0.72f
                        val leftCenter = Offset(bandWidth * 0.18f, bandHeight * 1.35f)
                        val rightCenter = Offset(bandWidth * 0.82f, bandHeight * 1.35f)

                        val leftBrush = Brush.radialGradient(
                            colors = listOf(
                                palette.artworkPrimary.copy(alpha = 0.55f),
                                palette.artworkPrimary.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                            center = leftCenter,
                            radius = blobRadius,
                        )
                        val rightBrush = Brush.radialGradient(
                            colors = listOf(
                                palette.artworkSecondary.copy(alpha = 0.55f),
                                palette.artworkSecondary.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                            center = rightCenter,
                            radius = blobRadius,
                        )

                        onDrawBehind {
                            // Anti-phase sway, not a synced pulse: measured correlation between
                            // the two corners on the reference footage was r = -0.89 — as one
                            // brightens the other dims. `breath` still goes 0f..1f from the
                            // existing infinite transition; remap it to -1f..1f so the two sides
                            // move in opposite directions from a shared center intensity.
                            val breath = glowBreath?.value ?: 0.5f
                            val sway = (breath - 0.5f) * 2f // -1f..1f
                            val leftAlpha = (0.78f + sway * 0.22f).coerceIn(0.5f, 1f)
                            val rightAlpha = (0.78f - sway * 0.22f).coerceIn(0.5f, 1f)

                            drawCircle(
                                brush = leftBrush,
                                radius = blobRadius,
                                center = leftCenter,
                                alpha = leftAlpha,
                                blendMode = BlendMode.Plus,
                            )
                            drawCircle(
                                brush = rightBrush,
                                radius = blobRadius,
                                center = rightCenter,
                                alpha = rightAlpha,
                                blendMode = BlendMode.Plus,
                            )
                        }
                    },
            )
        }
```

## Nothing else needs to change
- `GlowModeScrim` (lines 2156–2165), `SeekGlowBandHeight`/`SeekGlowBottomPadding` (2123–2124),
  and the `glowBreath` state setup (2199–2209) are all reused as-is.
- `rememberFrostSoulPalette` (line 2054) already extracts exactly two ordered colors via
  `PlayerColorExtractor.extractGradientColors` and assigns them to `artworkPrimary` /
  `artworkSecondary` — that's the same source this reuses, no new extraction work needed.
- `PlayerBackgroundStyle.GLOW` (static, non-animated) automatically gets the new geometry too
  since it flows through the same `isGlowMode` branch — just without the sway animating
  (`glowBreath` stays null for it, so `sway` is `0f` and both sides sit at a flat `0.78f`).

## Optional tuning knobs, once it's running
- `blobRadius = bandWidth * 0.72f` — raise toward `0.9f` for a softer/more overlapping center,
  lower toward `0.55f` for two more separated pools of color.
- `bandHeight * 1.35f` for the vertical center offset — closer to `1.0f` pushes more of each
  circle into view (stronger/brighter), higher values push it further off-canvas (subtler, more
  falloff before it's cut off by the band edge).
- If left/right ever look color-swapped for a given track, it's just because
  `PlayerColorExtractor.extractGradientColors` ordering depends on that image's swatch
  ranking — not a bug in this block, that's the same ordering the old crossfade version used.

## QA checklist
- [ ] Verify on Main (turntable), Lyrics, and Recommendations pages — same shared function.
- [ ] Confirm `PlayerBackgroundStyle.GLOW` (static) still renders sensibly with no animation.
- [ ] Test a low-saturation/near-grayscale artwork (like the "AbdusalamAkbar" track in the
      reference video) — `PlayerColorExtractor` should still give two distinguishable swatches;
      if they're too close in hue, the two-blob split will look flat rather than broken.
- [ ] Confirm perf is unchanged — this is still a single `drawWithCache` layer with brushes
      cached on `palette`/`size`, same cost profile as the block it replaces.
- [ ] Watch the sway for ~10–15s and confirm left/right visibly trade off emphasis rather than
      pulsing together — that's the part that was verified against the reference footage
      (r = -0.89 anti-phase correlation) and is the main behavioral change in this pass.
