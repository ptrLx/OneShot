package de.ptrlx.oneshot.feature_diary.domain.util

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import kotlinx.coroutines.flow.Flow

/**
 * Defines statistics of recent activity of the user.
 *
 * @property lastDays28 Information about whether a diary entry was captured in the past 28 days (including current day).
 * @property streakCount Count of the continuous days where a entry was captured.
 * @property happinessThisWeek Happiness within current week.
 * @property happinessLastWeek Happiness within last week.
 * @property happinessThisMonth Happiness within current month.
 * @property happinessLastMonth Happiness within last month.
 */
data class StatsType(
    val lastDays28: Flow<List<DiaryEntry>>, // DE needed for on click of date
    val streakCount: Flow<UInt>,
    val happinessStreakCount: Flow<UInt>,
    val happinessThisWeek: Flow<List<HappinessType>>,
    val happinessLastWeek: Flow<List<HappinessType>>,
    val happinessThisMonth: Flow<List<HappinessType>>,
    val happinessLastMonth: Flow<List<HappinessType>>,
)
