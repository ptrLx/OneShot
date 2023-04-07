package de.ptrlx.oneshot.feature_diary.presentation.diary.util

import de.ptrlx.oneshot.R

enum class SnackbarCause {
    DELETE_ENTRY,
    SUCCESS,
    ERROR;

    fun msg(): Int {
        return when (this) {
            DELETE_ENTRY -> R.string.snackbar_msg_entry_deleted
            SUCCESS -> R.string.snackbar_msg_success
            ERROR -> R.string.snackbar_msg_error
        }
    }

    fun actionLabel(): Int {
        return when (this) {
            DELETE_ENTRY -> R.string.snackbar_action_undo
            SUCCESS -> R.string.snackbar_action_dismiss
            ERROR -> R.string.snackbar_action_dismiss
        }
    }
}
