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
- `release.yml` runs release-please on every push to `master`. It keeps one release PR open (`chore: release X.Y.Z`) that updates `CHANGELOG.md`, `.release-please-manifest.json` and `versionName` in `app/build.gradle.kts` (line marked `// x-release-please-version`).
- The same workflow runs `scripts/release/sync_version.py` on the release PR branch: it sets `versionCode = MAJOR*10000 + MINOR*100 + PATCH` (1.2.0 is 10200, larger than the legacy 111) and writes the F-Droid changelog file for that versionCode. The script refuses to lower the versionCode.
- Configuration: `release-please-config.json` (`release-type: simple`, `include-component-in-tag: false`, so tags are `vX.Y.Z`).

## Builds

| Trigger | Output | Signing |
|---|---|---|
| Pull request | debug APK + unsigned release APK as workflow artifacts | debug key / none |
| Push to `master` (nightly) | same artifacts for every master commit | debug key / none |
| Release PR merged (upstream) | `OneShot.apk` attached to the GitHub release `vX.Y.Z` | upstream release key from the `release` environment |
| Manual run of `sign-release.yml` (rehearsal) | signed `OneShot.apk` as a workflow artifact, nothing published | upstream release key from the `release` environment |

The release APK is built by the reusable workflow `sign-release.yml` in two jobs. The **build** job uses the same Debian container and JDK as the CI reproducibility job (which mirrors the F-Droid buildserver), checks versionCode against the tag and runs the APK checks; it has no access to the key. The **sign** job runs in the protected `release` environment, never checks out or runs repository code, signs the unsigned artifact with `apksigner` without zipalign (F-Droid must be able to transplant the signature onto its own build), fails unless the APK has exactly one signer whose certificate SHA-256 is the `AllowedAPKSigningKeys` value, and checks the transplant with `apksigcopier compare`. `release.yml` then uploads the APK under the fixed asset name `OneShot.apk`. `reproduce-release.yml` can re-verify any published release from source; it picks the environment F-Droid used for that release (`debian:bullseye` with OpenJDK 11 up to v1.1.1, `debian:trixie` with OpenJDK 21 afterwards), the `image` and `jdk` inputs override it.

## Toolchain

| Component | Version | Why |
|---|---|---|
| JDK | 21 (Temurin on the runner, Debian OpenJDK in the containers) | default JDK of the F-Droid buildserver (Debian trixie) |
| Gradle wrapper | 9.4.1, `distributionSha256Sum` pinned | AGP 8 uses an internal API that Gradle 9.6 removed, so the wrapper stays at 9.5 or below; F-Droid runs exactly the wrapper version and checks it against its Gradle transparency log |
| Android Gradle plugin | 8.13.2 | last AGP 8 release. AGP 9 (built-in Kotlin, new DSL) needs Gradle 9.5.0 or newer and compileSdk 37, and unlocks Hilt 2.59+, Kotlin 2.4 and Compose 1.12: separate step, see #79 |
| Kotlin / KSP | 2.3.21 / 2.3.12, Compose compiler Gradle plugin, strong skipping off | Hilt 2.58 reads Kotlin metadata up to 2.3. Strong skipping stays off while composables read plain ViewModel properties; re-enable it together with UI tests |
| Room / Hilt | 2.8.5 / 2.58 | both run as KSP processors since the kapt removal, with a byte-identical exported schema; Hilt 2.58 is the last release that works with AGP 8 |
| Compose BOM | 2026.06.01 (Compose 1.11) | newest BOM whose `aar-metadata.properties` still asks for `minCompileSdk=35` and `minAndroidGradlePluginVersion=8.6.0`; Compose 1.12 requires compileSdk 37 and AGP 9.1 |
| compileSdk | 36 (targetSdk 36, minSdk 29) | Android 16, reviewed for behavior changes with edge-to-edge, predictive back and the splash screen; minSdk 29 keeps Android 10 supported |

