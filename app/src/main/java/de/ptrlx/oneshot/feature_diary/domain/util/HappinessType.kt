package de.ptrlx.oneshot.feature_diary.domain.util

/**
 * Defines a range of happiness-values from very happy to very sad or not specified.
 */
enum class HappinessType {
    VERY_HAPPY, HAPPY, NEUTRAL, SAD, VERY_SAD, NOT_SPECIFIED;

    /**
     * Presentation only: the persisted value is the enum [name], never this string.
     *
     * Keep to emoji that the oldest supported system font has. minSdk 29 is Android 10, which ships
     * Emoji 12, so anything newer renders as an empty box there: U+1FAE5 (Emoji 14) left
     * [NOT_SPECIFIED] blank on Android 10 and 11.
     */
    //todo allow user specified emoticons
    fun emoticon(): String {
        return when (this) {
            VERY_HAPPY -> "\uD83D\uDE0A"
            HAPPY -> "\uD83D\uDE04"
            NEUTRAL -> "\uD83D\uDE10"
            SAD -> "\uD83D\uDE41"
            VERY_SAD -> "\uD83D\uDE22"
            NOT_SPECIFIED -> "\uD83D\uDE36"
        }
    }

//    fun describe(): String {
//        return when (this) {
//            VERY_HAPPY -> "very happy"
//            HAPPY -> "happy"
//            NEUTRAL -> "neutral"
//            SAD -> "sad"
//            VERY_SAD -> "very sad"
//            NOT_SPECIFIED -> ""
//        }
//    }

    fun progress(): Float {
        return when (this) {
            VERY_HAPPY -> 1f
            HAPPY -> 0.72f
            NEUTRAL -> 0.5f
            SAD -> 0.28f
            VERY_SAD -> 0.05f
            NOT_SPECIFIED -> 0f
        }
    }
}
