package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import de.ptrlx.oneshot.R
import java.time.LocalDate
import java.time.LocalTime

/** The part of the day, used for both the greeting and the little mark beside it. */
enum class DayPart(@StringRes val greeting: Int, val emoji: String) {
    // Emoji from Emoji 1.0 only: Android 10 and 11 have no glyph for anything newer and would
    // draw a tofu box, which is the same trap HappinessType.emoticon() fell into (#77).
    MORNING(R.string.greeting_morning, "🌅"),
    AFTERNOON(R.string.greeting_afternoon, "☀️"),
    EVENING(R.string.greeting_evening, "🌙");

    companion object {
        fun of(time: LocalTime = LocalTime.now()): DayPart = when (time.hour) {
            in 5..11 -> MORNING
            in 12..17 -> AFTERNOON
            else -> EVENING
        }
    }
}

/**
 * The line shown under the greeting.
 *
 * The original code carried a `//todo motivation => fetch motivation api`, which cannot ship:
 * the app has no INTERNET permission and that is a product promise, not an oversight. The lines
 * are therefore bundled and translatable, and the one for a given day is chosen from the date
 * itself, so it is stable all day instead of changing on every recomposition.
 */
@Composable
fun motivationFor(date: LocalDate): String {
    // For now the app shows its own slogan rather than rotating through the set below. The
    // rotation is kept and tested by dailyLineFor, ready to be switched on.
    return stringResource(R.string.app_slogan)
}

/**
 * The bundled line for a given day.
 *
 * Chosen from the date itself so it is stable for the whole day instead of changing on every
 * recomposition.
 */
@Composable
fun dailyLineFor(date: LocalDate): String {
    val lines = stringArrayResource(R.array.daily_motivations)
    if (lines.isEmpty()) return ""
    val index = Math.floorMod(date.toEpochDay(), lines.size.toLong()).toInt()
    return lines[index]
}
