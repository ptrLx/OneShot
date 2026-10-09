package de.ptrlx.oneshot.migration

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.ptrlx.oneshot.feature_diary.data.data_source.DiaryEntryDatabase
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Room migration tests (docs/modernization/TESTING.md, layer 3).
 *
 * A database is created from the exported schema `app/db-schema/<DiaryEntryDatabase>/<version>.json`
 * (packaged as androidTest assets) and filled with raw SQL exactly as that app version wrote it. It is
 * then opened like the app opens it and every row must read back identically.
 *
 * Today there is only version 1 (every installation since v1.0.0), so the test proves that the current
 * code still opens a v1.1.1 database without a migration and without changing it. When the schema gets
 * a version N:
 * - add the production `Migration(N - 1, N)` to [ALL_MIGRATIONS] (the same list `AppModule` passes to
 *   `addMigrations`),
 * - add a test `migrate N-1 to N` that creates version N-1 with rows as that version wrote them, calls
 *   `helper.runMigrationsAndValidate(TEST_DB, N, true, MIGRATION_N_1_N)` and checks the migrated rows,
 * - bump [LATEST_VERSION]: `v1_database_opens_with_the_current_code_and_reads_back_every_row` then
 *   runs all migrations from 1 to N in one go, like an update from v1.1.1 to the newest version.
 */
