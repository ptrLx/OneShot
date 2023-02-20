package de.ptrlx.oneshot.feature_diary.data.repository

import de.ptrlx.oneshot.feature_diary.data.data_source.DiaryEntryDao
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DiaryEntryImplementation(
    private val dao: DiaryEntryDao
) : DiaryEntryRepository {
    override fun getDiaryEntries(): Flow<List<DiaryEntry>> {
        return dao.getDiaryEntries()
    }

    override fun getDiaryEntries(keyword: String): Flow<List<DiaryEntry>> {
        return dao.getDiaryEntries(keyword)
    }

    override fun getDiaryEntries(happiness: HappinessType): Flow<List<DiaryEntry>> {
        return dao.getDiaryEntries(happiness)
    }

    override fun getDiaryEntries(
        keyword: String,
        happiness: HappinessType
    ): Flow<List<DiaryEntry>> {
        return dao.getDiaryEntries(keyword, happiness)
    }

    override fun getDiaryEntriesSameDayOfYear(date: LocalDate): Flow<List<DiaryEntry>> {
        return dao.getDiaryEntriesDateOfYear(date.dayOfYear)
    }

    override fun getDiaryEntryBy(date: LocalDate): Flow<DiaryEntry?> {
        return dao.getDiaryEntry(date)
    }

    override fun getDiaryEntriesBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DiaryEntry>> {
        return dao.getDiaryEntriesBetween(startDate, endDate)
    }

    override fun getHappinessBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<HappinessType>> {
        return dao.getHappinessBetween(startDate, endDate)
    }

    override fun getLastVeryHappyDay(): Flow<DiaryEntry?> {
        return dao.getLastVeryHappyDay()
    }

    override fun getCurrentStreakCount(date: LocalDate, onlyHappyDaysCount: Boolean): Flow<UInt> {
        val last1000 = dao.getDiaryEntriesBetween(date.minusDays(999), date.plusDays(1))
        return last1000.map { listOfEntries ->
            var counter = 0u
            var currentDate = date

            run breaking@{
                listOfEntries.forEachIndexed { i, entry ->
                    if (i >= listOfEntries.size - 1) {
                        return@breaking
                    } else if (((i == 0 && entry.date == date) || // first element can be today or tomorrow
                                (entry.date == currentDate.minusDays(1))) &&
                        (!onlyHappyDaysCount || entry.happiness == HappinessType.VERY_HAPPY || entry.happiness == HappinessType.HAPPY) // User has to be happy :)
                    ) {
                        currentDate = entry.date
                        counter++
                    } else {
                        return@breaking
                    }
                }
            }
            if (counter > 999u)
                999u
            else
                counter
        }
    }

    override suspend fun insertDiaryEntry(entry: DiaryEntry) {
        dao.insertDiaryEntry(entry)
    }

    override suspend fun deleteDiaryEntry(entry: DiaryEntry) {
        dao.deleteDiaryEntry(entry)
    }
}