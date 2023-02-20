package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.model.InvalidDiaryEntryException
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository


class CreateUpdateDiaryEntryUseCase(
    private val repository: DiaryEntryRepository
) {

    /**
     * UseCase to create or a diary entry in data source.
     *
     * @param entry entry that should be stored.
     */
    @Throws(InvalidDiaryEntryException::class)
    suspend operator fun invoke(entry: DiaryEntry) {
        if (entry.relativePath.length != 26) {
            throw InvalidDiaryEntryException("No valid image path provided.")
        }

        repository.insertDiaryEntry(entry)
    }
}