package de.ptrlx.oneshot.feature_diary.presentation.diary.util

import android.content.res.Resources
import de.ptrlx.oneshot.R

enum class SnackbarCause {
    DELETE_ENTRY,
    SUCCESS,
    ERROR,
    IMPORT_SKIPPED_ENTRIES,
    NOTHING_TO_EXPORT,
    NO_FOLDER;

    fun msg(): Int {
        return when (this) {
            DELETE_ENTRY -> R.string.snackbar_msg_entry_deleted
            SUCCESS -> R.string.snackbar_msg_success
            ERROR -> R.string.snackbar_msg_error
            IMPORT_SKIPPED_ENTRIES -> R.string.snackbar_msg_error
            NOTHING_TO_EXPORT -> R.string.snackbar_msg_nothing_to_export
            NO_FOLDER -> R.string.msg_set_base_location
        }
    }

    fun actionLabel(): Int {
        return when (this) {
            DELETE_ENTRY -> R.string.snackbar_action_undo
            SUCCESS -> R.string.snackbar_action_dismiss
            ERROR -> R.string.snackbar_action_dismiss
            IMPORT_SKIPPED_ENTRIES -> R.string.snackbar_action_dismiss
            NOTHING_TO_EXPORT -> R.string.snackbar_action_dismiss
            NO_FOLDER -> R.string.snackbar_action_dismiss
        }
    }

    /**
     * Text of the snackbar.
     *
     * @param resources to resolve the strings.
     * @param imported number of imported entries, used by [IMPORT_SKIPPED_ENTRIES].
     * @param skipped number of skipped entries, used by [IMPORT_SKIPPED_ENTRIES].
     */
    fun message(resources: Resources, imported: Int = 0, skipped: Int = 0): String {
        return when (this) {
            IMPORT_SKIPPED_ENTRIES -> resources.getQuantityString(
                R.plurals.snackbar_msg_import_skipped_entries,
                skipped,
                imported,
                skipped
            )
            else -> resources.getString(msg())
        }
    }
}
