package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * A month drawn as a camera roll: the weeks are the strips and the weekdays are the columns.
 *
 * This is the one surface where a tap opens a single day, because a frame here is large enough to
 * aim at. It replaces both the old 28-day grid and a separate month card: the same composable
 * renders the current month on Mood and any other month in the roll detail.
 */
@Composable
fun MonthRollCalendar(
    yearMonth: YearMonth,
    entriesByDay: Map<Int, DiaryEntry>,
    today: LocalDate?,
    onDayClick: (DiaryEntry) -> Unit,
    onAddPhotoForDay: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val length = yearMonth.lengthOfMonth()
    // Monday is column 0, matching the weekday header below.
    val offset = yearMonth.atDay(1).dayOfWeek.value - 1
    val weeks = ((offset + length) + 6) / 7

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val locale = Locale.getDefault()
            for (i in 0..6) {
                val day = java.time.DayOfWeek.of(i + 1)
                Text(
                    text = day.getDisplayName(TextStyle.NARROW, locale),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == 6) scheme.primary else scheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        for (week in 0 until weeks) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                FilmBase(Modifier.matchParentSize())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (column in 0..6) {
                        val day = week * 7 + column - offset + 1
                        if (day < 1 || day > length) {
                            // Outside the month: bare film, so a roll is exactly one month.
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            Frame(
                                modifier = Modifier.weight(1f),
                                day = day,
                                entry = entriesByDay[day],
                                state = frameStateOf(day, yearMonth, entriesByDay.keys, today),
                                onClick = onDayClick,
                                onAddPhoto = { onAddPhotoForDay(yearMonth.atDay(day)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The film base and its perforation, drawn behind one week. */
@Composable
private fun FilmBase(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val sprocket = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
    Canvas(modifier = modifier) {
        drawRoundRect(
            color = base,
            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
        )
        val holeW = 5.dp.toPx()
        val holeH = 3.dp.toPx()
        val pitch = 12.dp.toPx()
        val holes = (size.width / pitch).toInt().coerceAtLeast(1)
        val start = (size.width - (holes - 1) * pitch - holeW) / 2f
        repeat(holes) { i ->
            val x = start + i * pitch
            drawRoundRect(
                color = sprocket,
                topLeft = Offset(x, 2.dp.toPx()),
                size = Size(holeW, holeH),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            drawRoundRect(
                color = sprocket,
                topLeft = Offset(x, size.height - 2.dp.toPx() - holeH),
                size = Size(holeW, holeH),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun Frame(
    modifier: Modifier,
    day: Int,
    entry: DiaryEntry?,
    state: FrameState,
    onClick: (DiaryEntry) -> Unit,
    onAddPhoto: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(3.dp)
    val background = when (state) {
        FrameState.EXPOSED -> scheme.secondaryContainer
        FrameState.TODAY -> scheme.primaryContainer
        FrameState.UNEXPOSED -> scheme.onSurfaceVariant.copy(alpha = 0.12f)
        FrameState.FUTURE -> scheme.onSurfaceVariant.copy(alpha = 0.05f)
    }
    val onBackground = when (state) {
        FrameState.EXPOSED -> scheme.onSecondaryContainer
        FrameState.TODAY -> scheme.onPrimaryContainer
        else -> scheme.onSurfaceVariant.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(background)
            .then(
                if (state == FrameState.TODAY)
                    Modifier.border(1.5.dp, scheme.primary, shape)
                else Modifier
            )
            .then(
                when {
                    entry != null -> Modifier.clickable { onClick(entry) }
                    state == FrameState.UNEXPOSED -> Modifier.clickable { onAddPhoto() }
                    else -> Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = day.toString(),
                fontSize = 9.sp,
                color = onBackground,
                fontWeight = if (state == FrameState.TODAY) FontWeight.Bold else FontWeight.Normal
            )
            when (state) {
                FrameState.EXPOSED -> HappinessFace(
                    happiness = entry?.happiness ?: HappinessType.NOT_SPECIFIED,
                    size = 18.dp
                )
                FrameState.UNEXPOSED -> HappinessFace(
                    happiness = HappinessType.NOT_SPECIFIED,
                    size = 16.dp,
                    placeholderTint = onBackground.copy(alpha = 0.5f)
                )
                else -> Unit
            }
        }
    }
}
