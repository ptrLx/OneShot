package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class GetFlashbacksUseCase(
    private val repository: DiaryEntryRepository
) {

    /**
     * UseCase to get flashbacks of certain days in past.
     *
     * @param currentDate
     * @return Flow of List of diary entries that are considered as flashback:
     * yesterday, last very happy day, 7 days ago, 14 days ago, all entries that where captured on the same date in a past year
     */
    operator fun invoke(currentDate: LocalDate): Flow<List<DiaryEntry?>> {
        return combine(repository.getDiaryEntryBy(currentDate.minusDays(1)),
            repository.getLastVeryHappyDay(),
            repository.getDiaryEntryBy(currentDate.minusDays(7)),
            repository.getDiaryEntryBy(currentDate.minusDays(14)),
            repository.getDiaryEntriesSameDayOfYear(currentDate),
            transform = { a: DiaryEntry?, b: DiaryEntry?, c: DiaryEntry?, d: DiaryEntry?, e: List<DiaryEntry?> ->
                listOf(a, b, c, d) + e
                    .filter { entry -> entry?.date != currentDate }
            })
    }
}