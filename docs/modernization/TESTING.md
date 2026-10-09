# Testing strategy: data consistency end to end

Goal: prove for every change that a user who updates from any released version (today: v1.1.1 from F-Droid) keeps all entries, settings, the folder permission and the images, and that old JSON exports still import. The contract being protected is [DATA_CONTRACT.md](DATA_CONTRACT.md).

The strategy has five layers. Cheap layers run on every PR; expensive layers run on PRs labeled `data-touching` and before every release.

| Layer | What it proves | Runs on | Issue |
|---|---|---|---|
| 0. Static guards | schema and data code did not change by accident | every PR | #11 |
| 1. Contract unit tests | names, keys, enums and formats are still the old ones | every PR (JVM) | #20 |
| 2. JSON golden files | every export ever written still imports identically | every PR (JVM) | #21 |
| 3. Room migration tests | a v1 database opens and reads back identically with the current code | every PR (instrumented, emulator job) | #22 |
| 4. Upgrade E2E | real APK upgrade v1.1.1 to HEAD on an emulator keeps everything | `data-touching` PRs, master nightly, release PRs | #23 |
| 5. Manual release QA | real device, real F-Droid install, real folder provider | every release | #24 |

## Layer 0: static guards (in CI since PR #75)

- **Schema freeze:** Room exports the schema on every compile. CI fails when the build changes anything under `app/db-schema/`, and pins the identityHash of version 1.
- **Data-contract guard:** PRs touching paths from `.github/data-contract-paths.txt` fail until the maintainer adds `data-change-approved`.
- **APK guard:** package name and versionCode of the built APK, no `INTERNET` permission.

## Layer 1: contract unit tests (JVM, fast)

