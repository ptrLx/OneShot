package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository

class DeleteDiaryEntryUseCase(
    private val repository: DiaryEntryRepository
) {

    /**
     * UseCase to delete a diary entry from data source.
     *
     * @param entry the entry that should be deleted.
     */
    suspend operator fun invoke(entry: DiaryEntry) {
        repository.deleteDiaryEntry(entry)
    }
}