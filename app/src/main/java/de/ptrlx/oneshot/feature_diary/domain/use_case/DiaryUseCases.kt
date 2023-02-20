package de.ptrlx.oneshot.feature_diary.domain.use_case

import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry.*
import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_settings.GetSettingsString
import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_settings.SetSettingsValue

/**
 * All use cases to interact with the diary data.
 *
 * @property createUpdateDiaryEntry Create or update a diary entry in data source.
 * @property deleteDiaryEntry delete a diary entry from data source.
 * @property getCalenderMonth Get all diary entries of a certain month.
 * @property getDiaryEntries  Get diary entries with the possibility to filter for keywords and a certain happiness.
 * @property getFlashbacks Get flashbacks of certain days in past.
 * @property getStats Get statistics about happiness.
 * @property setSettingsValue Set a value in the settings datastore.
 * @property getSettingsString Get a value from the settings datastore.
 */
data class DiaryUseCases(
    val createUpdateDiaryEntry: CreateUpdateDiaryEntryUseCase,
    val deleteDiaryEntry: DeleteDiaryEntryUseCase,
    val getCalenderMonth: GetCalenderMonthUseCase,
    val getDiaryEntries: GetDiaryEntriesUseCase,
    val getFlashbacks: GetFlashbacksUseCase,
    val getStats: GetStatsUseCase,
    val setSettingsValue: SetSettingsValue,
    val getSettingsString: GetSettingsString
)
