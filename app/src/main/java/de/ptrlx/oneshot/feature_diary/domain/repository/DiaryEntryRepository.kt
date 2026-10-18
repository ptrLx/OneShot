package de.ptrlx.oneshot.feature_diary.domain.repository

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface DiaryEntryRepository {
    fun getDiaryEntries(): Flow<List<DiaryEntry>>
    fun pagedDiaryEntries(pageSize: Int): Flow<PagingData<DiaryEntry>>
    fun getAllDates(): Flow<List<LocalDate>>
    suspend fun getAllEntriesOnce(): List<DiaryEntry>
    fun countByHappiness(): Flow<Map<HappinessType, Int>>
    fun getHappiestEntries(limit: Int): Flow<List<DiaryEntry>>
    fun getDiaryEntries(keyword: String): Flow<List<DiaryEntry>>
    fun getDiaryEntries(happiness: HappinessType): Flow<List<DiaryEntry>>
    fun getDiaryEntries(keyword: String, happiness: HappinessType): Flow<List<DiaryEntry>>
    fun getDiaryEntriesSameDayOfYear(date: LocalDate): Flow<List<DiaryEntry>>
    fun getDiaryEntryBy(date: LocalDate): Flow<DiaryEntry?>
    fun getDiaryEntriesBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<DiaryEntry>>
    fun getHappinessBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<HappinessType>>
    fun getLastVeryHappyDay(): Flow<DiaryEntry?>
    fun getCurrentStreakCount(date: LocalDate, onlyHappyDaysCount: Boolean = false): Flow<UInt>
    suspend fun insertDiaryEntry(entry: DiaryEntry)
    suspend fun deleteDiaryEntry(entry: DiaryEntry)
}