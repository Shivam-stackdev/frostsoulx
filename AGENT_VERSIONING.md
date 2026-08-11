# FrostSoul Agent Versioning Guide

This file is the shared handoff point for every human or AI agent working on the FrostSoul Android repository. Read it before changing the app version, preparing a release build, or creating a new build-related commit.

## Current Version Baseline

The Android module currently uses the following version values in `app/build.gradle.kts`:

| Field | Current value | Meaning |
|---|---:|---|
| `versionName` | `14.0.0` | User-visible application version |
| `versionCode` | `139` | Google Play/Android monotonically increasing build number |
| Next planned `versionName` | `14.0.1` | Increase by `0.01` from the previous release/build version |
| Next planned `versionCode` | `140` | Increase by exactly `1` |

The project uses a three-part Android version string. Therefore, the requested `14.00 + 0.01` rule is represented canonically as `14.0.0 -> 14.0.1`. Do not write `14.01` unless the release owner explicitly requests a two-part display format.

## Rule for Every New Build

For each new distributable build, increase both values together:

```text
versionName: 14.0.0 -> 14.0.1
versionCode: 139     -> 140
```

The next build after that is:

```text
versionName: 14.0.1 -> 14.0.2
versionCode: 140     -> 141
```

Never reuse a `versionCode`. Android and Google Play require it to increase monotonically. If two agents prepare builds at the same time, the agent merging later must re-read this file and `app/build.gradle.kts`, then choose the next unused `versionCode`.

## Required Change Procedure

1. Read this file and inspect `app/build.gradle.kts` before editing.
2. Change only `versionCode` and `versionName` when the task is only a version bump.
3. Update the **Version History** table below in the same commit.
4. Use a clear commit message such as `build: bump version to 14.0.1 (140)`.
5. Do not silently change the version as part of an unrelated feature commit.
6. Before pushing, confirm that the target branch is correct and that `main` is not being modified unintentionally.
7. For release builds, verify that the release signing configuration and required GitHub Actions secrets are available. Never commit keystores, passwords, bearer tokens, or signing keys.

## Multiple-Agent Coordination

The first agent that plans a version bump should record the intended next version in the task or pull request description. A later agent must treat the checked-in `app/build.gradle.kts` value as authoritative, not an old message or local screenshot.

Only one version bump should be included per release/build commit. Feature agents should avoid changing version fields unless the task explicitly requests a build bump. If a feature branch contains several commits, the release agent should perform the version bump once near release preparation rather than incrementing for every internal commit.

## Version History

| Version name | Version code | Purpose or note | Commit/branch | Date |
|---|---:|---|---|---|
| `14.0.0` | `139` | Current repository baseline before the next planned build bump | `feature/qqmusic-karaoke` | 2026-08-11 |
| `14.0.1` | `140` | Reserved next build; apply only when preparing that build | — | — |

## Release Checklist

Before calling a build complete, confirm the following:

- `versionName` follows the previous version by `0.01`.
- `versionCode` is exactly one greater than the previous value and is unused on the target release channel.
- The branch and commit are recorded in the Version History table.
- The release APK is built from the intended release variant, not an unshrunk debug variant.
- Signing secrets are configured in GitHub Actions using the repository’s documented secret names.
- No secret, keystore, generated APK, or private signing material is committed.
- The build result and artifact link are recorded in the pull request or release notes.

## Important Current Handoff Note

A release-signing workflow correction was prepared locally to map the signing action’s documented `ANDROID_*` secret names while accepting the repository’s older aliases. It was intentionally not pushed after the release-signing investigation was paused. Do not assume that local workflow correction exists on the remote branch; inspect the branch and workflow before attempting the next release build.

Keep this file concise and update it whenever the version baseline or release convention changes.
