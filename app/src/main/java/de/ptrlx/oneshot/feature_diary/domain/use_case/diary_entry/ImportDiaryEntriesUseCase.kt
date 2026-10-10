package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.model.InvalidDiaryEntryException

/**
 * Result of a JSON import.
 *
 * @property imported number of entries written to the data source.
 * @property skipped number of entries rejected as invalid and not written.
 */
data class ImportResult(val imported: Int, val skipped: Int)

class ImportDiaryEntriesUseCase(
    private val createUpdateDiaryEntry: CreateUpdateDiaryEntryUseCase
) {

    /**
     * UseCase to import entries (e.g. from a JSON export) into the data source.
     *
     * Every entry is stored exactly like a single [CreateUpdateDiaryEntryUseCase] call would store it,
     * in the given order. Entries rejected by its validation are skipped and counted instead of
     * aborting the import, so one invalid entry does not prevent the valid ones from being imported.
     *
     * @param entries entries that should be stored.
     * @return how many entries were imported and how many were skipped.
     */
    suspend operator fun invoke(entries: List<DiaryEntry>): ImportResult {
        var imported = 0
        var skipped = 0
        entries.forEach { entry ->
            try {
                createUpdateDiaryEntry(entry)
                imported++
            } catch (e: InvalidDiaryEntryException) {
                skipped++
            }
        }
        return ImportResult(imported, skipped)
    }
}
