package de.ptrlx.oneshot.contract

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.model.InvalidDiaryEntryException
import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry.CreateUpdateDiaryEntryUseCase
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDate

/**
 * Pins the `relativePath` rule of [CreateUpdateDiaryEntryUseCase] (docs/modernization/DATA_CONTRACT.md,
 * section 4): only names of exactly 26 characters, the length of `OneShot_yyyyMMddHHmmss.jpg`, are
 * stored. Capture, gallery import, undo of a delete and JSON import all go through this check.
 */
class CreateUpdateDiaryEntryUseCaseContractTest {

    private val repository = InMemoryDiaryEntryRepository()
    private val createUpdate = CreateUpdateDiaryEntryUseCase(repository)

    private fun entry(relativePath: String, date: LocalDate = LocalDate.of(2023, 4, 3)) =
        DiaryEntry(date, 1680516900L, date.dayOfYear, relativePath, HappinessType.HAPPY, "", "A good day")

    @Test
    fun `accepts image names of the pinned pattern`(): Unit = runBlocking {
        val accepted = entry("OneShot_20230403101500.jpg")
        createUpdate(accepted)
        assertEquals(listOf(accepted), repository.entries.values.toList())
    }

    @Test
    fun `rejects names that are not 26 characters long`(): Unit = runBlocking {
        listOf(
            "",
            "OneShot_2023040310150.jpg", // 25
            "OneShot_202304031015000.jpg", // 27
            "OneShot_20230403101500.jpeg",
            "IMG_20230403_101500.jpg",
        ).forEach { path ->
            try {
                createUpdate(entry(path))
                fail("relativePath '$path' (${path.length} characters) was accepted")
            } catch (expected: InvalidDiaryEntryException) {
            }
        }
        assertTrue(repository.entries.isEmpty())
    }

    @Test
    fun `checks the length only and not the pattern as current behavior`(): Unit = runBlocking {
        // Names written under a locale with non-ASCII digits (#68) and any other 26 character name pass.
        val arabicIndicDigits = "OneShot_٢٠٢٣٠٤٠٣١٠١٥٠٠.jpg"
        assertEquals(26, arabicIndicDigits.length)
        createUpdate(entry(arabicIndicDigits, LocalDate.of(2023, 4, 3)))
        createUpdate(entry("abcdefghijklmnopqrstuvwxyz", LocalDate.of(2023, 4, 4)))
        assertEquals(2, repository.entries.size)
    }
}
