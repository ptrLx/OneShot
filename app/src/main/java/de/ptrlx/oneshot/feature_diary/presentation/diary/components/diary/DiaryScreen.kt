package de.ptrlx.oneshot.feature_diary.presentation.diary.components.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryListItem
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.ImageCard
import java.time.format.TextStyle
import java.util.Locale

/**
 * The diary, paged.
 *
 * Pages load as the grid is scrolled, so the whole diary is never held in memory. The month
 * headers arrive inside the paged stream rather than being worked out here, which is what keeps
 * them correct across page boundaries.
 */
@Composable
fun DiaryScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    val items = viewModel.diaryPages.collectAsLazyPagingItems()

    LazyVerticalGrid(
        modifier = modifier.padding(horizontal = 8.dp),
        columns = GridCells.Adaptive(minSize = 190.dp),
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
                is DiaryListItem.MonthHeader -> Text(
                    text = "${
                        item.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    } ${item.month.year}",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                is DiaryListItem.Photo -> Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ImageCard(
                        modifier = Modifier.clickable {
                            viewModel.onEvent(DiaryEvent.DiaryEditEntry(item.entry))
                        },
                        viewModel = viewModel,
                        entry = item.entry,
                        description = item.entry.date.toString()
                    )
                }

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
