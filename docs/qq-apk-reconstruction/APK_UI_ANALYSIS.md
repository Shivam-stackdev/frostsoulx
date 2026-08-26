# QQ Music APK UI Reconstruction Analysis

**Reference APK:** `qq-music-20-7-5-8.apk`  
**Package:** `com.tencent.qqmusic`  
**Version:** `20.7.5.8` (version code `7308`)  
**Reconstruction branch:** `qq-apk-ui-reconstruction`, based on `frostsoulx-reborn` at `d54a9d2132d73b3b50223aacdc3a42410e778261`

## Scope and method

The APK is treated only as an observable reference. The analysis uses decoded manifest/resources, asset filenames and dimensions, DEX string signals, and the supplied visual references. No proprietary source code is recovered or copied. The new app remains an independent Jetpack Compose implementation with its own models, callbacks and resource usage.

## Phase 1: APK technical analysis

| Area | Observation | Reconstruction implication |
|---|---|---|
| Package/platform | Package `com.tencent.qqmusic`; minimum SDK 23; target SDK 30; portrait-oriented launcher path. | Preserve a phone-first portrait baseline while making the Compose layouts responsive. |
| UI framework signals | DEX contains extensive Android View, Fragment, Navigation, Hippy and Kuikly signals, but no `androidx.compose` signal in the inspected DEX string set. | The reference is a hybrid View/Fragment application with Hippy/Kuikly-rendered feature surfaces, not a pure Compose application. Rebuild the observable UI independently in Compose rather than reproducing the runtime. |
| Launch flow | `AppStarterActivity` is the primary `MAIN`/`LAUNCHER` activity. `WelcomeActivity`, privacy/config activities and splash activities are separate entry stages. | FrostSoul should have a deterministic launch shell and lightweight first-run route, without copying the reference’s privacy/ads machinery. |
| Primary shell | Manifest and DEX signals expose main desk/bottom navigation concepts, a mini-bar activity family, player activities, and search/settings entrypoints. | Keep Home, search, library/profile, mini-player and full player in one coherent navigation shell. |
| Player surfaces | The APK includes `QuickListenPlayerActivity`, `PortMVPlayerActivity`, custom-player activity names, player button assets, lyrics assets and `bw_main_player` resources. | Use one full audio player plus focused lyrics/details/recommendation pages; leave video/MV features out of the audio reconstruction unless explicitly requested. |
| Feature containers | Hippy bundles include `CmtList`, `NewSong`, `NoticeList`, `MineWormHole`, `FreeModeDialog`, and common popup bundles. Kuikly assets include dialogs, player-related pages and style/quality pages. | Treat recommendations, comments and dialog/sheet surfaces as reusable feature containers, not as separate hard-coded screens. |
| Motion/rendering | The APK contains Lottie JSON, PAG assets, player loading animation assets and multiple light/dark share/action assets. | Recreate observable state transitions using Compose animation primitives; do not import or decompile proprietary animation logic. |

## Phase 2: Screen inventory

### Home / Main Desk

| Field | Inventory |
|---|---|
| Name | Main Desk / Home feed |
| Purpose | Entry surface for personalized discovery, promotional content, listening shortcuts and bottom navigation. |
| Navigation entry | Launcher shell after startup; bottom navigation returns here. |
| Background | Light/dark theme surface; reference asset and token set supports both dark and light surfaces. |
| Header | Compact search affordance, category/tab strip and utility entry. |
| Components | Promotional/banner carousel, “everyone is listening” or recommendation shelf, preference/personalization prompt, recent/playlist shelves, persistent mini-player and bottom navigation. |
| Typography | High-contrast section titles, muted metadata, compact tab labels and emphasized selected state. |
| Spacing | Verified named reference tokens include `margin_l=16dip`, `margin_m=8dip`; use 16dp page gutters and 8dp micro gaps as baseline. |
| Corner radius | Rounded cards and pills; use 16–20dp cards and pill-shaped selected tabs in the independent implementation. |
| Shadows | Restrained elevation; artwork and active controls carry more emphasis than container shadows. |
| Icons/images | Search, tab, play, folder/album, more, favorite and navigation icons; home-page album/default/decorative assets are present in the APK. |
| Interactive states | Selected tab, pressed card, play/pause, favorite, queue, add-to-queue and preference CTA. |
| Animation | Horizontal carousel motion, pressed feedback, mini-player appearance/peek and bottom-tab transitions. |
| Scroll behavior | Vertically scrolling feed with horizontally scrolling shelves/carousels. |

### Full audio player

