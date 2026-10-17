# OneShot modernization roadmap

This is the plan for refactoring and modernizing OneShot so that every change ships as a normal F-Droid update. It explains the order, the rules and the reasoning; open work is tracked in GitHub issues.

## Hard constraints

Every change, without exception, must keep these three properties. They are checked in CI where possible.

1. **The F-Droid pipeline keeps working.** F-Droid builds OneShot from source ([`metadata/de.ptrlx.oneshot.yml`](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/de.ptrlx.oneshot.yml)): `subdir: app`, `gradle: yes`, new versions are discovered from git tags (`UpdateCheckMode: Tags`, `AutoUpdateMode: Version`) and the build must be byte-identical to the signed `OneShot.apk` attached to the GitHub release `v<version>` (`Binaries:` + `AllowedAPKSigningKeys`, reproducible builds). Details: [RELEASING.md](RELEASING.md).
2. **Existing installations upgrade in place.** Same `applicationId` (`de.ptrlx.oneshot`), same signing key (only used in this repository's release environment), strictly increasing `versionCode`.
3. **No user data is lost.** The Room database, the DataStore settings, the persisted folder permission and the images in the user's folder must survive every update unchanged, and old JSON exports must stay importable. Details: [DATA_CONTRACT.md](DATA_CONTRACT.md), test strategy: [TESTING.md](TESTING.md).

## Key findings that shape the plan

- **F-Droid could not build master before the toolchain upgrade.** The F-Droid buildserver runs Debian trixie with OpenJDK 21 by default, and the old Gradle 7.4 wrapper does not run on Java 21. The toolchain upgrade (Gradle 9.4.1, AGP 8.13, JDK 21) was therefore a release blocker, not polish.
- **Reproducible builds are the most fragile part.** Our signed `OneShot.apk` must match F-Droid's own build. CI rebuilds every change in an F-Droid-like Debian container and compares the APKs byte by byte, and `reproduce-release.yml` checks published releases with `apksigcopier`.
- **The data contract is small but implicit.** One Room table (schema v1, identityHash `78c91e56607bf67a0e206c573e5cd700`), one DataStore key holding a SAF tree URI, image files named `OneShot_yyyyMMddHHmmss.jpg` (the 26-character length is enforced on save and import), and a JSON export format. Nothing protected it before the contract tests.
- **The product promises "no internet permission".** The CI checks the built APK for it. Cloud sync therefore needs an opt-in design.
- **Maintainer decisions to respect:** photos are never deleted automatically (#11).

## Working model

- **Branches and PRs:** one topic branch per issue (`ci/…`, `build/…`, `feat/…`, `fix/…`, `refactor/…`, `docs/…`), small PRs, **squash merge only** into `master`. The PR title is the squash commit message and must be a [conventional commit](https://www.conventionalcommits.org/) (checked by CI), because release-please derives versions and the changelog from it.
- **CI is the build machine.** Every PR builds debug and unsigned release APKs (downloadable artifacts), runs unit tests and the fast guards. The reproducible Debian build, the emulator tests and the upgrade test run on master and on the release branch, or on demand with `make verify-run`. A local Android SDK is not required.
- **Labels:** `data-touching` marks backlog items that change persisted data. `data-change-approved` must be set by the maintainer before CI lets a PR touch data-contract paths.
- **Releases:** release-please keeps a release PR open; the release gate rehearses signing on it and only lets it merge when F-Droid accepts the key. Details: [RELEASING.md](RELEASING.md).

## Phases

Phases are ordered by risk: first make breakage visible, then upgrade the toolchain behavior-neutrally, then change the UI, then the architecture. Data-touching work is deliberately last and needs its own design.

### M0 Foundation (no app code changes), done

CI baseline (build, unit tests, APK artifacts, Room schema freeze, data-contract guard, F-Droid metadata and APK checks, reproducibility check, PR title lint), release-please versioning with versionCode and fastlane changelog sync, release signing in a protected environment with a release gate, this plan with the data contract and testing strategy, data-contract tests, JSON golden files, Room migration test infrastructure. Open: nightly builds on master, the manual release QA template.

### M1 Toolchain modernization (behavior-neutral), done

Gradle 9, AGP 8.13 and JDK 21 matching the F-Droid buildserver, CI containers on `debian:trixie`, `dependenciesInfo` and `vcsInfo` off, baseline profile workaround removed; Kotlin 2.3 with the Compose compiler plugin and the Compose BOM; kapt to KSP for Room and Hilt with a byte-identical schema; Gradle Kotlin DSL and version catalog (versionCode/versionName stay literal in `app/build.gradle.kts`); compileSdk/targetSdk 36 with a behavior-change review (minSdk stays 29, Android 10); deprecated and pre-release libraries replaced; grouped Dependabot updates; emulator upgrade test from v1.1.1 to HEAD on API 29 and 36. Open: Coil 3, Spotless/ktlint and Android lint in CI, R8 evaluation.

### M2 Material 3 and native Android UX (UI only)

Done: edge-to-edge, predictive back and splash screen, logo v2. Open: Material 3 with dynamic color, M3 navigation and adaptive layouts, system photo picker, per-app language, accessibility, M3 sheets and snackbars, widget and shortcut, Compose UI smoke tests, visible image load errors.

### M3 Architecture and Kotlin Multiplatform

Split the 352-line `DiaryViewModel` and modularize, a `:shared` KMP module for pure domain logic, iOS targets gated so the F-Droid build never downloads Kotlin/Native, a DI strategy (Hilt stays in the app), an iOS app shell (planning). Persistence stays in the Android app.

### M4 Features that do not change persisted data

From issues and feature ideas: reminders, share photo, calendar beyond 28 days, richer stats, flashbacks, search and filter, Markdown rendering, theme selection and onboarding, delete with undo and explicit keep-the-photo semantics. New settings are additive DataStore keys only.

### Backlog: data-touching (deferred)

Each item needs a migration design, Room `Migration` with a `MigrationTestHelper` test, JSON backward compatibility and a green upgrade test before work starts: black images (#17), revoked folder permission, orphaned images, automatic JSON backups, full archive export and restore, multiple photos per entry, a thoughts list, location, stable IDs and timestamps, sync, delete all, locale-independent filenames, backup rules, KMP persistence.

### Backlog: infrastructure

Local Android development setup (needs about 20 GB of free disk), a real `fdroid build` simulation, scripted store screenshots.

## Decisions for the maintainer

| Topic | Recommendation |
|---|---|
| versionCode scheme | `MAJOR*10000 + MINOR*100 + PATCH` (1.2.0 is 10200) |
| JDK for releases | follow the F-Droid buildserver default (OpenJDK 21 on trixie), no `sudo:` override in fdroiddata |
| R8 for release builds | decide with the upgrade test in place |
| DI in shared KMP code | constructor injection in `:shared`, Hilt stays in the app |
| Location and sync features | only offline or as an opt-in flavor; keep "no internet" in the F-Droid build |