@RunWith(AndroidJUnit4::class)
class DiaryEntryDatabaseMigrationTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, DiaryEntryDatabase::class.java)

    @After
    fun deleteTestDatabase() {
        instrumentation.targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun v1_database_opens_with_the_current_code_and_reads_back_every_row(): Unit = runBlocking {
        createV1Database()

        val database = openLikeTheApp()
        val entries = database.diaryEntryDao.getDiaryEntries().first()
        database.close()

        assertEquals(v1Entries, entries.sortedBy { it.date })
    }

    @Test
    fun opening_a_v1_database_does_not_change_the_stored_values() {
        createV1Database()

        val database = openLikeTheApp()
        val db = database.openHelper.writableDatabase
        val rows = readRows(db)
        val identityHash = db.query("SELECT identity_hash FROM room_master_table WHERE id = 42").use {
            it.moveToFirst()
            it.getString(0)
        }
        database.close()

        assertEquals(v1Rows, rows)
        assertEquals(V1_IDENTITY_HASH, identityHash)
    }

    @Test
    fun v1_schema_validates_against_all_migrations() {
        createV1Database()

        val db = helper.runMigrationsAndValidate(TEST_DB, LATEST_VERSION, true, *ALL_MIGRATIONS)

        assertEquals(v1Rows, readRows(db))
    }

    @Test
    fun dao_queries_used_by_the_app_work_on_v1_data(): Unit = runBlocking {
        createV1Database()

        val dao = openLikeTheApp().diaryEntryDao
        assertEquals(v1Entries[2], dao.getDiaryEntry(LocalDate.of(2023, 4, 3)).first())
        assertEquals(
            listOf(v1Entries[1], v1Entries[5]),
            dao.getDiaryEntriesDateOfYear(60).first().sortedBy { it.date },
        )
        assertEquals(v1Entries[2], dao.getLastVeryHappyDay().first())
        assertEquals(listOf(v1Entries[3]), dao.getDiaryEntries(HappinessType.SAD).first())
        assertEquals(listOf(v1Entries[3]), dao.getDiaryEntries("東京").first())
    }

    private fun createV1Database() {
        helper.createDatabase(TEST_DB, 1).apply {
            v1Rows.forEach { row ->
                execSQL(
                    "INSERT INTO DiaryEntry (date, created, dayOfYear, relativePath, happiness, motivation, textContent) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    row.toTypedArray(),
                )
            }
            close()
        }
    }

    /** Opens the database with the same configuration as `AppModule.provideDiaryEntryDatabase`. */
    private fun openLikeTheApp(): DiaryEntryDatabase =
        Room.databaseBuilder(instrumentation.targetContext, DiaryEntryDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
            .also { helper.closeWhenFinished(it) }

    private fun readRows(db: SupportSQLiteDatabase): List<List<Any>> =
        db.query(
            "SELECT date, created, dayOfYear, relativePath, happiness, motivation, textContent FROM DiaryEntry ORDER BY date"
        ).use { cursor ->
            val rows = mutableListOf<List<Any>>()
            while (cursor.moveToNext()) {
                rows += listOf<Any>(
                    cursor.getLong(0),
                    cursor.getLong(1),
                    cursor.getLong(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    cursor.getString(5),
                    cursor.getString(6),
                )
            }
            rows
        }

    companion object {
        private const val TEST_DB = "migration-test"

        /** Current `@Database(version = ...)` of [DiaryEntryDatabase]. */
        private const val LATEST_VERSION = 1

        /** identityHash of `app/db-schema/.../1.json`, the schema every existing installation has. */
        private const val V1_IDENTITY_HASH = "78c91e56607bf67a0e206c573e5cd700"

        /** All production migrations, in order. Empty while the schema is at version 1. */
        private val ALL_MIGRATIONS = arrayOf<Migration>()

        /**
         * Rows as v1.1.1 stores them: date as epoch day, created in epoch seconds, happiness as enum
         * name. Covers every happiness value, epoch day 0, leap days, day 366, a date after 2099,
         * empty strings, multi-line, unicode and emoji text and SQL-relevant characters.
         */
        private val v1Rows: List<List<Any>> = listOf(
            listOf(0L, 3_600L, 1L, "OneShot_19700101010000.jpg", "HAPPY", "", ""),
            listOf(18_321L, 1_582_970_400L, 60L, "OneShot_20200229100000.jpg", "NEUTRAL", "Keep going", "Leap day\nsecond line\n"),
            listOf(19_450L, 1_680_516_900L, 93L, "OneShot_20230403101500.jpg", "VERY_HAPPY", "", "A good day"),
            listOf(19_722L, 1_704_020_400L, 365L, "OneShot_20231231110000.jpg", "SAD", "Grüße", "Grüße aus 東京 😀👍🏽"),
            listOf(20_088L, 1_735_642_800L, 366L, "OneShot_20241231120000.jpg", "VERY_SAD", "100%", "'single' \"double\" ; -- %_ \\"),
            listOf(47_541L, 4_107_574_800L, 60L, "OneShot_21000301090000.jpg", "NOT_SPECIFIED", "", "after 2099"),
        )

        /** [v1Rows] as the app's model, written independently of the converters under test. */
        private val v1Entries: List<DiaryEntry> = listOf(
            DiaryEntry(LocalDate.of(1970, 1, 1), 3_600L, 1, "OneShot_19700101010000.jpg", HappinessType.HAPPY, "", ""),
            DiaryEntry(LocalDate.of(2020, 2, 29), 1_582_970_400L, 60, "OneShot_20200229100000.jpg", HappinessType.NEUTRAL, "Keep going", "Leap day\nsecond line\n"),
            DiaryEntry(LocalDate.of(2023, 4, 3), 1_680_516_900L, 93, "OneShot_20230403101500.jpg", HappinessType.VERY_HAPPY, "", "A good day"),
            DiaryEntry(LocalDate.of(2023, 12, 31), 1_704_020_400L, 365, "OneShot_20231231110000.jpg", HappinessType.SAD, "Grüße", "Grüße aus 東京 😀👍🏽"),
            DiaryEntry(LocalDate.of(2024, 12, 31), 1_735_642_800L, 366, "OneShot_20241231120000.jpg", HappinessType.VERY_SAD, "100%", "'single' \"double\" ; -- %_ \\"),
            DiaryEntry(LocalDate.of(2100, 3, 1), 4_107_574_800L, 60, "OneShot_21000301090000.jpg", HappinessType.NOT_SPECIFIED, "", "after 2099"),
        )
    }
}
