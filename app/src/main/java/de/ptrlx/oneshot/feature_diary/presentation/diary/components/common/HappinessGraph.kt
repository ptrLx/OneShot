package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import java.time.YearMonth

/**
 * How the month felt, as one smoothed curve.
 *
 * Only days that have an entry carry a value, so the curve runs between the days actually
 * recorded rather than pretending a missed day was neutral. When the month is sparse the
 * recorded days are marked, because a curve drawn through three points over four weeks would
 * otherwise imply readings that were never taken. When the month is dense the marks are dropped
 * and the line is left to speak for itself.
 */
@Composable
fun MonthHappinessGraph(
    yearMonth: YearMonth,
    entries: List<DiaryEntry>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val scheme = MaterialTheme.colorScheme
    val line = scheme.primary
    val fillTop = scheme.primary.copy(alpha = 0.35f)
    val grid = scheme.onSurfaceVariant.copy(alpha = 0.18f)
    val days = yearMonth.lengthOfMonth()

    // A day that was never rated is not a low mood: NOT_SPECIFIED has a progress of 0, so
    // including it would drag the curve to the floor on every unrated day.
    val points = entries
        .filter { YearMonth.from(it.date) == yearMonth }
        .filter { it.happiness != HappinessType.NOT_SPECIFIED }
        .sortedBy { it.date }
        .map { (it.date.dayOfMonth - 1) / (days - 1).toFloat() to it.happiness.progress() }

    // Without at least two readings there is no shape to draw.
    if (points.size < 2) return

    // Few enough points that the eye would invent readings between them: mark the real ones.
    val markPoints = points.size <= days / 3

    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650),
        label = "graph"
    )

    Row(modifier = modifier.fillMaxWidth()) {
        HappinessScale(height = height)
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(height)
        ) {
        val padding = 10.dp.toPx()
        val w = size.width - 2 * padding
        val h = size.height - 2 * padding

        // One line per reference level, so a point's height can be read off rather than guessed.
        SCALE_LEVELS.forEach { level ->
            val y = padding + (1f - level.progress()) * h
            drawLine(
                color = grid,
                start = Offset(padding, y),
                end = Offset(padding + w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val coords = points.map { (x, value) ->
            Offset(padding + x * w, padding + (1f - value) * h)
        }
        val path = smoothPath(coords)

        val area = Path().apply {
            addPath(path)
            lineTo(coords.last().x, padding + h)
            lineTo(coords.first().x, padding + h)
            close()
        }
        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                listOf(fillTop, fillTop.copy(alpha = 0f)),
                startY = padding,
                endY = padding + h
            ),
            alpha = reveal
        )
        drawPath(
            path = path,
            color = line,
            style = Stroke(width = 2.5.dp.toPx()),
            alpha = reveal
        )

            if (markPoints) {
                coords.forEach { point ->
                    drawCircle(color = line, radius = 3.5.dp.toPx(), center = point, alpha = reveal)
                }
            }
        }
    }
}

/** The happiness values the gridlines sit on, highest first. */
private val SCALE_LEVELS = listOf(
    HappinessType.VERY_HAPPY,
    HappinessType.NEUTRAL,
    HappinessType.VERY_SAD
)

/**
 * The faces down the left edge, each at the height its own value sits on.
 *
 * Placed by the same progress() the curve uses, so the labels cannot drift away from the lines
 * they belong to.
 */
@Composable
private fun HappinessScale(height: Dp) {
    val face = 16.dp
    val padding = 10.dp
    Box(
        modifier = Modifier
            .width(22.dp)
            .height(height)
    ) {
        SCALE_LEVELS.forEach { level ->
            val y = padding + (height - padding * 2) * (1f - level.progress()) - face / 2
            HappinessFace(
                happiness = level,
                modifier = Modifier.offset(y = y),
                size = face
            )
        }
    }
}

/**
 * A path through [points] using Catmull-Rom style control points.
 *
 * Each control point is derived from the neighbours of a segment, which keeps the curve passing
 * exactly through every reading instead of rounding the peaks away.
 */
private fun DrawScope.smoothPath(points: List<Offset>): Path = Path().apply {
    moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
        cubicTo(
            p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
            p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
            p2.x, p2.y
        )
    }
}
