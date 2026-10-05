package de.ptrlx.oneshot.contract

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.model.InvalidDiaryEntryException
import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry.CreateUpdateDiaryEntryUseCase
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.InputStream
import java.time.LocalDate

/**
 * Golden-file tests for the JSON export and import (docs/modernization/DATA_CONTRACT.md, section 5).
 *
 * Fixtures live in `src/test/resources/export/` and are never edited, only added: every export ever
 * written by a released version must keep importing. All released versions (v1.0.0 to v1.1.1) share one
 * format, written by `DiaryEntrySerializer` through the default [Json] instance.
 *
 * [export] and [import] mirror `DiaryFileManager.writeJSONExport` and `readJSONExport` without the
 * Android file access: `Json.encodeToString(entries)` written as UTF-8, `Json.decodeFromStream`.
 * The export contains no timestamps or other nondeterministic data, so it is compared byte for byte.
 */
class JsonExportGoldenTest {

    private fun fixture(name: String): InputStream =
        javaClass.getResourceAsStream("/export/$name") ?: throw AssertionError("fixture export/$name missing")

    private fun fixtureBytes(name: String): ByteArray = fixture(name).use { it.readBytes() }

    private fun export(entries: List<DiaryEntry>): ByteArray = Json.encodeToString(entries).toByteArray(Charsets.UTF_8)

    private fun import(stream: InputStream): List<DiaryEntry> = stream.use { Json.decodeFromStream<List<DiaryEntry>>(it) }

    private fun import(json: String): List<DiaryEntry> = import(json.byteInputStream(Charsets.UTF_8))

    @Test
    fun `v1_1_1 export decodes to the expected entries`() {
        assertEquals(basicEntries, import(fixture(BASIC)))
    }

    @Test
    fun `exporting the expected entries reproduces the v1_1_1 file byte for byte`() {
        assertBytes(fixtureBytes(BASIC), export(basicEntries))
    }

    @Test
    fun `v1_1_1 export survives decode and encode unchanged`() {
        assertBytes(fixtureBytes(BASIC), export(import(fixture(BASIC))))
    }

    @Test
    fun `fixture entries are consistent with the data contract`() {
        val imageName = Regex("^OneShot_\\d{14}\\.jpg$")
        assertEquals(
            "fixture must cover every happiness value",
            HappinessType.values().toSet(),
            basicEntries.map { it.happiness }.toSet(),
        )
        basicEntries.forEach {
            assertEquals("dayOfYear of ${it.date}", it.date.dayOfYear, it.dayOfYear)
            assertTrue("relativePath ${it.relativePath}", imageName.matches(it.relativePath))
        }
    }

