package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.flow.Flow

class GetDiaryEntriesUseCase(
    private val repository: DiaryEntryRepository
) {

    /**
     * UseCase to get diary entries with the possibility to filter for keywords and a certain happiness.
     *
     * @param keyword The query keyword for textContent in DiaryEntries.
     * @param happiness The query of happiness for happiness in DiaryEntries.
     * @return Flow of List of matching entries in diary.
     */
    operator fun invoke(keyword: String? = null, happiness: HappinessType? = null): Flow<List<DiaryEntry>> {

        // null save use of right overloaded function from repository
        keyword?.let { kw ->
            happiness?.let { h ->
                return repository.getDiaryEntries(kw, h)
            } ?: run {
                return repository.getDiaryEntries(kw)
            }
        } ?: run {
            happiness?.let { h ->
                return repository.getDiaryEntries(h)
            } ?: run {
                return repository.getDiaryEntries()
            }
        }
    }
}