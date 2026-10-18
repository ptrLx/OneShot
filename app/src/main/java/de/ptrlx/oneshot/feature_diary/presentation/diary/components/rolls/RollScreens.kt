package de.ptrlx.oneshot.feature_diary.presentation.diary.components.rolls

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.MonthRollCalendar
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.SquarePhoto
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.MonthStrip
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.rollRoute
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private fun YearMonth.label(): String =
    "${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $year"

@Composable
private fun BackBar(title: String, subtitle: String?, controller: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.ArrowBack,
            contentDescription = stringResource(R.string.back),
            modifier = Modifier
                .size(28.dp)
                .clickable { controller.popBackStack() }
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * One roll, opened: the same calendar view as Mood, for an arbitrary month.
 *
 * This is one of only two places where a tap opens a single day, because a frame here is large
 * enough to aim at.
 */
@Composable
fun RollDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    controller: NavController,
    month: YearMonth
) {
    LaunchedEffect(month) { viewModel.showMonth(month) }

    var pendingDay by remember { mutableStateOf<LocalDate?>(null) }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val day = pendingDay
            pendingDay = null
            if (uri != null && day != null) {
                viewModel.onEvent(DiaryEvent.ImageImport(uri, day))
            }
        }

    val photos = viewModel.visibleMonthEntries.sortedByDescending { it.date }

    LazyVerticalGrid(
        modifier = modifier.padding(horizontal = 16.dp),
        columns = GridCells.Adaptive(minSize = 150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BackBar(
                    title = month.label(),
                    subtitle = stringResource(
                        R.string.roll_frames_exposed,
                        viewModel.visibleMonthByDay.size,
                        month.lengthOfMonth()
                    ),
                    controller = controller
                )
                MonthRollCalendar(
                    yearMonth = month,
                    entriesByDay = viewModel.visibleMonthByDay,
                    today = viewModel.today.takeIf { YearMonth.from(it) == month },
                    onDayClick = { viewModel.onEvent(DiaryEvent.DiaryEditEntry(it)) },
                    onAddPhotoForDay = { day ->
                        pendingDay = day
                        galleryLauncher.launch("image/*")
                    }
                )
                if (photos.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.roll_photos),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        items(photos.size) { i ->
            val entry = photos[i]
            SquarePhoto(
                model = viewModel.fileManager?.resolveUri(entry.relativePath),
                contentDescription = entry.date.toString(),
                label = entry.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                happiness = entry.happiness,
                onClick = { viewModel.onEvent(DiaryEvent.DiaryEditEntry(entry)) }
            )
        }
    }
}

/**
 * Every roll, one collapsed row each, so a year fits on a screen.
 *
 * A month with no frames is simply an empty row, which means a gap in the archive needs no
 * special case and is never silently skipped.
 */
@Composable
fun RollsOverviewScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    controller: NavController
) {
    val byMonth = viewModel.entryDates.groupBy { YearMonth.from(it) }
    val current = YearMonth.from(viewModel.today)
    val earliest = byMonth.keys.minOrNull() ?: current
    val rows = rollRows(current, earliest, byMonth.keys)

    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        BackBar(
            title = stringResource(R.string.rolls),
            subtitle = stringResource(
                R.string.rolls_summary,
                byMonth.size,
                viewModel.entryDates.size
            ),
            controller = controller
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(rows.size) { i ->
                when (val row = rows[i]) {
                    is RollRowItem.Roll -> RollRow(
                        month = row.month,
                        exposedDays = byMonth[row.month].orEmpty()
                            .map { it.dayOfMonth }.toSet(),
                        today = viewModel.today.takeIf { YearMonth.from(it) == row.month },
                        isCurrent = row.month == current,
                        onClick = {
                            viewModel.showMonth(row.month)
                            controller.navigate(rollRoute(row.month))
                        }
                    )

                    is RollRowItem.Gap -> GapRow(from = row.from, to = row.to)
                }
            }
        }
    }
}

/** A row of the rolls list: either one roll, or a run of months that hold nothing. */
private sealed interface RollRowItem {
    data class Roll(val month: YearMonth) : RollRowItem
    data class Gap(val from: YearMonth, val to: YearMonth) : RollRowItem
}

/**
 * Build the list newest first, folding runs of empty months into a single line.
 *
 * A long break would otherwise push every real roll off the screen, but dropping those months
 * silently would claim one month followed the other. One quiet line says how long the gap was
 * without spending a row per month.
 */
private fun rollRows(
    current: YearMonth,
    earliest: YearMonth,
    filled: Set<YearMonth>
): List<RollRowItem> {
    val rows = mutableListOf<RollRowItem>()
    var month = current
    while (month >= earliest) {
        if (month in filled) {
            rows += RollRowItem.Roll(month)
            month = month.minusMonths(1)
        } else {
            val gapEnd = month
            while (month >= earliest && month !in filled) month = month.minusMonths(1)
            val gapStart = month.plusMonths(1)
            // A single empty month is still worth its own row; only a real run is folded.
            if (gapStart == gapEnd) rows += RollRowItem.Roll(gapEnd)
            else rows += RollRowItem.Gap(from = gapStart, to = gapEnd)
        }
    }
    return rows
}

@Composable
private fun GapRow(from: YearMonth, to: YearMonth) {
    val months = ChronoUnit.MONTHS.between(from, to).toInt() + 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.rolls_gap, from.label(), to.label(), months),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun RollRow(
    month: YearMonth,
    exposedDays: Set<Int>,
    today: java.time.LocalDate?,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = month.label(),
                modifier = Modifier.width(118.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )
            MonthStrip(
                yearMonth = month,
                exposedDays = exposedDays,
                today = today,
                modifier = Modifier.weight(1f),
                height = 26.dp
            )
            Text(
                text = "${exposedDays.size}/${month.lengthOfMonth()}",
                modifier = Modifier
                    .width(46.dp)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