| Field | Inventory |
|---|---|
| Name | Main Player / Quick Listen Player |
| Purpose | Playback control, artwork focus, metadata, queue entry and secondary player pages. |
| Navigation entry | Tap mini-player, select a song, or open a quick-listen/player activity. |
| Background | Opaque theme/dynamic artwork surface. Reference visual shows a dark matte vinyl/plinth region and a lower artwork-derived glow rather than a transparent card. |
| Header | Collapse/back control, centered page indicator, style/theme and share utilities. |
| Components | Large square album/vinyl visual, track title/artist, metadata chips, action rail, seekbar, elapsed/total time, repeat/previous/play-pause/next/queue controls. |
| Typography | Large title, lighter artist line, compact chip labels and small utility labels. |
| Spacing | Reference asset dimensions include 256x256 play/pause controls and a 40x41 white-arrow asset; use responsive artwork width around 86–90% of the content width and large central transport control. |
| Corner radius | Large rounded plinth/artwork container; small pill chips and circular controls. |
| Shadows | Matte black plinth with controlled edge contrast; no translucent Material surface behind the main artwork. |
| Icons/images | `bw_main_player` play/pause/loading assets, white arrow, back/share assets, album/default-artwork assets and artwork-derived palette. |
| Interactive states | Playing/paused/loading, liked/unliked, repeat mode, seek drag, previous/next availability, queue open, lyrics open and page swipe. |
| Animation | Rotating vinyl while playing, smooth page indicator movement, seek progress, mini-player expand/collapse and control press transitions. |
| Scroll behavior | Player pages are horizontally paged; details/recommendations scroll vertically. |

### Lyrics

| Field | Inventory |
|---|---|
| Name | Synchronized Lyrics / Lyric UI |
| Purpose | Line-by-line and word-level playback-following lyrics, with seek-on-line-tap. |
| Navigation entry | Player action rail or horizontal player pager. |
| Background | Same player artwork-derived visual language; active glow is subdued and lower-weight than the lyric text. |
| Header | Same collapse/pager/style/share family as the full player. |
| Components | Current line, nearby lines, optional translation/romanization, bottom utility rail, play/pause and line-seek behavior. |
| Typography | Large active line, dim inactive lines, high contrast and generous vertical rhythm. |
| Spacing | Use 28dp horizontal inset for lyric text, 12–16dp line gaps and a bottom action rail above navigation/system insets. |
| Interactive states | Current line, past/future line, loading, plain lyrics, unavailable lyrics and tapped-to-seek. |
| Animation | Current-line emphasis, karaoke word fill, automatic scroll-to-current-line and smooth pager transitions. |
| Scroll behavior | Lazy vertical list auto-scrolls to current line unless the user is actively scrolling. |

### Details / recommendations

| Field | Inventory |
|---|---|
| Name | Song Details / Related Recommendations |
| Purpose | Expose metadata, artist/context chips and related playlist/video discovery. |
| Navigation entry | Player pager or metadata/detail action. |
| Background | Same player/dynamic theme surface. |
| Header | Player pager header with collapse and utilities. |
| Components | Title/artist row, artist avatar, quality/listening chips, playlist recommendation grid and optional video shelf. |
| Typography | Medium title, muted artist/metadata and compact card labels. |
| Spacing | Responsive two- or three-column grid; 10–12dp gutters; medium cards rather than tall list rows. |
| Interactive states | Play card, play all, refresh recommendations, queue/add and open video/detail. |
| Animation | Card press, pager transition and loading/refresh state. |
| Scroll behavior | Vertical detail page with a lazy grid/shelves. |

### Mini-player and bottom navigation

| Field | Inventory |
|---|---|
| Name | Persistent Mini-player / Main bottom navigation |
| Purpose | Provide playback continuity and fast access to Home/library/search/player. |
| Navigation entry | Persistent shell element while a track is loaded. |
| Background | Opaque light or dark theme surface; reference dimensions explicitly include `main_desk_bottom_navigate_height=58dip` and `main_desk_bottom_navigate_cool_height=70dip`. |
| Components | Artwork thumbnail, title/artist, play/pause, queue, optional progress strip, five bottom destinations. |
| Interactive states | Tap to expand, play/pause, queue, swipe next/previous, peek, dismiss when stopped and selected bottom tab. |
| Animation | Slide/fade in, spring peek/expand, artwork update and progress movement. |
| Scroll behavior | Fixed above bottom navigation; feed content receives bottom padding. |

### Settings / onboarding / dialogs

The reference exposes a broad settings and onboarding ecosystem, including startup privacy/config stages, settings, quality/download settings, custom-skin/player settings, share sheets, login dialogs, free-mode dialogs and notification-related overlays. FrostSoul’s independent reconstruction should keep only supported, functional settings and use custom sheets/dialogs that share the same design tokens. It should not replicate ads, payment, proprietary login, telemetry or device-specific integrations.

## Phase 3: Component inventory