Implemented in `app/src/test/java/de/ptrlx/oneshot/contract/` (run by `make test` and CI's `testDebugUnitTest`). Identifiers that only exist as private literals (DataStore name, filename patterns, MIME types) or in Room-generated code (CREATE TABLE, identity hash, `INSERT OR REPLACE`) are read from the compiled class files, so production code stays untouched.

Pin every identifier from the data contract in plain JUnit tests so a rename fails loudly, independent of the guard:

- `DiaryEntryDatabase.DATABASE_NAME == "diary_entry_db"`, table and column names (read from the exported schema JSON)
- DataStore name `diary_settings`, key `image_base_location`
- `HappinessType.entries.map { it.name }` equals the six stored names in order
- image filename pattern: generated names match `^OneShot_\d{14}\.jpg$` and have length 26 for a fixed clock and the `Locale.ROOT`; document the behavior for locales with non-ASCII digits (#68)
- `Converters` round-trip `LocalDate` to epoch day for edge dates (1970-01-01, leap days, year 2100)

## Layer 2: JSON golden files (JVM, fast)

Implemented in `JsonExportGoldenTest` next to the Layer 1 tests. v1.0.0 to v1.1.1 share one serializer, so there is one format today. The export has no nondeterministic content, so it is compared byte for byte. The fixtures are marked `-text` in `.gitattributes` so git never rewrites their line endings.

- Fixtures under `app/src/test/resources/export/`: `v1.1.1-basic.json` (all happiness values, unicode and emoji text, empty strings, leap day, multi-line text) and `v1.1.1-unknown-happiness.json`.
- Tests: decoding each fixture yields the expected `DiaryEntry` list; encoding that list reproduces the fixture byte for byte; unknown happiness decodes to `NOT_SPECIFIED`; an import of an entry whose `relativePath` is not 26 characters is rejected (current behavior, made explicit).
- Rule: fixtures are never edited, only added. A new export format adds a new fixture and keeps the old tests.

## Layer 3: Room migration tests (instrumented)

In place: `app/src/androidTest/java/de/ptrlx/oneshot/migration/DiaryEntryDatabaseMigrationTest.kt`, run by the `Emulator` workflow on every PR (`./gradlew connectedDebugAndroidTest`); the `CI` build compiles it (`assembleDebugAndroidTest`).

- `androidx.room:room-testing` (`MigrationTestHelper`) reads the exported schemas: `app/db-schema` is an asset directory of the `androidTest` source set.
- `helper.createDatabase(TEST_DB, 1)` creates a v1 database from `1.json`; the rows are inserted with raw SQL exactly as v1.1.1 wrote them (epoch-day dates, epoch-second timestamps, enum names as text), covering every happiness value, edge dates, empty, multi-line, unicode and emoji text.
- The database is opened with the production `Room.databaseBuilder` configuration (`openLikeTheApp()`, mirroring `AppModule` with the same migrations), so Room's identity check proves the compiled schema still equals `1.json`. All rows read back identically through the DAO, the raw stored values and the identityHash are unchanged afterwards, and the DAO queries the app uses (by date, day of year, happiness, keyword, last very happy day) work on v1 data.

Pattern for a future schema version N (needs `data-change-approved`):

1. Bump `@Database(version = N)`, commit the generated `N.json` (never edit older ones), write `MIGRATION_{N-1}_N` in production code and register it in `AppModule`.
2. In the test, add it to `ALL_MIGRATIONS` and bump `LATEST_VERSION`. The existing v1 tests then run all migrations from 1 to N in one go (`runMigrationsAndValidate(TEST_DB, LATEST_VERSION, true, *ALL_MIGRATIONS)` validates the result against `N.json`).
3. Add a test `migrate N-1 to N`: `createDatabase(TEST_DB, N - 1)`, insert rows as version N-1 wrote them, `runMigrationsAndValidate(TEST_DB, N, true, MIGRATION_{N-1}_N)`, then assert the migrated rows (and the defaults of new columns).

## Layer 4: upgrade end-to-end test (emulator)

This is the test that matches the user's real situation: an installed old version with data, upgraded in place.

**Why we build the old version ourselves:** the published v1.1.1 APK is signed with the upstream key, so a CI build of HEAD cannot be installed over it. The job therefore builds `v1.1.1` from its tag and HEAD from the PR, both signed with the same throwaway CI test key. Signature continuity for real users is covered separately by the release flow (same key, increasing versionCode).

**Environment:** `reactivecircus/android-emulator-runner` on Linux runners with KVM, `google_apis` system images (root available), API 29 (minSdk, Android 10) and the newest API level.

**Steps:**

1. Install the old APK and launch it once so it creates its directories.
2. Seed state as v1.1.1 would have written it:
   - create a folder `/sdcard/OneShot` with fixture JPEGs named like real captures;
   - grant the folder: drive the system folder picker with UI Automator once (`OpenDocumentTree`), which also writes the DataStore key, or with `adb root` push a prepared `diary_settings.preferences_pb` and grant the URI permission through the picker;
   - with `adb root`, stop the app and push a fixture `diary_entry_db` (created from the v1 schema SQL with `sqlite3`, rows pointing at the fixture images);
3. Record a canonical snapshot: `sqlite3 .dump` of the DB, the DataStore file hash, the persisted URI permissions (`dumpsys activity permissions`/`cmd uri`), file list and hashes of the folder.
4. `adb install -r` the new APK (in-place update, keeps data), launch it, let it open the DB (runs migrations, if any).
5. Record the snapshot again and compare: rows equal (or equal after the documented migration mapping), DataStore unchanged, permission still present, no image file changed or removed.
6. UI checks with UI Automator: the diary shows the seeded entries, images render (not black, see #57), an export from the new app equals the fixture export semantically, and an import of the old export succeeds.

Artifacts on failure: both snapshots, logcat, screenshots.

**When it runs:** on PRs labeled `data-touching` or touching the data-contract paths, nightly on master, and as a required check on release PRs. It is too slow (about 15 minutes per API level) for every PR.

## Layer 5: manual release QA

Checklist in the release PR (template to add with #24): on a real phone with OneShot from F-Droid installed and real data, install the release candidate signed with the release key (upstream only) or verify the F-Droid update after publication:

- entries, calendar, stats and flashbacks unchanged
- images visible, including entries older than a year
- folder still accessible without re-picking it; capture today still writes into it
- export, then import the same file again: no duplicates, no changes
- app survives a reboot and a system update of the documents provider

## UI regression tests

Compose UI tests for the main flows (#25) and, later, screenshot tests (#73). They protect the M2 redesign but are not part of the data guarantee.
