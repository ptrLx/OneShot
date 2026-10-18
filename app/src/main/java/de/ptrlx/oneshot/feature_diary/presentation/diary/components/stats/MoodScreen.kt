package de.ptrlx.oneshot.feature_diary.presentation.diary.components.stats

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.HappinessFace
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.MonthHappinessGraph
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.MonthRollCalendar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Mood: the streaks, then the month as a roll.
 *
 * The roll calendar replaces both the old 28-day grid and a separate month card. A rolling
 * 28-day window always showed four weeks of history and a calendar month does not, so the month
 * is swipeable: the chevrons move between rolls and a new month opens on the new roll.
 */
@Composable
fun MoodScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    controller: NavController
) {
    var pendingDay by remember { mutableStateOf<LocalDate?>(null) }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            val day = pendingDay
            pendingDay = null
            if (uri != null && day != null) {
                viewModel.onEvent(DiaryEvent.ImageImport(uri, day))
            }
        }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StreakCards(viewModel)

        MonthSwitcher(
            month = viewModel.visibleMonth,
            exposed = viewModel.visibleMonthByDay.size,
            isCurrentMonth = viewModel.visibleMonth == YearMonth.from(viewModel.today),
            onPrevious = { viewModel.showMonth(viewModel.visibleMonth.minusMonths(1)) },
            onNext = { viewModel.showMonth(viewModel.visibleMonth.plusMonths(1)) },
            onToday = { viewModel.showCurrentMonth() }
        )

        MonthRollCalendar(
            yearMonth = viewModel.visibleMonth,
            entriesByDay = viewModel.visibleMonthByDay,
            today = viewModel.today.takeIf { YearMonth.from(it) == viewModel.visibleMonth },
            onDayClick = { viewModel.onEvent(DiaryEvent.DiaryEditEntry(it)) },
            onAddPhotoForDay = { day ->
                pendingDay = day
                galleryLauncher.launch("image/*")
            }
        )

        // Counting rated days only, so the heading cannot appear above a graph that has nothing
        // left to draw after the unrated ones are filtered out.
        val ratedThisMonth = viewModel.visibleMonthEntries
            .count { it.happiness != HappinessType.NOT_SPECIFIED }
        if (ratedThisMonth >= 2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.month_shape),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                MonthHappinessGraph(
                    yearMonth = viewModel.visibleMonth,
                    entries = viewModel.visibleMonthEntries
                )
            }
        }

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { controller.navigate("rolls") }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.all_rolls),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        AllTimeDistribution(viewModel)
    }
}

@Composable
private fun StreakCards(viewModel: DiaryViewModel) {
    val streak: UInt? by viewModel.stats.streakCount.collectAsState(initial = null)
    val happyStreak: UInt? by viewModel.stats.happinessStreakCount.collectAsState(initial = null)

    // Both streaks appear from two days, exactly as they did on the old Statistics screen.
    val showStreak = (streak ?: 0u) >= 2u
    val showHappyStreak = (happyStreak ?: 0u) >= 2u
    if (!showStreak && !showHappyStreak) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showStreak) {
            StreakCard(
                modifier = Modifier.weight(1f),
                value = streak?.toInt() ?: 0,
                textRes = R.string.streak_days,
                container = MaterialTheme.colorScheme.primaryContainer,
                onContainer = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (showHappyStreak) {
            StreakCard(
                modifier = Modifier.weight(1f),
                value = happyStreak?.toInt() ?: 0,
                textRes = R.string.streak_happy_days,
                container = MaterialTheme.colorScheme.secondaryContainer,
                onContainer = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun StreakCard(
    modifier: Modifier,
    value: Int,
    textRes: Int,
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = onContainer
            )
            Text(
                text = stringResource(textRes, value),
                style = MaterialTheme.typography.bodySmall,
                color = onContainer
            )
        }
    }
}

@Composable
private fun MonthSwitcher(
    month: YearMonth,
    exposed: Int,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.month_previous),
            modifier = Modifier
                .size(32.dp)
                .clickable { onPrevious() }
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${
                    month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                } ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.roll_frames_exposed, exposed, month.lengthOfMonth()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Only offered once it would actually do something, so it never sits there inert.
            AnimatedVisibility(visible = !isCurrentMonth) {
                TextButton(
                    onClick = onToday,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.month_today),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = stringResource(R.string.month_next),
            modifier = Modifier
                .size(32.dp)
                .clickable { onNext() }
        )
    }
}

/** Fills the long-standing `//todo other stats`: how every rated day felt, all time. */
@Composable
private fun AllTimeDistribution(viewModel: DiaryViewModel) {
    val counts = HappinessType.values()
        .filter { it != HappinessType.NOT_SPECIFIED }
        .map { type -> type to (viewModel.happinessCounts[type] ?: 0) }
    if (counts.all { it.second == 0 }) return

    Column(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.all_time),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            counts.forEach { (type, count) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HappinessFace(happiness = type, size = 32.dp)
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
