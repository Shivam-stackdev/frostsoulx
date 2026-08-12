#!/usr/bin/env bash
set -euo pipefail

old_package='moe.rukamori.archivetune'
new_package='dev.vxs.frostsoulx'

apply_refactor() {
    local source_root destination_root file

    # Replace fully-qualified package references in tracked text files, including
    # Kotlin package/import declarations, Android manifests, Gradle settings, and docs.
    while IFS= read -r -d '' file; do
        perl -0pi -e 's/moe\.rukamori\.archivetune/dev.vxs.frostsoulx/g' "$file"
    done < <(git -c color.grep=false grep -lzF "$old_package" || true)

    # The application label is a user-visible brand and is kept consistent in
    # every resource locale. Resource keys remain unchanged to avoid API churn.
    while IFS= read -r -d '' file; do
        perl -0pi -e 's/ArchiveTune/FrostSoulX/g' "$file"
    done < <(find app/src -path '*/res/*' -type f -name '*.xml' -print0)

    perl -0pi -e 's/rootProject\.name\s*=\s*"ArchiveTune"/rootProject.name = "FrostSoulX"/g' settings.gradle.kts
    perl -0pi -e 's/^# ArchiveTune\b/# FrostSoulX/mg' README.md README_JA.md 2>/dev/null || true

    # Move every Kotlin source root so paths match package declarations. Build
    # variants and all app modules are handled without touching generated files.
    while IFS= read -r -d '' source_root; do
        destination_root="${source_root%/moe/rukamori/archivetune}/dev/vxs/frostsoulx"
        if [[ -e "$destination_root" ]]; then
            echo "Destination already exists: $destination_root" >&2
            exit 1
        fi
        mkdir -p "$(dirname "$destination_root")"
        mv "$source_root" "$destination_root"
        rmdir "$(dirname "$source_root")" 2>/dev/null || true
        rmdir "$(dirname "$(dirname "$source_root")")" 2>/dev/null || true
        rmdir "$(dirname "$(dirname "$(dirname "$source_root")")")" 2>/dev/null || true
    done < <(find . -type d -path '*/moe/rukamori/archivetune' -print0 | sort -z)

    if git -c color.grep=false grep -nF "$old_package" -- ':!*.patch' ':!*.diff' >/dev/null; then
        echo "Unreplaced package references remain" >&2
        git -c color.grep=false grep -nF "$old_package" -- ':!*.patch' ':!*.diff' >&2
        exit 1
    fi
}

branches=(
    main
    feature/qqmusic-karaoke
    feature/qqmusic-karaoke-runtime-repair
)

for branch in "${branches[@]}"; do
    git switch --force-create "$branch" "origin/$branch"
    apply_refactor
    git add -A
    if ! git diff --cached --quiet; then
        git commit -m "refactor: rename app package to dev.vxs.frostsoulx"
    fi
    git push origin "$branch"
done

# The requested 2.0 branch starts from the repaired QQ Music-inspired karaoke implementation.
git switch --force-create frostsoulx-2.0 origin/feature/qqmusic-karaoke-runtime-repair
git push --set-upstream origin frostsoulx-2.0
