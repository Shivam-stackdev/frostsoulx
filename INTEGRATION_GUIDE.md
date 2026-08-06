# Lyrics Features Integration Guide

## Files Overview

| File | Destination |
|------|-------------|
| `ArchiveTuneMediaNotificationProvider.kt` | `app/src/main/kotlin/moe/rukamori/archivetune/playback/` |
| `LyricsPreferences.kt` | `app/src/main/kotlin/moe/rukamori/archivetune/utils/` |
| `LyricsAdapter.kt` | `app/src/main/kotlin/moe/rukamori/archivetune/ui/lyrics/` (create dir if needed) |
| `MusicService_LyricsHook.kt.txt` | Snippets to paste into `MusicService.kt` |
| `notification_player_big.xml` | `app/src/main/res/layout/` |
| `item_lyric_line.xml` | `app/src/main/res/layout/` |
| `preferences_lyrics_additions.xml` | Paste into your existing settings XML |

## Step-by-Step Integration

### 1. Copy Files
Copy all `.kt` files to their respective packages and `.xml` files to `res/`.

### 2. Integrate MusicService Hooks
Open `MusicService.kt` and apply the snippets from `MusicService_LyricsHook.kt.txt`:
- Add imports
- Add properties
- Replace notification provider init
- Add `startLyricsSync()` / `stopLyricsSync()`
- Hook into `onIsPlayingChanged` and `onMediaItemTransition`

### 3. Connect Your Lyrics UI
In your lyrics screen (wherever you display lyrics), use `LyricsAdapter`:

```kotlin
val adapter = LyricsAdapter()
lyricsRecyclerView.adapter = adapter
lyricsRecyclerView.layoutManager = LinearLayoutManager(context)

// When lyrics load:
adapter.submitList(lyricsList)

// In your playback position update (every ~400ms):
adapter.updateActivePosition(player.currentPosition)
lyricsRecyclerView.smoothScrollToPosition(adapter.getActivePosition())
```

### 4. Add Settings
Paste the two `SwitchPreferenceCompat` blocks into your existing Preference XML.

### 5. Build
```bash
./gradlew :app:assembleDebug
```

## How It Works

### Notification Lyrics
- A `Handler` runs every 400ms while music is playing
- It finds the current lyric line by timestamp
- Updates the `RemoteViews` in the notification
- Notification refreshes automatically via Media3

### Dark Cyan Highlight
- `LyricsPreferences` reads the toggle state
- `LyricsAdapter` checks it in `onBindViewHolder`
- Active line gets `0x33008B8B` (Dark Cyan ~20% opacity) background
- White bold text for contrast

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Notification doesn't show lyrics | Ensure `notification_player_big.xml` is correct and `setCustomBigContentView` is called |
| Lyrics not syncing | Check that `LyricsEntry.time` is in **milliseconds**, not seconds |
| Highlight not appearing | Verify `LyricsPreferences.isDarkCyanHighlightEnabled()` returns true |
| Build error on `DefaultMediaNotificationProvider` | Make sure you have `androidx.media3:media3-session` dependency |
