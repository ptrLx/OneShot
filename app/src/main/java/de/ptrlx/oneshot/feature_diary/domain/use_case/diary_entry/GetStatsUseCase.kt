package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import de.ptrlx.oneshot.feature_diary.domain.util.StatsType
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters.*

class GetStatsUseCase(
    private val repository: DiaryEntryRepository
) {

    /**
     * UseCase to get statistics about happiness.
     *
     * @param now Current time from ZoneDataTime.
     * @return Flow of instance of FlashbackType witch contains the matching DiaryEntries.
     */
    operator fun invoke(now: ZonedDateTime): StatsType {
        val currentDate = now.toLocalDate()
        val currentWeekMonday = now.with(previousOrSame(DayOfWeek.MONDAY))
        val previousMonth = currentDate.minusMonths(1)

        return StatsType(
            lastDays28 = repository.getDiaryEntriesBetween(currentDate.minusDays(27), currentDate.plusDays(1)),
            streakCount = repository.getCurrentStreakCount(currentDate),
            happinessStreakCount = repository.getCurrentStreakCount(currentDate, onlyHappyDaysCount = true),
            happinessThisWeek = repository.getHappinessBetween(currentDate, currentWeekMonday.toLocalDate()),
            happinessLastWeek = repository.getHappinessBetween(
                currentWeekMonday.with(previous(DayOfWeek.MONDAY)).toLocalDate(),
                currentWeekMonday.toLocalDate().minusDays(1)
            ),
            happinessThisMonth = repository.getHappinessBetween(
                currentDate.withDayOfMonth(1),
                currentDate.with(lastDayOfMonth())
            ),
            happinessLastMonth = repository.getHappinessBetween(
                previousMonth.withDayOfMonth(1),
                previousMonth.with(lastDayOfMonth())
            )
        )
    }
}