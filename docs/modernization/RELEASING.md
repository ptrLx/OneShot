# Releasing, versioning and F-Droid

## How F-Droid ships OneShot

The F-Droid recipe [`metadata/de.ptrlx.oneshot.yml`](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/de.ptrlx.oneshot.yml) (as of v1.1.1):

```yaml
RepoType: git
Repo: https://github.com/ptrLx/OneShot.git
Binaries: https://github.com/ptrLx/OneShot/releases/download/v%v/OneShot.apk
Builds:
  - versionName: 1.1.1
    versionCode: 111
    commit: a3f3b358fe56ac73b25b7810595004d7a7ba2140
    subdir: app
    gradle:
      - yes
AllowedAPKSigningKeys: 511c8933704278f936183c54ef963b9b66a0f6c232adca17bb52090f62b80456
AutoUpdateMode: Version
UpdateCheckMode: Tags
```

What this means for us:

1. **Discovery:** `checkupdates` scans the git tags of ptrLx/OneShot, reads `versionCode` and `versionName` from `app/build.gradle(.kts)` at each tag with regular expressions, and picks the highest versionCode. Keep both as plain literals in the app build file (not in a version catalog, not computed). Only push `vX.Y.Z` tags upstream; any other tag is scanned too.
2. **Build:** F-Droid copies the previous build block, sets `commit` to the new tag and runs `gradle assembleRelease` in `app/` on its buildserver, today **Debian trixie with the default OpenJDK 21** (fdroidserver `buildserver/Dockerfile`, no `sudo:` override in our recipe). It uses the Gradle version of our wrapper and scans the source for proprietary dependencies and binaries.
3. **Reproducibility:** F-Droid downloads `OneShot.apk` from the GitHub release `v<versionName>`, copies its signature onto its own unsigned build (`apksigcopier`) and publishes our signed APK only if the result verifies. Any byte difference blocks the update. The signature must be from the key in `AllowedAPKSigningKeys`.
4. **Tag = built commit.** The tag must point at exactly the commit the release APK was built from. History shows why: tag `v1.1.1` points to `319fba1`, but the published APK and the F-Droid recipe are from `a3f3b35` (baseline profile sort fix added after tagging), so the recipe needed a hand-edited `commit:`. release-please tags the release commit and the release job builds that tag, which rules this out. `reproduce-release.yml` verified on 2026-10-04 that `a3f3b35` rebuilt in `debian:bullseye` with OpenJDK 11 matches the published v1.1.1 `OneShot.apk`.
5. **Changelog:** F-Droid shows `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (max 500 characters). Descriptions, icon and screenshots come from `fastlane/metadata/android/`.

## Versioning with release-please

- Every change lands on `master` as a squash commit whose message is the conventional-commit PR title (`feat:` gives a minor bump, `fix:` a patch bump, `feat!:`/`BREAKING CHANGE` a major bump; `build:`, `ci:`, `chore:`, `docs:`, `refactor:`, `test:` do not release and are hidden in the changelog).
- `release.yml` runs release-please on every push to `master`. It keeps one release PR open (`chore: release X.Y.Z`) that updates `CHANGELOG.md`, `.release-please-manifest.json` and `versionName` in `app/build.gradle` (line marked `// x-release-please-version`).
- The same workflow runs `scripts/release/sync_version.py` on the release PR branch: it sets `versionCode = MAJOR*10000 + MINOR*100 + PATCH` (1.2.0 is 10200, larger than the legacy 111) and writes the F-Droid changelog file for that versionCode. The script refuses to lower the versionCode.
- Configuration: `release-please-config.json` (`release-type: simple`, `include-component-in-tag: false`, so tags are `vX.Y.Z`).

## Builds

| Trigger | Output | Signing |
|---|---|---|
| Pull request | debug APK + unsigned release APK as workflow artifacts | debug key / none |
| Push to `master` (nightly) | same artifacts for every master commit | debug key / none |
| Release PR merged (upstream) | `OneShot.apk` attached to the GitHub release `vX.Y.Z` | upstream release key from repository secrets |

The release APK is built in the same Debian container and JDK as the CI reproducibility job (which mirrors the F-Droid buildserver), signed with `apksigner` without zipalign (F-Droid must be able to transplant the signature onto its own build), checked with `apksigcopier compare` and uploaded under the fixed asset name `OneShot.apk`. `reproduce-release.yml` can re-verify any published release from source.

## Secrets (upstream repository only)

| Secret | Purpose |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | base64 of the keystore holding the key behind `AllowedAPKSigningKeys` |
| `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD` | keystore access |
| `RELEASE_PLEASE_TOKEN` (optional) | fine-grained PAT (contents and pull requests: write) so CI runs on release PRs; PRs opened with the default token do not trigger workflows |

The fork has no release key. Release PRs in the fork are only used to validate the automation and are not merged.

## Rules that keep F-Droid green

- Toolchain changes go together: wrapper Gradle version, CI JDK, Debian image of the reproducibility and release jobs. They must match what the F-Droid buildserver uses (#26).
- With AGP 8 or newer: `dependenciesInfo { includeInApk = false; includeInBundle = false }` and `vcsInfo { include = false }`.
- No proprietary libraries (Google Play services, Firebase, Crashlytics), no prebuilt binaries in the repository except the validated Gradle wrapper jar, no toolchain auto-download.
- Kotlin Multiplatform: iOS targets must not be configured on Linux builds unless explicitly enabled (#44), so the F-Droid build never downloads Kotlin/Native.
- After the first release from the new pipeline, watch the F-Droid build log (monitor.f-droid.org) and, if the recipe needs changes, open a merge request on fdroiddata.

## Upstream handover

Tracked in #19: add the secrets, enable Actions and "Allow GitHub Actions to create pull requests", set squash-only merges with the PR title as commit message, merge the fork's `master`, let release-please open the first release PR, merge it, and verify the published `OneShot.apk` with `reproduce-release.yml` before F-Droid picks it up.
