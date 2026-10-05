package de.ptrlx.oneshot.contract

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.TreeMap

/**
 * Repository fake keyed by the primary key `date`, inserting like the DAO's
 * `OnConflictStrategy.REPLACE`. Only the write path is implemented.
 */
class InMemoryDiaryEntryRepository : DiaryEntryRepository {
    val entries = TreeMap<LocalDate, DiaryEntry>()

    override suspend fun insertDiaryEntry(entry: DiaryEntry) {
        entries[entry.date] = entry
    }

    override suspend fun deleteDiaryEntry(entry: DiaryEntry) {
        entries.remove(entry.date)
    }

    override fun getDiaryEntries(): Flow<List<DiaryEntry>> = unsupported()
    override fun getDiaryEntries(keyword: String): Flow<List<DiaryEntry>> = unsupported()
    override fun getDiaryEntries(happiness: HappinessType): Flow<List<DiaryEntry>> = unsupported()
    override fun getDiaryEntries(keyword: String, happiness: HappinessType): Flow<List<DiaryEntry>> = unsupported()
    override fun getDiaryEntriesSameDayOfYear(date: LocalDate): Flow<List<DiaryEntry>> = unsupported()
    override fun getDiaryEntryBy(date: LocalDate): Flow<DiaryEntry?> = unsupported()
    override fun getDiaryEntriesBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DiaryEntry>> = unsupported()
    override fun getHappinessBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<HappinessType>> = unsupported()
    override fun getLastVeryHappyDay(): Flow<DiaryEntry?> = unsupported()
    override fun getCurrentStreakCount(date: LocalDate, onlyHappyDaysCount: Boolean): Flow<UInt> = unsupported()

    private fun unsupported(): Nothing = throw UnsupportedOperationException("not needed by the contract tests")
}
