package de.ptrlx.oneshot.feature_diary.domain.model

import java.time.YearMonth

/**
 * One row of the paged gallery.
 *
 * The month headers are part of the stream rather than something the screen works out from the
 * entries it happens to have loaded, so a divider can never be misplaced by a page boundary.
 */
sealed interface DiaryListItem {

    /** A day with a photo. */
    data class Photo(val entry: DiaryEntry) : DiaryListItem

    /** The start of a month, spanning the full width of the grid. */
    data class MonthHeader(val month: YearMonth) : DiaryListItem
}
