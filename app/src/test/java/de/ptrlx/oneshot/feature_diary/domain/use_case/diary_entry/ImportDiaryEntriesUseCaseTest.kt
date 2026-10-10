package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.contract.InMemoryDiaryEntryRepository
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

/**
 * JSON import with invalid entries (#84): valid entries are imported, invalid ones are skipped and
 * counted, nothing is stored differently from a single create/update.
 */
class ImportDiaryEntriesUseCaseTest {

    private val repository = InMemoryDiaryEntryRepository()
    private val importEntries = ImportDiaryEntriesUseCase(CreateUpdateDiaryEntryUseCase(repository))

    private fun entry(date: LocalDate, relativePath: String = "OneShot_${date.toString().replace("-", "")}101500.jpg") =
        DiaryEntry(date, 1_680_516_900L, date.dayOfYear, relativePath, HappinessType.HAPPY, "", "text $date")

    private val day1 = LocalDate.of(2023, 4, 1)
    private val day2 = LocalDate.of(2023, 4, 2)
    private val day3 = LocalDate.of(2023, 4, 3)

    @Test
    fun `all valid entries are imported`(): Unit = runBlocking {
        val entries = listOf(entry(day1), entry(day2), entry(day3))

        assertEquals(ImportResult(imported = 3, skipped = 0), importEntries(entries))
        assertEquals(entries, repository.entries.values.toList())
    }

    @Test
    fun `an invalid entry is skipped and the following valid entries are still imported`(): Unit = runBlocking {
        val invalid = entry(day2, relativePath = "too_short.jpg")
        val entries = listOf(entry(day1), invalid, entry(day3))

        assertEquals(ImportResult(imported = 2, skipped = 1), importEntries(entries))
        assertEquals(listOf(entry(day1), entry(day3)), repository.entries.values.toList())
    }

    @Test
    fun `an invalid entry does not touch the existing entry of the same date`(): Unit = runBlocking {
        val existing = entry(day2)
        repository.insertDiaryEntry(existing)

        val result = importEntries(listOf(entry(day2, relativePath = "")))

        assertEquals(ImportResult(imported = 0, skipped = 1), result)
        assertEquals(mapOf(day2 to existing), repository.entries.toMap())
    }

    @Test
    fun `only invalid entries are all skipped`(): Unit = runBlocking {
        val entries = listOf(entry(day1, relativePath = ""), entry(day2, relativePath = "x".repeat(27)))

        assertEquals(ImportResult(imported = 0, skipped = 2), importEntries(entries))
        assertTrue(repository.entries.isEmpty())
    }

    @Test
    fun `an empty import imports nothing`(): Unit = runBlocking {
        assertEquals(ImportResult(imported = 0, skipped = 0), importEntries(emptyList()))
    }

    @Test
    fun `entries are stored in order so a later entry of the same date wins as before`(): Unit = runBlocking {
        val first = entry(day1).copy(textContent = "first")
        val second = entry(day1).copy(textContent = "second")

        assertEquals(ImportResult(imported = 2, skipped = 0), importEntries(listOf(first, second)))
        assertEquals(listOf(second), repository.entries.values.toList())
    }

    @Test
    fun `v1_1_1 export with an injected invalid entry imports every fixture entry unchanged`(): Unit = runBlocking {
        val fixture = javaClass.getResourceAsStream("/export/v1.1.1-basic.json")
            ?: throw AssertionError("fixture export/v1.1.1-basic.json missing")
        val valid = fixture.use { Json.decodeFromStream<List<DiaryEntry>>(it) }
        val invalid = entry(LocalDate.of(1999, 12, 31), relativePath = "IMG_0001.jpg")
        val withInvalid = valid.take(1) + invalid + valid.drop(1)

        assertEquals(ImportResult(imported = valid.size, skipped = 1), importEntries(withInvalid))
        assertEquals(valid.sortedBy { it.date }, repository.entries.values.toList())
    }

    @Test
    fun `storage errors are not swallowed`() {
        val failing = object : DiaryEntryRepository by InMemoryDiaryEntryRepository() {
            override suspend fun insertDiaryEntry(entry: DiaryEntry) {
                throw IOException("disk full")
            }
        }
        val useCase = ImportDiaryEntriesUseCase(CreateUpdateDiaryEntryUseCase(failing))

        try {
            runBlocking { useCase(listOf(entry(day1))) }
            fail("expected the storage error to propagate")
        } catch (e: IOException) {
            assertEquals("disk full", e.message)
        }
    }
}