| Component family | Independent FrostSoul implementation target |
|---|---|
| Theme tokens | `FrostSoulTheme`, `FrostSoulColors`, `FrostSoulTypography`, `FrostSoulShapes`, `FrostSoulSpacing` and motion tokens. |
| Text/icon primitives | `FSText` and `FSIcon` provide Material-independent rendering for primary surfaces. |
| Buttons | `FSButton`, `FSIconButton`, play/pause and transport controls with selected/pressed/disabled states. |
| Cards | `FSGlassCard`, `FSAlbumCard`, `FSArtistCard`, medium recommendation cards and preference cards. |
| Lists | `FSListItem`, queue rows, “everyone listening” rows and recent-play rows. |
| Navigation | Custom top pager/header, bottom navigation, mini-player shell and player-page pager. |
| Media visuals | `FSAlbumArt`, vinyl/plinth renderer, artwork palette extraction and lower-region glow. |
| Lyrics | `FSLyrics`, karaoke line renderer, current-line auto-scroll, translation/romanization fallback and line seek. |
| Sheets/dialogs | Custom FrostSoul bottom sheet, player options sheet, queue sheet and onboarding permission rows. |
| State/behavior | Existing player connection, Media3 playback, queue callbacks, lyric synchronization engine and navigation callbacks are reused as contracts, not copied from the reference APK. |

## Phase 4: Navigation map

```text
App launch
  -> onboarding/privacy/config gate (FrostSoul simplified first run)
  -> Main shell
       -> Home feed
       -> Search
       -> Library / Profile
       -> persistent Mini-player
            -> Full Player pager
                 -> Album / Player page
                 -> Synchronized Lyrics page
                 -> Details / Recommendations page
                 -> Queue sheet
                 -> Player options sheet
       -> Settings
```

The navigation map is intentionally smaller than the APK’s full feature graph. It covers the observable core experience requested for FrostSoul and avoids reproducing proprietary service integrations. The reference’s interactive Community Hub/social layer is intentionally excluded from the reconstruction: the player remains focused on playback, lyrics, queue and music recommendations, and first-run onboarding does not expose community actions or URI launches.

## Phase 5: Design-token map

| Token | APK/reference signal | FrostSoul baseline |
|---|---|---:|
| Phone reference canvas | APK contains portrait assets and reference visual is a phone layout. | Responsive Compose; optimize around 390dp logical width. |
| Page horizontal inset | `margin_l=16dip` is explicitly named in decoded resources. | 20dp for FrostSoul Home/player shell; 16dp for dense lists. |
| Micro gap | `margin_m=8dip` is explicitly named. | 4dp micro, 8dp small, 12dp medium. |
| Bottom navigation | `main_desk_bottom_navigate_height=58dip`; cool variant `70dip`. | 58dp normal, 70dp expanded/paired state. |
| Player main control asset | Play/pause PNGs are 256x256. | Responsive 64–84dp on phone; scale asset geometry independently. |
| Player white utility arrow | White arrow asset is 40x41. | 20–24dp visible icon box. |
| Home album fallback | `home_page_album_default.png` is 224x224. | Square artwork fallback with 1:1 ratio. |
| Lyric active color | `lyric_hilight_text_color=#2dc26c`; `lyric_interim_text_color=#b3ffffff`. | Theme-controlled active lyric; green only where appropriate, with white/dark contrast states. |
| Lyric page backgrounds | `lyric_multi_style_page_bg_dark_color=#1c1f1e`; light `#ecf0f2`. | Dark `#0c0c0c` and light `#f3f5f8` theme surfaces. |
| Accent | `lyric_multi_style_use_button_color=#00eb81`; other green accents exist. | Restrained `#00E676` in dark Home; neutral dark accent in light theme; player glow derived from artwork. |
| Typography | APK bundles Manrope, Albert Sans, Anton and other font assets. | System/available Compose fonts first; use bundled-safe equivalents only if present in the app. |

## Implementation plan

The independent reconstruction uses the existing FrostSoul playback contracts and replaces visual surfaces through reusable Compose primitives. First, establish the theme and shell tokens. Next, align Home shelves, bottom navigation and mini-player. Then align the full player’s pager/header, vinyl/plinth, lower glow, action rail, seekbar and transport. Finally, align lyrics and recommendation/details pages, including loading and unavailable states. All playback, queue and lyrics timing callbacks remain routed through the existing player connection and synchronization engine.

## Visual-comparison protocol

Because the sandbox has no Android emulator and the APK does not expose a runnable reference device session here, automated screenshot pixel-diff against the APK cannot be executed in this environment. The implementation therefore uses decoded dimensions/assets, APK resource naming, supplied visual references and CI compilation as evidence. The branch should receive device screenshots for Home, full player, lyrics, recommendations, mini-player and dark/light modes for the next pixel-level comparison pass. Differences should be corrected in this order: overall canvas/background, major artwork/plinth size, bottom navigation/mini-player placement, typography scale, card gutters/radii, icon sizes and finally motion timing.
