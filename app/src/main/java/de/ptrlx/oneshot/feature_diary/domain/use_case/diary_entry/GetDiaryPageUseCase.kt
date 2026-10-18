package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry

import androidx.paging.PagingData
import androidx.paging.insertSeparators
import androidx.paging.map
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryListItem
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.YearMonth

/** How many entries one page holds. */
private const val PAGE_SIZE = 60

class GetDiaryPageUseCase(
    private val repository: DiaryEntryRepository
) {
    /**
     * The diary as a paged stream of gallery items, newest first.
     *
     * The month dividers are produced here rather than in the UI, with Paging's own
     * `insertSeparators`, so the screen never has to know which entries are loaded in order to
     * decide where a month begins. That is what keeps the dividers correct while pages stream in:
     * a header is emitted exactly when the month changes between two adjacent entries.
     */
    operator fun invoke(): Flow<PagingData<DiaryListItem>> =
        repository.pagedDiaryEntries(PAGE_SIZE)
            .map { paging ->
                paging
                    .map<DiaryEntry, DiaryListItem> { DiaryListItem.Photo(it) }
                    .insertSeparators { before, after ->
                        when {
                            // End of the list: nothing follows, so no header is needed.
                            after == null -> null
                            after !is DiaryListItem.Photo -> null
                            else -> {
                                val month = YearMonth.from(after.entry.date)
                                val previousMonth = (before as? DiaryListItem.Photo)
                                    ?.let { YearMonth.from(it.entry.date) }
                                if (previousMonth == month) null
                                else DiaryListItem.MonthHeader(month)
                            }
                        }
                    }
            }
}
