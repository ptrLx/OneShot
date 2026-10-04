# AGENTS.md

Instructions for humans and coding agents working on OneShot, a privacy-first Android photo diary distributed through F-Droid. Claude Code reads this file through `CLAUDE.md` (`@AGENTS.md`).

## Non-negotiable constraints

1. **F-Droid must keep building and reproducing every release.** Keep `versionCode`/`versionName` as literals in `app/build.gradle(.kts)`, tags `vX.Y.Z` only, release asset `OneShot.apk`, no proprietary dependencies, no toolchain auto-download, no `INTERNET` permission. See `docs/modernization/RELEASING.md`.
2. **Existing installations upgrade in place.** `applicationId` stays `de.ptrlx.oneshot`, `versionCode` only increases, releases are signed with the upstream key by upstream CI only.
3. **No user data is lost.** Do not change the Room schema, DataStore names/keys, image file naming/folder handling or the JSON export format without the `data-change-approved` label and a migration/compatibility plan. See `docs/modernization/DATA_CONTRACT.md` and `docs/modernization/TESTING.md`. Never delete user photos automatically.

## Workflow

- Plan and order of work: `docs/modernization/ROADMAP.md`; backlog: GitHub issues of the fork (epics #1 to #9).
- One topic branch per issue, small PRs, squash merge only. The PR title becomes the commit message and must be a conventional commit (`feat:`, `fix:`, `build:`, `ci:`, `refactor:`, `docs:`, `test:`, `chore:`); release-please derives versions from it.
- Never edit `CHANGELOG.md`, `.release-please-manifest.json`, `versionName` or `versionCode` by hand: release-please and `scripts/release/sync_version.py` own them.
- No agent or tool attribution in commits, PRs or code comments.
- CI is the build machine (no local Android SDK required): use the `Makefile` targets (`make help`). Every PR must pass the build, schema freeze, F-Droid, APK, reproducibility and data-contract checks.

## Code map

- `app/src/main/java/de/ptrlx/oneshot/feature_diary/`: single feature, layered `data` (Room, DataStore), `domain` (model, use cases, `DiaryFileManager`), `presentation` (Compose UI, one `DiaryViewModel`).
- `app/db-schema/`: exported Room schemas, never edited by hand.
- `fastlane/metadata/android/`: F-Droid store texts, icon, screenshots, changelogs per versionCode.
- `scripts/ci/`: CI checks, `scripts/release/`: versioning helpers.
