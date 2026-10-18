package de.ptrlx.oneshot.feature_diary.presentation.diary.components.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryListItem
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.EmptyDiary
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.MonthStrip
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.SquarePhoto
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.rollRoute
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/**
 * The diary: a gallery, newest first, paged.
 *
 * Pages load as the grid is scrolled, so the whole diary is never held in memory. The month
 * headers arrive inside the paged stream rather than being worked out here, which is what keeps
 * them correct across page boundaries. Their roll strips still show every day of the month,
 * because those come from the dates list rather than from the loaded page.
 */
@Composable
fun DiaryScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    controller: NavController
) {
    val items = viewModel.diaryPages.collectAsLazyPagingItems()
    val daysByMonth: Map<YearMonth, Set<Int>> = remember(viewModel.entryDates) {
        viewModel.entryDates.groupBy { YearMonth.from(it) }
            .mapValues { (_, dates) -> dates.map { it.dayOfMonth }.toSet() }
    }

    val isEmpty = items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading
    if (isEmpty) {
        EmptyDiary(modifier = modifier.padding(horizontal = 24.dp))
        return
    }

    LazyVerticalGrid(
        modifier = modifier.padding(horizontal = 16.dp),
        columns = GridCells.Adaptive(minSize = 150.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = items.itemCount,
            // A header spans the row, a photo is one cell. peek() does not trigger a load.
            span = { index ->
                if (items.peek(index) is DiaryListItem.MonthHeader) GridItemSpan(maxLineSpan)
                else GridItemSpan(1)
            },
            key = { index ->
                when (val item = items.peek(index)) {
                    is DiaryListItem.MonthHeader -> "header-${item.month}"
                    is DiaryListItem.Photo -> "photo-${item.entry.date}"
                    null -> "placeholder-$index"
                }
            }
        ) { index ->
            when (val item = items[index]) {
                is DiaryListItem.MonthHeader -> MonthDivider(
                    month = item.month,
                    exposedDays = daysByMonth[item.month].orEmpty(),
                    today = viewModel.today.takeIf { YearMonth.from(it) == item.month },
                    onClick = {
                        viewModel.showMonth(item.month)
                        controller.navigate(rollRoute(item.month))
                    }
                )

                is DiaryListItem.Photo -> SquarePhoto(
                    model = viewModel.fileManager?.resolveUri(item.entry.relativePath),
                    contentDescription = item.entry.date.toString(),
                    label = item.entry.date
                        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                    happiness = item.entry.happiness,
                    onClick = { viewModel.onEvent(DiaryEvent.DiaryEditEntry(item.entry)) }
                )

                null -> Box(modifier = Modifier.fillMaxWidth())
            }
        }

        if (items.loadState.append is LoadState.Loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun MonthDivider(
    month: YearMonth,
    exposedDays: Set<Int>,
    today: LocalDate?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "${
                    month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                } ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.roll_frames_of,
                    exposedDays.size,
                    month.lengthOfMonth()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MonthStrip(
            yearMonth = month,
            exposedDays = exposedDays,
            today = today,
            height = 22.dp
        )
    }
}
