package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

/** What a single frame of a roll shows. Decided only by comparing its date to today. */
enum class FrameState {
    /** An entry exists for that day. */
    EXPOSED,

    /** Today, still without an entry: the one frame that can be filled right now. */
    TODAY,

    /** A past day with no entry. Calm, never an error. */
    UNEXPOSED,

    /** A day later this month. Visible, but not a gap. */
    FUTURE
}

/**
 * The state of one day of [yearMonth].
 *
 * @param today null when the month is in the past, so no frame is today and none is still to come.
 */
fun frameStateOf(
    day: Int,
    yearMonth: YearMonth,
    exposedDays: Set<Int>,
    today: LocalDate?
): FrameState {
    if (day in exposedDays) return FrameState.EXPOSED
    if (today == null) return FrameState.UNEXPOSED
    val date = yearMonth.atDay(day)
    return when {
        date.isEqual(today) -> FrameState.TODAY
        date.isAfter(today) -> FrameState.FUTURE
        else -> FrameState.UNEXPOSED
    }
}

/**
 * A compact strip of film for one month: a progress bar, not a gallery.
 *
 * The cells are far too small to hold an image, so the whole strip is one target and opens the
 * roll. Individual days are only tappable in [MonthRollCalendar], where a frame is big enough
 * to aim at.
 */
@Composable
fun MonthStrip(
    yearMonth: YearMonth,
    exposedDays: Set<Int>,
    today: LocalDate?,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp
) {
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceVariant
    val sprocket = scheme.onSurfaceVariant.copy(alpha = 0.30f)
    val exposed = scheme.primary
    val todayOutline = scheme.primary
    val todayFill = scheme.primaryContainer
    val unexposed = scheme.onSurface.copy(alpha = 0.12f)
    val future = scheme.onSurface.copy(alpha = 0.05f)
    val days = yearMonth.lengthOfMonth()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val band = 6.dp.toPx()
        val pad = 5.dp.toPx()
        val gap = 1.5.dp.toPx()
        val radius = CornerRadius(4.dp.toPx(), 4.dp.toPx())

        drawRoundRect(color = base, cornerRadius = radius)

        // Sprocket holes along both edges, as regular as the perforation on real film.
        val holeW = 5.dp.toPx()
        val holeH = 3.dp.toPx()
        val pitch = 11.dp.toPx()
        val holes = ((size.width - 2 * pad) / pitch).toInt().coerceAtLeast(1)
        val holeStart = (size.width - (holes - 1) * pitch - holeW) / 2f
        repeat(holes) { i ->
            val x = holeStart + i * pitch
            drawRoundRect(
                color = sprocket,
                topLeft = Offset(x, band / 2f - holeH / 2f),
                size = Size(holeW, holeH),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
            drawRoundRect(
                color = sprocket,
                topLeft = Offset(x, size.height - band / 2f - holeH / 2f),
                size = Size(holeW, holeH),
                cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
            )
        }

        val cellTop = band
        val cellH = size.height - 2 * band
        val cellW = (size.width - 2 * pad - gap * (days - 1)) / days
        for (day in 1..days) {
            val x = pad + (day - 1) * (cellW + gap)
            when (frameStateOf(day, yearMonth, exposedDays, today)) {
                FrameState.EXPOSED -> drawCell(x, cellTop, cellW, cellH, exposed)
                FrameState.UNEXPOSED -> drawCell(x, cellTop, cellW, cellH, unexposed)
                FrameState.FUTURE -> drawCell(x, cellTop, cellW, cellH, future)
                FrameState.TODAY -> {
                    drawCell(x, cellTop, cellW, cellH, todayFill)
                    drawRoundRect(
                        color = todayOutline,
                        topLeft = Offset(x, cellTop),
                        size = Size(cellW, cellH),
                        cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCell(
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    color: Color
) = drawRoundRect(
    color = color,
    topLeft = Offset(x, y),
    size = Size(w, h),
    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
)
