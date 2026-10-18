package de.ptrlx.oneshot.feature_diary.presentation.diary.components.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.AddEditEntryModalBottomSheetContent
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.CaptureBand
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.EmptyDiary
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.HappyMemories
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.MonthStrip
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.SquarePhoto
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.motivationFor
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.rollRoute
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Home: the shot itself lives in a sheet that is always within reach.
 *
 * The capture band sits in a standard bottom sheet that peeks above the navigation bar, so the
 * app opens on what there is to look back on while today stays one pull away. Tapping any other
 * one-shot swaps the sheet to that day; letting the sheet settle back down returns it to today,
 * so it is never left pointing at a day the rest of the screen is not about.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    controller: NavController
) {
    val context = LocalContext.current
    val currentMonth = YearMonth.from(viewModel.today)
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val selectedDate = viewModel.selectedHomeDate
    // Today has its own query; any other day was opened from a card that already carried its
    // entry, so nothing here depends on what the paged gallery happens to have loaded.
    val selectedEntry = viewModel.selectedHomeEntry ?: viewModel.todayEntry

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            viewModel.onEvent(
                if (saved) DiaryEvent.ImageSaved else DiaryEvent.CaptureImageAborted
            )
        }
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { viewModel.onEvent(DiaryEvent.ImageImport(it, selectedDate)) }
        }

    // Settling back to the peek height means the user is done with that day.
    LaunchedEffect(scaffoldState.bottomSheetState) {
        snapshotFlow { scaffoldState.bottomSheetState.currentValue }
            .collect { value ->
                if (value == SheetValue.PartiallyExpanded) {
                    viewModel.selectHomeDate(viewModel.today)
                    // Collapsing is also how the editor is dismissed here, since there is no
                    // scrim to tap outside of.
                    if (viewModel.isEntrySheetVisible) viewModel.onEntrySheetDismissed()
                }
            }
    }

    // Editing opens the sheet it already lives in rather than a second one on top.
    LaunchedEffect(viewModel.isEntrySheetVisible) {
        if (viewModel.isEntrySheetVisible) scaffoldState.bottomSheetState.expand()
        else scaffoldState.bottomSheetState.partialExpand()
    }

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetPeekHeight = 132.dp,
        // The sheet used to be the same colour as the page behind it, so at peek height there
        // was nothing to show it could be pulled. A tinted container plus a shadow gives it an
        // edge.
        sheetContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        sheetContentColor = MaterialTheme.colorScheme.onSurface,
        sheetShadowElevation = 8.dp,
        sheetDragHandle = { SheetHandle() },
        sheetContent = {
            if (viewModel.isEntrySheetVisible) {
                AddEditEntryModalBottomSheetContent(viewModel = viewModel)
            } else HomeSheet(
                viewModel = viewModel,
                date = selectedDate,
                entry = selectedEntry,
                onCapture = {
                    viewModel.createNewImageDummy(selectedDate)?.let { cameraLauncher.launch(it) }
                        ?: Toast.makeText(
                            context,
                            context.getString(R.string.msg_set_base_location),
                            Toast.LENGTH_LONG
                        ).show()
                },
                onImport = { galleryLauncher.launch("image/*") },
                onEdit = { viewModel.onEvent(DiaryEvent.DiaryEditEntry(it)) },
                onBackToToday = { viewModel.selectHomeDate(viewModel.today) }
            )
        }
    ) { innerPadding ->
        val openOnSheet: (DiaryEntry) -> Unit = { entry ->
            viewModel.selectHomeEntry(entry)
            scope.launch { scaffoldState.bottomSheetState.expand() }
        }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = motivationFor(viewModel.today),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MonthSection(
                viewModel = viewModel,
                month = currentMonth,
                onOpenRoll = { controller.navigate(rollRoute(currentMonth)) }
            )

            if (viewModel.entryDates.isEmpty()) {
                EmptyDiary()
            } else {
                HappyMemoriesSection(viewModel, openOnSheet)
                FlashbackRow(viewModel, openOnSheet)
            }

            Spacer(modifier = Modifier.size(8.dp))
        }
    }
}

/**
 * The grip at the top of the sheet.
 *
 * The Material default is a clickable handle whose ripple is a rectangle far larger than the bar
 * itself, which looked like a stray box appearing on touch. This one is decoration: the sheet is
 * dragged by its surface, so the handle needs no touch target of its own.
 */
