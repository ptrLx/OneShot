package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository

/**
 * Facts about the diary as a whole, read on demand rather than kept in memory.
 */
class GetDiarySummaryUseCase(
    private val repository: DiaryEntryRepository
) {
    /** Every entry, once, for the JSON export. */
    suspend fun allEntriesOnce(): List<DiaryEntry> = repository.getAllEntriesOnce()
}
