# OneShot modernization roadmap

This is the plan for refactoring and modernizing OneShot in the fork [ptrLxAI/OneShot](https://github.com/ptrLxAI/OneShot) so that all changes can later be merged into [ptrLx/OneShot](https://github.com/ptrLx/OneShot) and shipped as a normal F-Droid update. The backlog lives in GitHub issues (epics #1 to #9); this document explains the order, the rules and the reasoning.

## Hard constraints

Every change, without exception, must keep these three properties. They are checked in CI where possible.

1. **The F-Droid pipeline keeps working.** F-Droid builds OneShot from source ([`metadata/de.ptrlx.oneshot.yml`](https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/de.ptrlx.oneshot.yml)): `subdir: app`, `gradle: yes`, new versions are discovered from git tags (`UpdateCheckMode: Tags`, `AutoUpdateMode: Version`) and the build must be byte-identical to the upstream-signed `OneShot.apk` attached to the GitHub release `v<version>` (`Binaries:` + `AllowedAPKSigningKeys`, reproducible builds). Details: [RELEASING.md](RELEASING.md).
2. **Existing installations upgrade in place.** Same `applicationId` (`de.ptrlx.oneshot`), same signing key (upstream key, only used in upstream CI), strictly increasing `versionCode`.
3. **No user data is lost.** The Room database, the DataStore settings, the persisted folder permission and the images in the user's folder must survive every update unchanged, and old JSON exports must stay importable. Details: [DATA_CONTRACT.md](DATA_CONTRACT.md), test strategy: [TESTING.md](TESTING.md).

## Key findings that shape the plan

- **F-Droid could not build master before the toolchain upgrade.** The F-Droid buildserver runs Debian trixie with OpenJDK 21 by default, and the old Gradle 7.4 wrapper does not run on Java 21. The toolchain upgrade (#26: Gradle 9.4.1, AGP 8.13, Kotlin 2.0, JDK 21) was therefore a release blocker, not polish.
- **Reproducible builds are the most fragile part.** Our signed `OneShot.apk` must match F-Droid's own build. CI rebuilds every change in an F-Droid-like Debian container and compares the APKs byte by byte, and `reproduce-release.yml` checks published releases with `apksigcopier`.
- **The data contract is small but implicit.** One Room table (schema v1, identityHash `78c91e56607bf67a0e206c573e5cd700`), one DataStore key holding a SAF tree URI, image files named `OneShot_yyyyMMddHHmmss.jpg` (the 26-character length is enforced on save and import), and a JSON export format. Nothing protected it so far.
- **The product promises "no internet permission".** The CI checks the built APK for it. Cloud sync therefore needs an opt-in design (see #66).
- **Upstream maintainer decisions to respect:** photos are never deleted automatically (ptrLx/OneShot#11).

## Working model

- **Repositories:** work happens only in the fork (remotes: `origin` = fork, `upstream` = ptrLx/OneShot). Parallel work uses one git worktree per topic branch.
- **Branches and PRs:** one topic branch per issue (`ci/…`, `build/…`, `feat/…`, `fix/…`, `refactor/…`, `docs/…`), small PRs, **squash merge only** into `master` of the fork. The PR title is the squash commit message and must be a [conventional commit](https://www.conventionalcommits.org/) (checked by CI), because release-please derives versions and the changelog from it.
- **CI is the build machine.** Every PR builds debug and unsigned release APKs (downloadable artifacts), runs unit tests and all guards. A local Android SDK is not required (local setup: #71).
- **Labels:** `data-touching` marks backlog items that change persisted data. `data-change-approved` must be set by the maintainer before CI lets a PR touch data-contract paths. `needs-decision` blocks on the maintainer.
- **Upstream merge:** the maintainer merges fork `master` into ptrLx/OneShot when a milestone is complete. Release PRs are not merged in the fork (no signing key there, and tags must not diverge from upstream). Checklist: #19.

## Phases

Each phase is a GitHub milestone. Phases are ordered by risk: first make breakage visible, then upgrade the toolchain behavior-neutrally, then change the UI, then the architecture. Data-touching work is deliberately last and needs its own design.

### M0 Foundation (no app code changes)

| Issue | Topic |
|---|---|
| #10 #11 #12 #13 #17 | CI baseline: build, unit tests, APK artifacts, Room schema freeze, data-contract guard, F-Droid metadata and APK checks, reproducibility check, PR title lint (PR #75) |
| #14 #15 | release-please versioning with versionCode and fastlane changelog sync, release workflow producing `OneShot.apk` (PR #76) |
| #16 | nightly builds on master (APK artifacts of every master build) |
| #18 | this plan, data contract, testing strategy, agent instructions |
| #20 #21 #22 #24 | data-contract tests, JSON golden files, Room migration test infrastructure, manual QA checklist |
| #19 | upstream handover checklist |

Exit criteria: CI green on master with all guards, v1.1.1 reproduced from source by `reproduce-release.yml`, contract and golden tests in place.

### M1 Toolchain modernization (behavior-neutral)

| Issue | Topic |
|---|---|
| #26 | **release blocker:** Gradle 9, AGP 8/9, JDK 21 to match the F-Droid buildserver, CI containers to `debian:trixie`, `dependenciesInfo` and `vcsInfo` off, decide on `fix-profm.gradle` |
| #27 | Kotlin 2.x, Compose compiler plugin, Compose BOM |
| #28 | kapt to KSP for Room and Hilt with a byte-identical schema |
| #29 | Gradle Kotlin DSL and version catalog (versionCode/versionName stay literal in `app/build.gradle.kts`) |
| #30 | compileSdk/targetSdk 36 with behavior-change review (minSdk stays 29, Android 10) |
| #31 | replace deprecated and pre-release libraries |
| #32 | Dependabot version updates (grouped) |
| #33 | Spotless/ktlint, Android lint in CI |
| #34 | R8 evaluation (decision) |
| #23 | emulator upgrade E2E (v1.1.1 to HEAD) running in CI |

Exit criteria: same app behavior, reproducible in `debian:trixie`, upgrade E2E green on API 29 and the newest API. This is the first state worth releasing upstream (it unblocks F-Droid).

### M2 Material 3 and native Android UX (UI only)

#35 Material 3 with dynamic color, #36 edge-to-edge, predictive back and splash screen, #37 M3 navigation and adaptive layouts, #38 photo picker, #39 per-app language, #40 accessibility, #41 M3 sheets and snackbars, #42 widget and shortcut, #25 Compose UI smoke tests, plus branding #74 (logo v2, separate PR).

### M3 Architecture and Kotlin Multiplatform

#46 split the 352-line `DiaryViewModel` and modularize, #43 `:shared` KMP module for pure domain logic, #44 iOS targets gated so the F-Droid build never downloads Kotlin/Native, #45 DI strategy (Hilt stays in the app), #47 iOS app shell (planning). Persistence stays in the Android app.

### M4 Features that do not change persisted data

From upstream issues and new feature ideas: #48 reminders, #49 share photo, #50 calendar beyond 28 days, #51 richer stats, #52 flashbacks, #53 search and filter, #54 Markdown rendering, #55 theme selection and onboarding, #56 delete with undo and explicit keep-the-photo semantics. New settings are additive DataStore keys only.

### Backlog: data-touching (deferred)

Excluded from the first refactoring. Each item needs a migration design, Room `Migration` with a `MigrationTestHelper` test, JSON backward compatibility and a green upgrade E2E before work starts: #57 black images (upstream #17), #58 revoked folder permission, #59 orphaned images, #60 automatic JSON backups, #61 full archive export and restore, #62 multiple photos per entry, #63 thoughts list, #64 location, #65 stable IDs and timestamps, #66 sync, #67 delete all, #68 locale-independent filenames, #69 backup rules, #70 KMP persistence.

### Backlog: infrastructure

#71 local Android development setup (needs about 20 GB of free disk), #72 real `fdroid build` simulation, #73 scripted store screenshots.

## Decisions for the maintainer

| Topic | Recommendation | Issue/PR |
|---|---|---|
| versionCode scheme | `MAJOR*10000 + MINOR*100 + PATCH` (1.2.0 is 10200) | #76 |
| Releases in the fork | do not merge release PRs in the fork; first real release upstream | #15, #76 |
| JDK for releases | follow the F-Droid buildserver default (OpenJDK 21 on trixie), no `sudo:` override in fdroiddata | #26 |
| R8 for release builds | decide after upgrade E2E exists | #34 |
| DI in shared KMP code | constructor injection in `:shared`, Hilt stays in the app | #45 |
| Location and sync features | only offline or as an opt-in flavor; keep "no internet" in the F-Droid build | #64, #66 |
