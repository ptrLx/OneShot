package de.ptrlx.oneshot.feature_diary.presentation.diary.components.stats

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier, viewModel: DiaryViewModel
) {
    Column(
        modifier = modifier
            .padding(top = 8.dp)
//            .verticalScroll(rememberScrollState()),
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StreakCount(
            modifier = Modifier.padding(8.dp),
            viewModel = viewModel
        )

        StreakCalendar(
            viewModel = viewModel
        )

        //todo other stats
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StreakCount(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    val streakCount: UInt? by viewModel.stats.streakCount.collectAsState(initial = null)
    val happinessStreakCount: UInt? by viewModel.stats.happinessStreakCount.collectAsState(initial = null)
    val visible = streakCount?.let { it >= 2u } ?: run { false }
    val happinessStreakVisible = happinessStreakCount?.let { it >= 2u } ?: run { false }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = scaleIn(),
        exit = ExitTransition.None
    ) {
        Text(
            text = "Streak of $streakCount days${
                if (happinessStreakVisible)
                    ",\nHappiness-Streak of $happinessStreakCount days"
                else ""
            }\uD83C\uDF89",
            //todo extract string
            style = MaterialTheme.typography.h5,
            color = MaterialTheme.colors.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StreakCalendar(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            modifier = Modifier.padding(8.dp),
            text = stringResource(
                R.string.last_28_days
            ),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold
        )

        LazyHorizontalGrid(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp)
                .height(304.dp),
            rows = GridCells.Fixed(4),
//            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(28) { i ->
                val matchingIndex = (28 - 7 * (i % 4) - (i / 4) - 1)
                HappinessCalendarEntry(
                    modifier = Modifier.padding(start = 4.dp),
                    viewModel = viewModel,
                    index = matchingIndex
                )
            }
        }
    }
}

@Composable
fun HappinessCalendarEntry(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    index: Int
) {
    val date = viewModel.last28Days[index].first
    val entry = viewModel.last28Days[index].second

    val background = entry?.let {
        MaterialTheme.colors.secondary
    } ?: run {
        MaterialTheme.colors.error
    }

    val onColor = entry?.let {
        MaterialTheme.colors.onSecondary
    } ?: run {
        MaterialTheme.colors.onError
    }

    val myModifier = (entry?.let {
        modifier.clickable {
            viewModel.onEvent(
                DiaryEvent.DiaryEditEntry(entry)
            )
        }
    } ?: run {
        modifier
    })
        .height(70.dp)
        .width(50.dp)

    Card(
        modifier = myModifier,
        backgroundColor = background
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = date,
                fontSize = 12.sp,
                color = onColor
            )
            Text(
                fontSize = 30.sp,
                color = onColor,
                text = (entry?.happiness ?: run { HappinessType.NOT_SPECIFIED }).emoticon()
            )
        }
    }
}
