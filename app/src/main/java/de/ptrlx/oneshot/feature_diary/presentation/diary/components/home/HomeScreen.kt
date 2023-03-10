package de.ptrlx.oneshot.feature_diary.presentation.diary.components.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.AddEditOrSetLocationButton
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.ImageCard

/**
 * Main Composable for home screen.
 *
 * @param modifier
 * @param viewModel
 * @param controller
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    controller: NavController
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        //todo pull to reload event => reset uri resolver storage; reset viewmodel.now; kill and relaunch jobs; display loading spinner
        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            HomeTop(
                Modifier.padding(start = 8.dp, end = 8.dp),
                controller = controller
            )

            Flashbacks(
                viewModel = viewModel
            )

            //todo fetch motivation api and display daily motivation -> Store in DiaryEntry.motivation
        }
        AddEditOrSetLocationButton(
            Modifier.padding(top = 16.dp, bottom = 8.dp, start = 8.dp, end = 8.dp),
            viewModel = viewModel
        )
    }
}

/**
 * Composable to display flashbacks data from view model in a row.
 *
 * @param modifier
 * @param viewModel
 */
@Composable
fun Flashbacks(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    val flashbackTitles =
        listOf("Yesterday", "Last very happy day", "7 days ago", "14 days ago")
    val stripedFlashbacks = (if (viewModel.flashbacks.size >= 4)
        (flashbackTitles.mapIndexed { i, title ->
            Pair(
                title,
                viewModel.flashbacks[i]
            )
        } + viewModel.flashbacks.subList(4, viewModel.flashbacks.size).map { entry ->
            Pair(
                "Day ${entry?.dayOfYear} of ${entry?.date?.year}",
                entry,
            )
        }).filter { it.second != null }
    else
        emptyList()) as List<Pair<String, DiaryEntry>> // This cast is not unchecked as .filter will remove null values

    AnimatedVisibility(visible = stripedFlashbacks.isNotEmpty()) {
        Column(modifier = modifier) {
            Text(
                modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                text = "Flashbacks",
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold
            )
            LazyRow(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {} // Spacing
                items(stripedFlashbacks.size) { i ->
                    val description = stripedFlashbacks[i].first
                    val entry = stripedFlashbacks[i].second
                    ImageCard(
                        modifier = Modifier
                            .clickable {
                                viewModel.onEvent(DiaryEvent.DiaryEditEntry(entry))
                            },
                        viewModel = viewModel,
                        entry = entry,
                        description = description
                    )

                }
                item {} // Spacing
            }
        }
    }
}