@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}

/**
 * What the sheet shows for one day.
 *
 * At peek height only the summary line is visible; pulling it up reveals the capture band and
 * anything written that day.
 */
@Composable
private fun HomeSheet(
    viewModel: DiaryViewModel,
    date: LocalDate,
    entry: DiaryEntry?,
    onCapture: () -> Unit,
    onImport: () -> Unit,
    onEdit: (DiaryEntry) -> Unit,
    onBackToToday: () -> Unit
) {
    val isToday = date == viewModel.today
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isToday) stringResource(R.string.today)
                    else date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(
                        when {
                            entry != null -> R.string.sheet_captured
                            isToday -> R.string.sheet_not_captured
                            else -> R.string.sheet_empty_day
                        }
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!isToday) {
                TextButton(onClick = onBackToToday) {
                    Text(stringResource(R.string.sheet_back_to_today))
                }
            }
        }

        if (viewModel.fileManager != null) {
            CaptureBand(
                todayEntry = entry,
                todayModel = entry?.let { viewModel.fileManager?.resolveUri(it.relativePath) },
                onCapture = onCapture,
                onImport = onImport,
                onOpenToday = onEdit,
                captureLabel = if (isToday) R.string.btn_capture_today
                else R.string.btn_capture_that_day
            )
        }

        if (entry != null && entry.textContent.isNotBlank()) {
            Text(
                text = entry.textContent,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * This month's roll.
 *
 * The strip is one target: tapping anywhere on it opens the roll, even if the finger lands on a
 * single frame. A frame here is a few pixels wide, so only the calendar view opens a single day.
 */
@Composable
private fun MonthSection(
    viewModel: DiaryViewModel,
    month: YearMonth,
    onOpenRoll: () -> Unit
) {
    val exposed = viewModel.visibleMonthByDay.keys
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = month.month.getDisplayName(
                    java.time.format.TextStyle.FULL,
                    java.util.Locale.getDefault()
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(
                    R.string.roll_frames_of,
                    exposed.size,
                    month.lengthOfMonth()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MonthStrip(
            yearMonth = month,
            exposedDays = exposed,
            today = viewModel.today,
            modifier = Modifier.clickable { onOpenRoll() }
        )
        if (exposed.isEmpty()) {
            Text(
                text = stringResource(R.string.roll_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HappyMemoriesSection(viewModel: DiaryViewModel, onOpen: (DiaryEntry) -> Unit) {
    val memories = viewModel.happyMemories
    if (memories.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.happy_memories),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        HappyMemories(
            memories = memories,
            label = { it.describe() },
            model = { viewModel.fileManager?.resolveUri(it.relativePath) },
            onOpen = onOpen
        )
    }
}

/**
 * Flashbacks, shown as soon as one can be computed.
 *
 * They used to stay hidden until four entries existed, which left a brand new diary with an
 * entirely blank screen. With a real empty state above, there is no reason to wait.
 */
@Composable
private fun FlashbackRow(viewModel: DiaryViewModel, onOpen: (DiaryEntry) -> Unit) {
    val titles = listOf(
        stringResource(R.string.yesterday),
        stringResource(R.string.last_very_happy_day),
        stringResource(R.string.days_ago_7),
        stringResource(R.string.days_ago_14)
    )
    val shown = viewModel.happyMemories.map { it.date }.toSet()
    val flashbacks = viewModel.flashbacks
        .mapIndexed { i, entry -> (titles.getOrNull(i) ?: entry?.describe().orEmpty()) to entry }
        .mapNotNull { (title, entry) -> entry?.let { title to it } }
        .filter { (_, entry) -> entry.date !in shown && entry.date != viewModel.today }
        .distinctBy { (_, entry) -> entry.date }

    if (flashbacks.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.flashbacks),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(flashbacks.size) { i ->
                val (title, entry) = flashbacks[i]
                SquarePhoto(
                    model = viewModel.fileManager?.resolveUri(entry.relativePath),
                    contentDescription = title,
                    modifier = Modifier.width(132.dp),
                    label = title,
                    happiness = entry.happiness,
                    onClick = { onOpen(entry) }
                )
            }
        }
    }
}

/** A short, localized label for an entry. */
internal fun DiaryEntry.describe(): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
