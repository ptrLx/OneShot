package de.ptrlx.oneshot.feature_diary.presentation.diary

import android.app.Activity
import android.net.Uri
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import java.time.LocalDate

sealed class DiaryEvent {
    data class DeleteDiaryEntry(val diaryEntry: DiaryEntry) : DiaryEvent()
    data class FilterSearch(val keyword: String) : DiaryEvent()
    data class FilterHappinessType(val type: HappinessType) : DiaryEvent()
    data class SetImageBaseLocation(val activity: Activity, val newBaseLocation: Uri) : DiaryEvent()
    data class DiaryEditEntry(val entry: DiaryEntry) : DiaryEvent()
    data class CaptureUpdateEntryProperty(
        val text: String? = null,
        val happiness: HappinessType? = null
    ) : DiaryEvent()
    /**
     * A photo was picked from the gallery.
     *
     * @param date the day the photo belongs to; null means today. A past day can be filled from
     * the roll calendar, a day in the future never can.
     */
    data class ImageImport(val uri: Uri, val date: LocalDate? = null) : DiaryEvent()

    object WriteDBExport : DiaryEvent()
    object ReadDBImport : DiaryEvent()
    object CaptureImageAborted : DiaryEvent()
    object CaptureUpdateEntry : DiaryEvent()
    object SnackbarDismissed : DiaryEvent()
    object ImageSaved : DiaryEvent()
}
