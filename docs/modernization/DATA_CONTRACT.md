# Data contract

Everything listed here exists on users' phones today (v1.1.1) and must keep working after every update. Changing any of it is a **data-touching change**: it needs the `data-change-approved` label, a written compatibility or migration plan in the PR, and the data tests from [TESTING.md](TESTING.md). CI enforces the label for the paths in [`.github/data-contract-paths.txt`](../../.github/data-contract-paths.txt).

## 1. App identity

| Item | Value | Why it matters |
|---|---|---|
| `applicationId` | `de.ptrlx.oneshot` | A different id is a different app: no update, data stays in the old app. |
| Signing key | upstream key, SHA-256 `511c8933704278f936183c54ef963b9b66a0f6c232adca17bb52090f62b80456` (F-Droid `AllowedAPKSigningKeys`) | Android refuses updates signed with another key. Only upstream CI signs releases. |
| `versionCode` | strictly increasing (111 today) | Android refuses downgrades. |

## 2. Room database

- File: `databases/diary_entry_db` (constant `DiaryEntryDatabase.DATABASE_NAME`), opened in `di/AppModule.kt` without migrations or destructive fallback. Never add `fallbackToDestructiveMigration()`.
- Version **1**, exported schema `app/db-schema/de.ptrlx.oneshot.feature_diary.data.data_source.DiaryEntryDatabase/1.json`, identityHash `78c91e56607bf67a0e206c573e5cd700`. The schema directory name contains the database class's fully qualified name: moving the class means copying the schema files to the new directory name, never regenerating them.
- Table `DiaryEntry` (named after the entity class `domain/model/DiaryEntry.kt`; renaming the class renames the table unless `@Entity(tableName = "DiaryEntry")` is set):

| Column | SQLite type | Meaning |
|---|---|---|
| `date` | INTEGER NOT NULL, **primary key** | `LocalDate.toEpochDay()` via `Converters`; one entry per day |
| `created` | INTEGER NOT NULL | creation time in epoch **seconds** |
| `dayOfYear` | INTEGER NOT NULL | 1 to 366, used for flashbacks |
| `relativePath` | TEXT NOT NULL | image filename in the chosen folder |
| `happiness` | TEXT NOT NULL | `HappinessType` enum **name** (`VERY_HAPPY`, `HAPPY`, `NEUTRAL`, `SAD`, `VERY_SAD`, `NOT_SPECIFIED`) |
| `motivation` | TEXT NOT NULL | short text, default `""` |
| `textContent` | TEXT NOT NULL | diary text, default `""` |

Consequences: enum constants must never be renamed or removed (only added), the date-to-epoch-day conversion must stay, `getLastVeryHappyDay` contains the literal `'VERY_HAPPY'`.

## 3. Settings (DataStore)

- Preferences DataStore named `diary_settings` (file `files/datastore/diary_settings.preferences_pb`), created in `DiarySettingsImplementation`.
- Key `image_base_location` (`SettingsKeyDefinitions.imageBaseLocationKey`): string form of the SAF tree URI of the image folder.
- New keys may be added; existing keys keep name and type. Moving to Proto DataStore or another file needs a migration that reads the old file.

## 4. Image folder and files

- The user picks a folder with `OpenDocumentTree`; the app takes a **persistable read/write URI permission** and releases all others (`DiaryViewModel`, event `SetImageBaseLocation`). The system keeps this grant across updates as long as package and signature stay the same; it is lost on uninstall and does not transfer with Android backup.
- Images are created in that folder as `OneShot_yyyyMMddHHmmss.jpg` (`SimpleDateFormat` with the default locale, see #68). `CreateUpdateDiaryEntryUseCase` rejects any `relativePath` whose length is not **26**, which also applies to JSON import.
- `DiaryFileManager.resolveUri` builds document URIs by string concatenation (`document/<tree path>%2F<filename>`) instead of resolving them. Any change here must be tested with real providers (internal storage, SD card).
- Images live outside the app's private storage and survive uninstall. The app never deletes them automatically (maintainer decision, ptrLx/OneShot#11), except the empty placeholder file of an aborted camera capture.

## 5. JSON export and import

- Export file `OneShot_DB_yyyyMMddHHmmss.json` written into the image folder: a JSON array of objects with exactly these keys (custom `DiaryEntrySerializer`):

```json
[{"date": 19450, "created": 1680516900, "dayOfYear": 93, "relativePath": "OneShot_20230403101500.jpg", "happiness": "VERY_HAPPY", "motivation": "", "textContent": "A good day"}]
```

- Import (`OpenDocument` with `application/json`) decodes the array and inserts every entry with `OnConflictStrategy.REPLACE`: an imported entry overwrites an existing entry of the same date. Unknown `happiness` values become `NOT_SPECIFIED`. Images are not part of the export.
- Every export ever written must stay importable. A new format gets a version field and the importer keeps reading the old one (golden files in tests, #21).

## 6. Backup

`android:allowBackup="true"` with empty `backup_rules.xml` and `data_extraction_rules.xml`, so Android backup and device transfer include the database and DataStore but not the folder permission or the images (#69).

## Change rules

| Change | Allowed without approval | Needs `data-change-approved` + migration plan |
|---|---|---|
| UI, navigation, theming, strings | yes | |
| Reading existing data in new ways (stats, search via `LIKE`, calendar) | yes | |
| New DataStore keys | yes (additive) | |
| Moving or renaming data classes, DB, DAO, serializer | | yes, and the schema and JSON output must stay byte-identical |
| New columns or tables, index, FTS | | yes (Room `Migration` + `MigrationTestHelper` test) |
| New enum constants | | yes (old apps importing new exports map them to `NOT_SPECIFIED`) |
| File naming, folder layout, permission handling | | yes |
| JSON format | | yes (versioned, old format still importable) |
