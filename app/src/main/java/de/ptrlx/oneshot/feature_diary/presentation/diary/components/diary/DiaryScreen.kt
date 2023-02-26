package de.ptrlx.oneshot.feature_diary.presentation.diary.components.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.AddEditOrSetLocationButton
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.ImageCard

/**
 * Main Composable for diary screen.
 *
 * @param modifier
 * @param viewModel
 */
@Composable
fun DiaryScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    Column(
        modifier = modifier
            .padding(start = 8.dp, end = 8.dp)
    ) {
        DiaryTop()
        DiaryEntryList(
            modifier = Modifier.weight(1f),
            viewModel = viewModel
        )
        AddEditOrSetLocationButton(
            viewModel = viewModel,
            hideWhenCaptured = true
        )
    }
}

/**
 * Composable to display diary entries data from view model in a column.
 *
 * @param modifier
 * @param viewModel
 */
@Composable
fun DiaryEntryList(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxHeight(),
        columns = GridCells.Adaptive(minSize = 190.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(viewModel.entries.size) { i ->
            val entry = viewModel.entries[viewModel.entries.size - i - 1]
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ImageCard(
                    modifier = Modifier
                        .clickable {
                            viewModel.onEvent(DiaryEvent.DiaryEditEntry(entry))
                        },
                    viewModel = viewModel,
                    entry = entry,
                    description = entry.date.toString()
                )
            }
        }
    }
}