    @Test
    fun `unknown happiness values import as NOT_SPECIFIED`() {
        val entries = import(fixture(UNKNOWN_HAPPINESS))
        assertEquals(
            listOf(HappinessType.NOT_SPECIFIED, HappinessType.NOT_SPECIFIED, HappinessType.NOT_SPECIFIED, HappinessType.HAPPY),
            entries.map { it.happiness },
        )
        assertEquals(
            listOf(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 2), LocalDate.of(2024, 3, 3), LocalDate.of(2024, 3, 4)),
            entries.map { it.date },
        )
        assertEquals(
            listOf("Unknown constant", "Wrong case", "Empty value", "Known value"),
            entries.map { it.textContent },
        )
        assertEquals("From a newer version", entries.first().motivation)
    }

    @Test
    fun `every fixture entry passes the import validation`(): Unit = runBlocking {
        // DiaryViewModel imports by calling CreateUpdateDiaryEntryUseCase for every decoded entry.
        val repository = InMemoryDiaryEntryRepository()
        val createUpdate = CreateUpdateDiaryEntryUseCase(repository)
        val entries = import(fixture(BASIC)) + import(fixture(UNKNOWN_HAPPINESS))
        entries.forEach { createUpdate(it) }
        assertEquals(entries.sortedBy { it.date }, repository.entries.values.toList())
    }

    @Test
    fun `import rejects an entry whose relativePath is not 26 characters as current behavior`(): Unit = runBlocking {
        val entries = import(
            """[{"date":19450,"created":1680516900,"dayOfYear":93,"relativePath":"photo.jpg","happiness":"HAPPY","motivation":"","textContent":""}]"""
        )
        val repository = InMemoryDiaryEntryRepository()
        try {
            CreateUpdateDiaryEntryUseCase(repository)(entries.single())
            fail("entry with relativePath photo.jpg was imported")
        } catch (expected: InvalidDiaryEntryException) {
        }
        assertEquals(0, repository.entries.size)
    }

    @Test
    fun `import ignores whitespace and key order`() {
        // Users may have pretty-printed or hand-edited an export; this is the example of DATA_CONTRACT.md.
        val documented = """
            [{"date": 19450, "created": 1680516900, "dayOfYear": 93, "relativePath": "OneShot_20230403101500.jpg", "happiness": "VERY_HAPPY", "motivation": "", "textContent": "A good day"}]
        """.trimIndent()
        val reordered = """
            [
              {
                "textContent": "A good day",
                "motivation": "",
                "happiness": "VERY_HAPPY",
                "relativePath": "OneShot_20230403101500.jpg",
                "dayOfYear": 93,
                "created": 1680516900,
                "date": 19450
              }
            ]
        """.trimIndent()
        val expected = listOf(
            DiaryEntry(LocalDate.of(2023, 4, 3), 1680516900L, 93, "OneShot_20230403101500.jpg", HappinessType.VERY_HAPPY, "", "A good day")
        )
        assertEquals(expected, import(documented))
        assertEquals(expected, import(reordered))
    }

    @Test
    fun `export of a single entry has exactly the documented keys in order`() {
        val entry = DiaryEntry(LocalDate.of(2023, 4, 3), 1680516900L, 93, "OneShot_20230403101500.jpg", HappinessType.VERY_HAPPY, "", "A good day")
        assertEquals(
            """[{"date":19450,"created":1680516900,"dayOfYear":93,"relativePath":"OneShot_20230403101500.jpg","happiness":"VERY_HAPPY","motivation":"","textContent":"A good day"}]""",
            Json.encodeToString(listOf(entry)),
        )
    }

    @Test
    fun `unknown keys make the current importer fail`() {
        // Documents current behavior: the default Json instance rejects unknown keys, so an export that
        // adds a field cannot be imported by v1.1.1. A future format change must account for this.
        try {
            import(
                """[{"version":2,"date":19450,"created":1680516900,"dayOfYear":93,"relativePath":"OneShot_20230403101500.jpg","happiness":"HAPPY","motivation":"","textContent":""}]"""
            )
            fail("unknown key was accepted")
        } catch (expected: SerializationException) {
        }
    }

    @Test
    fun `missing optional fields fall back to the entity defaults`() {
        val entries = Json.decodeFromString<List<DiaryEntry>>(
            """[{"date":19450,"created":1680516900,"dayOfYear":93,"relativePath":"OneShot_20230403101500.jpg"}]"""
        )
        assertEquals(
            listOf(DiaryEntry(LocalDate.of(2023, 4, 3), 1680516900L, 93, "OneShot_20230403101500.jpg", HappinessType.NOT_SPECIFIED, "", "")),
            entries,
        )
    }

    private fun assertBytes(expected: ByteArray, actual: ByteArray) {
        if (!expected.contentEquals(actual)) {
            // Show the text for a readable diff; the byte comparison below fails in any case.
            assertEquals(String(expected, Charsets.UTF_8), String(actual, Charsets.UTF_8))
        }
        assertArrayEquals(expected, actual)
    }

    private companion object {
        const val BASIC = "v1.1.1-basic.json"
        const val UNKNOWN_HAPPINESS = "v1.1.1-unknown-happiness.json"

        /** The content of [BASIC]: every happiness value, leap day, day 366, unicode, emoji, escapes, empty strings. */
        val basicEntries = listOf(
            DiaryEntry(
                LocalDate.of(2020, 2, 29), 1582965005L, 60, "OneShot_20200229083005.jpg",
                HappinessType.VERY_HAPPY, "Seize the day", "Leap day picnic",
            ),
            DiaryEntry(
                LocalDate.of(2023, 4, 3), 1680516900L, 93, "OneShot_20230403101500.jpg",
                HappinessType.HAPPY, "", "A good day",
            ),
            DiaryEntry(
                LocalDate.of(2023, 7, 14), 1689363959L, 195, "OneShot_20230714194559.jpg",
                HappinessType.NEUTRAL,
                "\u00dcn\u00efc\u00f6d\u00e9 \u2713",
                "Gr\u00fc\u00dfe aus K\u00f6ln. \u00c7a va? \u65e5\u672c\u8a9e\u306e\u30c6\u30ad\u30b9\u30c8 " +
                    "\ud83d\udcf7\ud83c\udf05\ud83d\udc68\u200d\ud83d\udc69\u200d\ud83d\udc67",
            ),
            DiaryEntry(
                LocalDate.of(2023, 11, 5), 1699228799L, 309, "OneShot_20231105235959.jpg",
                HappinessType.SAD, "", "Line one\nLine two\n\n\tindented line\nShe said \"hi\" and saved C:\\path\\file",
            ),
            DiaryEntry(
                LocalDate.of(2024, 1, 15), 1705276800L, 15, "OneShot_20240115000000.jpg",
                HappinessType.VERY_SAD, "", "",
            ),
            DiaryEntry(
                LocalDate.of(2024, 12, 31), 1735646400L, 366, "OneShot_20241231120000.jpg",
                HappinessType.NOT_SPECIFIED, "Last day", "50/50 & <tags> 'single' {braces} [brackets]",
            ),
        )
    }
}
