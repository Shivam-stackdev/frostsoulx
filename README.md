# FrostSoulX

**FrostSoulX** is an independently rebuilt Android music player focused on a polished, music-first listening experience. The project combines a modern Jetpack Compose interface with Media3 playback, synchronized lyrics, local persistence, artwork-driven player surfaces, and background media controls.

> FrostSoulX is an independent project. It is not an official Google or YouTube application and does not reproduce proprietary application source code or proprietary application assets.

[![Branch](https://img.shields.io/badge/default%20branch-frostsoul--reboot-111827?style=flat-square)](https://github.com/sakuraDev31/frostsoulx/tree/frostsoul-reboot)
[![Language](https://img.shields.io/badge/language-Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Playback](https://img.shields.io/badge/playback-Media3-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com/media/media3)
[![License](https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square)](LICENSE)

## Project at a glance

| Item | Details |
| --- | --- |
| Application ID | `dev.vxs.frostsoulx` |
| Current version | `14.0.1` (`versionCode 140`) |
| Primary branch | [`frostsoul-reboot`](https://github.com/sakuraDev31/frostsoulx/tree/frostsoul-reboot) |
| Platform | Android |
| Language | Kotlin |
| UI toolkit | Jetpack Compose and Material components used by the existing project |
| Playback engine | AndroidX Media3 / ExoPlayer |
| Persistence | Room and DataStore |
| Image loading | Coil 3 |
| Dependency injection | Hilt |
| Main verified CI task | `assembleGmsMobileArm64Debug` |
| License | GNU General Public License v3.0 |

## Why FrostSoulX exists

The project is a clean, independently implemented Android player designed around three priorities: a focused music-first interface, reliable playback and queue behavior, and a premium full-player experience. The default navigation is organized around **Home**, **Search**, and **Library**, while the player provides dedicated Vinyl, Artwork Blur, lyrics, and recommendations surfaces.

The repository is also structured to make the player easy to inspect and extend. Playback state is kept in the Media3 service layer, application data is persisted through repositories and Room/DataStore, and Compose screens consume state through view models and state adapters rather than directly owning the playback engine.

## Current feature set

### Listening and playback

FrostSoulX supports online music discovery, queue-based playback, background playback, local media handling, previous/next controls, seeking, playback speed controls, repeat behavior, audio-output selection, download actions, and a sleep timer. The Media3 `MediaSession` integration also exposes track metadata and artwork to Android system media controls.

### FrostSoulX player experience

The full player contains three horizontally navigable surfaces: the main player, recommendations, and lyrics. The two visual player styles are:

| Player surface | Purpose |
| --- | --- |
| Vinyl player | A turntable-inspired player with artwork, rotating vinyl presentation, metadata, one-line karaoke preview, and queue/output controls. |
| Artwork Blur player | A full-bleed artwork player with a seamless blurred backdrop, readable lyric contrast, metadata, recommendations, and synchronized lyric preview. |
| Lyrics page | A dedicated word-by-word karaoke surface with white fill/glow, active-line scrolling, translation/refetch actions, and per-track timing offset controls. |

Artwork rendering is intentionally separated from the notification pipeline. The in-app player uses artwork palettes and blurred layers for visual continuity, while Media3 receives notification-safe artwork metadata and a custom bitmap loader for Android media controls.

### Lyrics and synchronization

The lyrics subsystem supports timed lines, word-level karaoke where provider timing is available, line-level fallback timing, translation/refetch flows, and a persisted per-track offset. Positive offset values advance lyric selection relative to playback; negative values delay it. The offset is applied by the shared synchronization engine rather than by a second UI clock.

Lyrics fetching includes track identity safeguards intended to reduce wrong-song matches. Cache identity includes the track metadata used by the repository, and provider results are checked before being accepted when sufficient title or artist information is available.

### Discovery and library

Home and search surfaces expose music discovery, artist and album navigation, recent listening content, queue actions, and recommendation cards. Artist entries shown in the player’s Artists Involved dialog can open the corresponding artist profile when a provider identity is available. Library screens cover saved songs, albums, playlists, mixes, and local content.

### Appearance and interaction

The app includes a focused light/dark visual system, artwork blur intensity controls, player background options, lyric animation controls, gesture-aware player navigation, configurable refresh-rate behavior where supported, and responsive layouts for different Android screen sizes. AOD customization is no longer exposed as a separate settings page; the AOD player uses a stable artwork-backed reference layout.

## Architecture and repository map

FrostSoulX uses a feature-oriented Kotlin structure with a service-backed playback core.

| Area | Location | Responsibility |
| --- | --- | --- |
| Android application | `app/src/main/` | Manifest, resources, application wiring, and Android components. |
| Playback service | `app/src/main/kotlin/dev/vxs/frostsoulx/playback/` | Media3 player, MediaSession, notifications, queue persistence, downloads, and system integration. |
| Player UI | `app/src/main/kotlin/dev/vxs/frostsoulx/ui/player/` | Mini player, full player, Vinyl/Artwork Blur surfaces, lyrics preview, AOD, and controls. |
| Lyrics | `app/src/main/kotlin/dev/vxs/frostsoulx/lyrics/` | Provider orchestration, parsing, repository caching, synchronization, offsets, and lyric models. |
| Compose screens | `app/src/main/kotlin/dev/vxs/frostsoulx/ui/screens/` | Home, search, library, artist, album, settings, playlist, and detail screens. |
| View models | `app/src/main/kotlin/dev/vxs/frostsoulx/viewmodels/` | UI state, search, lyrics menus, queue flows, and feature-level actions. |
| Database | `app/src/main/kotlin/dev/vxs/frostsoulx/db/` | Room entities, DAOs, migrations, and persistent music data. |
| Settings/constants | `app/src/main/kotlin/dev/vxs/frostsoulx/constants/` | Preference keys, feature flags, player styles, and shared constants. |
| Image and palette utilities | `app/src/main/kotlin/dev/vxs/frostsoulx/ui/utils/` and `ui/theme/` | Artwork URLs, palette extraction, blur/background helpers, and Compose utilities. |
| Gradle configuration | `build.gradle.kts`, `app/build.gradle.kts`, `gradle/` | Plugins, dependencies, version catalog, variants, and build configuration. |
| CI workflows | `.github/workflows/` | Debug, release, and signing-key workflow definitions. |

## Getting started

### Requirements

Use Android Studio with a recent Android SDK, JDK 21, and the repository’s Gradle Wrapper. A physical Android device or emulator is recommended for validating Media3 notifications, audio output selection, background playback, artwork loading, gestures, and AOD behavior.

### Clone the repository

```bash
git clone --branch frostsoul-reboot https://github.com/sakuraDev31/frostsoulx.git
cd frostsoulx
```

### Open the project

Open the repository root in Android Studio and allow Gradle synchronization to complete. The project uses Kotlin DSL and the version catalog in `gradle/libs.versions.toml`; do not replace the Gradle Wrapper with a system Gradle installation.

### Build the verified debug variant

```bash
./gradlew assembleGmsMobileArm64Debug --no-daemon --build-cache
```

The generated debug APK is normally located under an `app/build/outputs/apk/` variant directory. CI uses the same `assembleGmsMobileArm64Debug` task for the arm64 debug build.

### Install a local debug APK

After a successful build, locate the generated APK and install it with Android Debug Bridge:

```bash
adb devices
adb install -r path/to/app-gms-mobile-arm64-debug.apk
```

The exact APK filename can vary with the Android Gradle Plugin output layout. If the path differs, search the output directory rather than changing the build task:

```bash
find app/build/outputs/apk -type f -name '*.apk' -print
```

### Useful development commands

| Command | Use |
| --- | --- |
| `./gradlew assembleGmsMobileArm64Debug --no-daemon --build-cache` | Build the CI-equivalent debug APK. |
| `./gradlew lintDebug` | Run Android lint for the debug configuration when the task is available. |
| `./gradlew testDebugUnitTest` | Run debug unit tests when the project contains applicable tests. |
| `./gradlew clean` | Clear generated build output when diagnosing stale artifacts. |
| `git diff --check` | Detect whitespace errors before committing. |

## Configuration and secrets

Do not commit API keys, signing keys, keystores, or personal `local.properties` files. Build-time integrations are supplied through the local environment or GitHub Actions secrets as appropriate. A normal local debug build should not require committing credentials to the repository.

Release builds require correctly configured signing inputs in the release environment. Treat generated keystores and passwords as sensitive material and rotate them if they are ever exposed.

## CI and branches

The repository’s active development branch is [`frostsoul-reboot`](https://github.com/sakuraDev31/frostsoulx/tree/frostsoul-reboot). Pushes and pull requests can trigger the Debug workflow, which builds the arm64 debug variant and uploads the resulting APK as a workflow artifact.

The release workflow is intended for signed distribution builds and depends on repository secrets. Workflow files may be adjusted independently from source changes when GitHub Actions permissions or artifact-upload behavior require repository-owner intervention.

## Screenshots

The README intentionally does not link to screenshots from other projects. This keeps the documentation accurate for FrostSoulX and avoids presenting unrelated reference images as the current application. Device captures can be added later under `docs/screenshots/` with descriptive names such as `home.png`, `vinyl-player.png`, `artwork-blur-player.png`, and `lyrics.png`.

## Contributing

Before opening a pull request, describe the user-visible behavior being changed, identify the affected feature area, and verify that normal playback and navigation still work. For player changes, test both Vinyl and Artwork Blur modes, the mini player, queue/output controls, lyrics synchronization, notification metadata, and rotation or process recreation where possible.

Keep changes focused. Do not modify workflow files as part of a source-only fix unless the workflow change is explicitly required. Run `git diff --check`, review the staged diff, and include the relevant Gradle task and result in the pull request description.

Read [`CONTRIBUTING.md`](CONTRIBUTING.md) for the project’s existing engineering guidance.

## Troubleshooting

| Symptom | Checks |
| --- | --- |
| Artwork is missing in the Android media notification | Confirm that the current `MediaItem` contains `MediaMetadata.artworkUri`, that the Media3 bitmap loader is registered, and that the notification provider preserves the loaded large icon. |
| Lyrics appear early or late | Use the Lyrics page offset control. Positive values advance lyric selection; negative values delay it. Verify that the track identity and fetched lyrics match before changing the offset. |
| The player build fails at Kotlin compilation | Read the first `e: file:///...` compiler line in CI, fix the source error, and rerun `assembleGmsMobileArm64Debug`. Do not rely only on the final Gradle summary. |
| A release build fails during signing | Verify the keystore, alias, passwords, and CI secrets in the release environment. Never place signing credentials in committed source. |
| An APK upload step fails | Check the workflow’s resolved APK path and artifact name. The build output directory can differ between Android Gradle Plugin versions. |
| Artwork Blur text is hard to read | Confirm that the effective blur intensity and dark contrast scrim are enabled; the player is designed to keep lyric text readable over bright artwork. |

## Credits and acknowledgments

FrostSoulX builds on ideas, libraries, and engineering work from the open-source Android music community. Credit is given to the following projects and maintainers:

| Project | Contribution or relationship | Link |
| --- | --- | --- |
| **ArchiveTune** | Upstream Android music-player foundation and project context retained in applicable source notices. | [github.com/rukamori/ArchiveTune](https://github.com/rukamori/ArchiveTune) |
| **InnerTube** | YouTube/YouTube Music data-model and client integration reference used by the project’s browsing and playback layers. | [InnerTube on GitHub](https://github.com/tombulled/innertube) |
| **Metrolist** | Open-source Android music-player project whose architecture and implementation patterns are acknowledged as project inspiration. | [github.com/mostafaalagamy/Metrolist](https://github.com/mostafaalagamy/Metrolist) |

FrostSoulX is independently maintained and is not an official release of any credited project. Their respective licenses, notices, names, and trademarks remain applicable to the portions governed by them.

## Legal and licensing

FrostSoulX is distributed under the GNU General Public License v3.0 as represented by [`LICENSE`](LICENSE). Review the license and the applicable notices in source files before redistributing modified builds.

FrostSoulX is not an official Google, YouTube, or YouTube Music application. Product names, logos, and service marks remain the property of their respective owners. Users are responsible for complying with the terms and laws applicable to the services they access.

## References

1. [Android Developers — Jetpack Compose](https://developer.android.com/compose)
2. [Android Developers — Media3](https://developer.android.com/media/media3)
3. [Android Developers — MediaSession and media controls](https://developer.android.com/media/media3/session)
4. [Kotlin documentation](https://kotlinlang.org/docs/home.html)
5. [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html)

## Repository navigation

- [Source tree](https://github.com/sakuraDev31/frostsoulx/tree/frostsoul-reboot)
- [Issues](https://github.com/sakuraDev31/frostsoulx/issues)
- [Pull requests](https://github.com/sakuraDev31/frostsoulx/pulls)
- [Debug workflow](https://github.com/sakuraDev31/frostsoulx/actions/workflows/Debug.yml)
- [Contributing guide](CONTRIBUTING.md)
- [License](LICENSE)