Baseline profiles: AGP packs `assets/dexopt/baseline.prof` and `baseline.profm` from the profiles shipped by libraries. Before AGP 8.1 the `.profm` listed the dex files in hash map order ([issuetracker 231837768](https://issuetracker.google.com/issues/231837768)), which v1.1.1 worked around with `app/fix-profm.gradle`. Since AGP 8.1 the profile generator sorts the dex files itself ([tools/base 2f2c6b3](https://android.googlesource.com/platform/tools/base/+/2f2c6b30b55e18e2672edf5ee8e8e583be759d3e)), so the script was removed with the AGP 8 upgrade. The CI reproducibility job guards this; if the profiles ever differ again, F-Droid's documented fallback is to disable the `ArtProfile` tasks.

## Signing setup (`release` environment)

The release key is stored only as secrets of a GitHub **environment** named `release` (Settings, Environments), never as repository secrets, so no other workflow or job can read it.

| Name | Kind | Purpose |
|---|---|---|
| `RELEASE_KEYSTORE_BASE64` | environment secret | `base64 -w0` of the keystore (JKS or PKCS12) holding the key behind `AllowedAPKSigningKeys` |
| `RELEASE_KEYSTORE_PASSWORD` | environment secret | keystore password |
| `RELEASE_KEY_ALIAS` | environment secret | alias of the signing key |
| `RELEASE_KEY_PASSWORD` | environment secret | key password (same as the keystore password for PKCS12) |
| `RELEASE_CERT_SHA256` | environment variable, optional | expected signer certificate SHA-256; only needed for a test key, defaults to `511c8933…0456` |
| `RELEASE_PLEASE_TOKEN` | repository secret, optional | fine-grained PAT (contents and pull requests: write) so CI runs on release PRs; PRs opened with the default token do not trigger workflows |

Protection rules for the `release` environment:

- **Required reviewers:** the maintainer. Every signing run waits for approval, so nothing gets signed unnoticed. Approve promptly after merging a release PR: F-Droid may try to build as soon as it sees the tag, and fails while `OneShot.apk` is missing.
- **Deployment branches and tags:** selected branches `master` and `release-please--branches--master*`. Releases run from `master` (`release.yml` builds the tag that release-please just created) and the release gate runs on the release PR branch; no other branch can reach the key.
- No wait timer, admins not allowed to bypass.

## Release gate (required check)

`release-gate.yml` provides the job **Release gate**, a required status check (branch ruleset on `master`). On ordinary PRs it passes immediately. For a release PR it runs on every push to the release-please branch, or manually with `make release-gate`, because environments do not deploy from the `refs/pull/N/merge` ref of pull_request runs; the check is attached to the branch head commit and therefore counts for the PR (the pull_request run of a release PR uses a different job name, so it cannot satisfy the check). It rehearses the release exactly:

1. `sync_version.py --check`: versionCode and the fastlane changelog match versionName.
2. `sign-release.yml` builds the PR head like F-Droid and signs it in the `release` environment (the maintainer approves the deployment, which is the explicit go for the release).
3. The signed APK must have exactly the certificate configured in the environment, `apksigcopier compare` must succeed, and **F-Droid must accept the key**: the certificate SHA-256 must be listed in `AllowedAPKSigningKeys` of the live F-Droid recipe (fdroiddata).

Only then the gate passes and the release PR can be merged. For every other PR the gate passes immediately. Consequences:

The gate rehearses signing, nothing else. The slow suites do not run on pull requests, so before merging a release PR check that the `CI`, `Emulator` and `Upgrade E2E` runs **on the release-please branch** are green: those pushes trigger all three, including the reproducible Debian build and the upgrade test on API 29 and 36.

- **Another key** (for example a test key in the `release` environment): it is not in `AllowedAPKSigningKeys`, so the gate fails and release PRs stay blocked, as intended.
- **The original key:** the gate passes and the release PR can be merged; `release.yml` then signs the tagged commit with the same checks and attaches `OneShot.apk`.
- Pushes and PRs made with the default `GITHUB_TOKEN` do not start the gate (and PR checks show "approval required"). Set `RELEASE_PLEASE_TOKEN` so the gate runs automatically on every update of the release PR; otherwise start it with `make release-gate`.
- Enable "Require branches to be up to date before merging", so the gated PR head equals the commit that gets tagged.

Check the key locally before storing it: `keytool -list -v -keystore <file>` must show the alias with certificate SHA-256 `51:1C:89:33:…:04:56`.

**Manual rehearsal:** start `sign-release.yml` manually from `master` (`make release-rehearsal`, optionally `REF=<tag or commit>`), approve the run, and download the signed `OneShot.apk` artifact. It proves that the secrets, the alias and the fingerprint are right without creating a release; with a test key it only warns that F-Droid would not accept the key (set the input `require-fdroid-key` to fail instead).

## Rules that keep F-Droid green

- Toolchain changes go together: wrapper Gradle version, CI JDK (`JAVA_VERSION`), Debian image and JDK of the reproducibility and release jobs. They must match what the F-Droid buildserver uses, today `debian:trixie` with its default OpenJDK 21 (see *Toolchain*).
- Library and plugin versions live in the version catalog `gradle/libs.versions.toml`; `versionCode` and `versionName` stay literals in `app/build.gradle.kts`, because `fdroid checkupdates` reads them from that file with regular expressions.
- With AGP 8 or newer: `dependenciesInfo { includeInApk = false; includeInBundle = false }` and `vcsInfo { include = false }`.
- Dependabot (`.github/dependabot.yml`) opens grouped weekly updates as `build(deps)` and `ci(deps)` PRs, which never trigger a release. Major updates of the Android Gradle plugin are ignored there and done by hand, together with the CI containers. The Gradle wrapper is ignored completely and updated by hand, after checking which Gradle version the F-Droid buildserver and AGP support.
- While AGP stays on 8.13, `ignore` entries in `dependabot.yml` cap the versions it cannot build: Kotlin and KSP 2.4, Dagger/Hilt 2.59, Compose BOM 2026.08.00, `androidx.core` 1.19, Lifecycle 2.11, Navigation 2.10, `hilt-navigation-compose` 1.4. The AndroidX ones declare `minCompileSdk=37` and `minAndroidGradlePluginVersion=9.1.0` in their `META-INF/com/android/build/gradle/aar-metadata.properties`, so they need AGP 9 and compileSdk 37 together, while Kotlin and Hilt are capped by their own toolchain coupling. Patch and minor updates below a ceiling keep arriving, and the ceilings are lifted with the AGP 9 upgrade. Read that properties file before raising one.
- No proprietary libraries (Google Play services, Firebase, Crashlytics), no prebuilt binaries in the repository except the validated Gradle wrapper jar, no toolchain auto-download.
- Kotlin Multiplatform: iOS targets must not be configured on Linux builds unless explicitly enabled, so the F-Droid build never downloads Kotlin/Native.
- After the first release from the new pipeline, watch the F-Droid build log (monitor.f-droid.org) and, if the recipe needs changes, open a merge request on fdroiddata.

## Repository setup for releases

Create the `release` environment with its secrets and protection rules (see above) and run a signing rehearsal, enable Actions and "Allow GitHub Actions to create pull requests", set squash-only merges with the PR title as commit message, let release-please open the release PR, merge it, and verify the published `OneShot.apk` with `reproduce-release.yml` before F-Droid picks it up.
