package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * The whole-diary facts that the screens need without needing the entries themselves.
 *
 * Keeping these separate is what lets the gallery page: the month dividers, the roll strips, the
 * rolls overview and the all-time tally are answered from dates and counts, so none of them
 * forces the full table into memory.
 */
class GetDiarySummaryUseCase(
    private val repository: DiaryEntryRepository
) {
    /** Every entry date, newest first. Eight bytes a row. */
    fun dates(): Flow<List<LocalDate>> = repository.getAllDates()

    /** How many entries carry each happiness, counted in SQL. */
    fun happinessCounts(): Flow<Map<HappinessType, Int>> = repository.countByHappiness()

    /** Every entry, once, for the JSON export. */
    suspend fun allEntriesOnce(): List<DiaryEntry> = repository.getAllEntriesOnce()

    /** The entry of one day, or null. */
    fun entryOn(date: LocalDate): Flow<DiaryEntry?> = repository.getDiaryEntryBy(date)

    /** The happiest days, happiest first. Over-fetch and filter, rather than scan everything. */
    fun happiest(limit: Int): Flow<List<DiaryEntry>> = repository.getHappiestEntries(limit)
}
