package de.ptrlx.oneshot.feature_diary.presentation.diary.util

enum class SnackbarCause {
    DELETE_ENTRY,
    SUCCESS,
    ERROR;

    fun msg(): String {
        return when (this) {
            DELETE_ENTRY -> "Entry deleted!"
            SUCCESS -> "Success!"
            ERROR -> "Error!"
        }
    }

    fun actionLabel(): String {
        return when (this) {
            DELETE_ENTRY -> "UNDO"
            SUCCESS -> "DISMISS"
            ERROR -> "DISMISS"
        }
    }
}
