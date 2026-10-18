package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth


class GetCalenderMonthUseCase(
    private val repository: DiaryEntryRepository
) {

    /**
     * UseCase to get all diary entries of a certain month.
     *
     * @param yearMonth
     * @return Flow of List of all diary entries created in this month.
     */
    operator fun invoke(yearMonth: YearMonth): Flow<List<DiaryEntry>> {
        // getDiaryEntriesBetween treats the end as exclusive (`date < :end`), so the first day of
        // the next month is passed: atEndOfMonth() dropped the last day of every month.
        return repository.getDiaryEntriesBetween(
            yearMonth.atDay(1),
            yearMonth.plusMonths(1).atDay(1)
        )
    }
}