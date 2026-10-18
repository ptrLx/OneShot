package de.ptrlx.oneshot.feature_diary.presentation.diary


import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.content.res.Resources
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import androidx.paging.PagingData
import androidx.paging.cachedIn
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.use_case.DiaryUseCases
import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry.ImportResult
import de.ptrlx.oneshot.feature_diary.domain.util.DiaryFileManager
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import de.ptrlx.oneshot.feature_diary.domain.util.StatsType
import de.ptrlx.oneshot.feature_diary.domain.util.imageBaseLocationKey
import de.ptrlx.oneshot.feature_diary.presentation.diary.util.SnackbarCause
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.*

/**
 * How many happy days to fetch for the memories row.
 *
 * Over-fetched because today and anything already in the flashback row is filtered out, and three
 * survivors are needed.
 */
private const val HAPPIEST_CANDIDATES = 12

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryUseCases: DiaryUseCases,
) : ViewModel() {
    private val LOG_TAG = "DiaryViewModel"
    private val modeRW =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    var isSnackbarShowing: Boolean by mutableStateOf(false)

    /**
     * The current date.
     *
     * Everything on screen is derived from this: the roll, the flashbacks, which frame is "today"
     * and which day can still be captured. It used to be read once when the view model was built,
     * so an app left open past midnight kept showing yesterday as today. [refreshDate] re-reads it
     * and restarts everything that depends on it.
     */
    var today: LocalDate by mutableStateOf(LocalDate.now())
        private set

    /**
     * Whether the add/edit sheet is open.
     *
     * Material 3 shows a modal sheet by composing it, so a single flag replaces the former pair of
     * show/hide events plus the separate "new dialog" marker.
     */
    var isEntrySheetVisible: Boolean by mutableStateOf(false)
        private set

//todo
//    private var searchQuery: String by mutableStateOf("")
//    private var searchEntries: List<DiaryEntry> by mutableStateOf(emptyList())

    var fileManager: DiaryFileManager? by mutableStateOf(null)
        private set

    var currentImageAddEditEntry: DiaryEntry? by mutableStateOf(null)
        private set
    var currentImportDatabaseUri: Uri? by mutableStateOf(null)

    /**
     * The gallery, paged.
     *
     * `cachedIn` keeps the loaded pages across configuration changes and across navigating away
     * and back, so returning to the diary does not re-read from the top.
     */
    val diaryPages: Flow<PagingData<DiaryListItem>> =
        diaryUseCases.getDiaryPage().cachedIn(viewModelScope)

    /** Every entry date, newest first: enough for the dividers, strips and the rolls overview. */
    var entryDates: List<LocalDate> by mutableStateOf(emptyList())
        private set

    /** How many entries carry each happiness, counted in SQL. */
    var happinessCounts: Map<HappinessType, Int> by mutableStateOf(emptyMap())
        private set


    var flashbacks: List<DiaryEntry?> by mutableStateOf(emptyList())
        private set

    /** Candidates for the happy memories row, fetched happiest first and filtered below. */
    private var happiestEntries: List<DiaryEntry> by mutableStateOf(emptyList())

    /** The month the roll calendar is showing. A new month opens on the new roll. */
    var visibleMonth: YearMonth by mutableStateOf(YearMonth.now())
        private set
    var visibleMonthEntries: List<DiaryEntry> by mutableStateOf(emptyList())
        private set

    /**
     * The day the Home sheet is showing. Today unless another one-shot was tapped.
     */
    var selectedHomeDate: LocalDate by mutableStateOf(LocalDate.now())
        private set

    /**
     * The entry the Home sheet is showing, when it was opened from a specific one-shot.
     *
     * Carried rather than looked up, because a happy memory or a flashback can be older than the
     * loaded gallery window and would not be found in [entries].
     */
    var selectedHomeEntry: DiaryEntry? by mutableStateOf(null)
        private set

    /** Show a specific one-shot, whatever its age. */
    fun selectHomeEntry(entry: DiaryEntry) {
        selectedHomeDate = entry.date
        selectedHomeEntry = entry
    }

    /** Show a day by date; its entry, if any, is resolved from the loaded window. */
    fun selectHomeDate(date: LocalDate) {
        selectedHomeDate = date
        selectedHomeEntry = null
    }

    /**
     * Today's entry, or null while today's frame is still unexposed.
     *
     * Its own small query, because with a paged gallery there is no longer a loaded list to
     * search, and today has to be correct whether or not the diary has been scrolled.
     */
    var todayEntry: DiaryEntry? by mutableStateOf(null)
        private set

    /** The entries of [visibleMonth], keyed by day of month, for the roll calendar. */
    val visibleMonthByDay: Map<Int, DiaryEntry>
        get() = visibleMonthEntries.associateBy { it.date.dayOfMonth }

    /**
     * Up to three happy memories for Home.
     *
     * The happiest days that are not today and are not already in the flashback row below, so the
     * same photo never appears twice on one screen. Ordered by happiness and then by date, so the
     * selection is stable instead of reshuffling on every recomposition.
     */
    val happyMemories: List<DiaryEntry>
        get() {
            val alreadyShown = flashbacks.filterNotNull().map { it.date }.toSet()
            return happiestEntries
                .filter { it.date != today && it.date !in alreadyShown }
                .take(3)
        }

    var stats: StatsType by mutableStateOf(diaryUseCases.getStats(ZonedDateTime.now()))
        private set

    private var recentlyDeleteDiaryEntry: DiaryEntry? = null

    private var searchDiaryEntriesJob: Job? = null
    private var getImageBaseLocationJob: Job? = null
    private var getDiaryEntriesJob: Job? = null
    private var getFlashbacksJob: Job? = null
    private var getVisibleMonthJob: Job? = null
    private var getDatesJob: Job? = null
    private var getHappinessCountsJob: Job? = null
    private var getHappiestJob: Job? = null
    private var getTodayJob: Job? = null

    var snackbarCause: SnackbarCause = SnackbarCause.DELETE_ENTRY
        private set

    var currentIsNewEntry: Boolean = true
        private set

    private var lastImportResult: ImportResult? = null

    init {
        Log.d(LOG_TAG, "CREATED")
    }

    /**
     * Function to pass events to view model.
     *
     * @param event [DiaryEvent] can store additional data.
     */
    fun onEvent(event: DiaryEvent) {
        Log.d(LOG_TAG, event.toString())
        when (event) {
            is DiaryEvent.SetImageBaseLocation -> {
                val resolver = event.activity.contentResolver

                // Get permanent permissions
                resolver.takePersistableUriPermission(
                    event.newBaseLocation,
                    modeRW
                )

                // Remove others
                resolver.persistedUriPermissions.forEach { current: UriPermission ->
                    if (current.uri != event.newBaseLocation) {
                        resolver.releasePersistableUriPermission(current.uri, modeRW)
                    }
                }

                viewModelScope.launch {
                    diaryUseCases.setSettingsValue(
                        stringPreferencesKey(imageBaseLocationKey),
                        event.newBaseLocation.toString()
                    )
                }
            }
            is DiaryEvent.ImageSaved -> {
                showModalBottomSheet()
                Log.d(LOG_TAG, "Image saved")
            }
            is DiaryEvent.ImageImport -> {
                createNewImageDummyOrImport(
                    copyImageUri = event.uri,
                    targetDate = event.date ?: today
                )
                showModalBottomSheet()
            }
            is DiaryEvent.CaptureImageAborted -> {
                // cleanup created (but empty) file
                currentImageAddEditEntry?.let { entry ->
                    fileManager?.deleteImage(entry.relativePath)
                    currentImageAddEditEntry = null
                }
            }
            is DiaryEvent.CaptureUpdateEntryProperty -> {
                currentImageAddEditEntry?.let { entry ->
                    event.text?.let {
                        currentImageAddEditEntry =
                            entry.copy(textContent = event.text)
                    }
                    event.happiness?.let {
                        currentImageAddEditEntry =
                            entry.copy(happiness = event.happiness)
                    }
                }

            }
            is DiaryEvent.CaptureUpdateEntry -> {
                hideModalBottomSheet()
                currentImageAddEditEntry?.let {
                    viewModelScope.launch {
                        diaryUseCases.createUpdateDiaryEntry(it)
                    }
                }
            }
            is DiaryEvent.DiaryEditEntry -> {
                currentImageAddEditEntry = event.entry
                currentIsNewEntry = false
                showModalBottomSheet()
            }
            is DiaryEvent.FilterHappinessType -> {

            }
            is DiaryEvent.FilterSearch -> {
//todo
//                searchQuery = event.keyword
//                searchDiaryEntriesJob?.cancel()
//                searchDiaryEntriesJob = viewModelScope.launch {
//                    delay(500L)
//                    diaryUseCases.getDiaryEntries(keyword = event.keyword).collect {
//                        searchEntries = it
//                    }
//                }
            }
            is DiaryEvent.DeleteDiaryEntry -> {
                hideModalBottomSheet()
                viewModelScope.launch {
                    diaryUseCases.deleteDiaryEntry(event.diaryEntry)
                    recentlyDeleteDiaryEntry = event.diaryEntry
                }
                snackbarCause = SnackbarCause.DELETE_ENTRY
                isSnackbarShowing = true
            }
            is DiaryEvent.SnackbarDismissed -> {
                if (snackbarCause == SnackbarCause.DELETE_ENTRY) {
                    viewModelScope.launch {
                        diaryUseCases.createUpdateDiaryEntry(
                            recentlyDeleteDiaryEntry ?: return@launch
                        )
                        recentlyDeleteDiaryEntry = null
                    }
                }
//              else if (snackbarCause == SnackbarCause.SUCCESS) {
//                  nothing to do here
//              } else if (snackbarCause == SnackbarCause.ERROR) {
//                  nothing to do here
//              }
            }
            is DiaryEvent.WriteDBExport -> {
                // The export has to hold the whole diary, not the pages the gallery happens to
                // have loaded, and writing a file is disk work that used to run inline on the
                // main thread.
                // Every outcome reports something. An export that silently did nothing, which is
                // what an empty diary or a missing folder used to produce, looks like a dead
                // button.
                viewModelScope.launch {
                    val manager = fileManager
                    if (manager == null) {
                        showSnackbar(SnackbarCause.NO_FOLDER)
                        return@launch
                    }
                    val result = withContext(Dispatchers.IO) {
                        val all = diaryUseCases.getDiarySummary.allEntriesOnce()
                        if (all.isEmpty()) return@withContext SnackbarCause.NOTHING_TO_EXPORT
                        val currentDate = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
                            .format(System.currentTimeMillis())
                        val ok = manager.writeJSONExport("OneShot_DB_$currentDate.json", all)
                        if (ok) SnackbarCause.SUCCESS else SnackbarCause.ERROR
                    }
                    showSnackbar(result)
                }
            }
            is DiaryEvent.ReadDBImport -> {
                val uri = currentImportDatabaseUri
                currentImportDatabaseUri = null
                val importEntries = uri?.let { readImport(it) }
                if (importEntries.isNullOrEmpty()) {
                    showSnackbar(SnackbarCause.ERROR)
                } else {
                    Log.d(LOG_TAG, importEntries.toString())
                    viewModelScope.launch {
                        val result = try {
                            diaryUseCases.importDiaryEntries(importEntries)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.e(LOG_TAG, "Import failed", e)
                            null
                        }
                        lastImportResult = result
                        showSnackbar(
                            when {
                                result == null -> SnackbarCause.ERROR
                                result.skipped > 0 -> SnackbarCause.IMPORT_SKIPPED_ENTRIES
                                else -> SnackbarCause.SUCCESS
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Read the entries of a JSON export.
     *
     * @return the entries, or null if no folder is set or the file cannot be read or decoded.
     */
    private fun readImport(uri: Uri): List<DiaryEntry>? {
        val fileManager = fileManager ?: return null
        Log.d(LOG_TAG, "Importing entries from $uri")
        return try {
            fileManager.readJSONExport(uri)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Could not read import file", e)
            null
        }
    }

    private fun showSnackbar(cause: SnackbarCause) {
        snackbarCause = cause
        isSnackbarShowing = true
    }

    /**
     * Text of the snackbar for the current [snackbarCause].
     */
    fun snackbarMessage(resources: Resources): String =
        snackbarCause.message(
            resources,
            imported = lastImportResult?.imported ?: 0,
            skipped = lastImportResult?.skipped ?: 0
        )

    /**
     * Call this function to start the flow of data into view model.
     *
     * @param context
     */
    fun startGetEntriesJobs(context: Context) {
        if (getImageBaseLocationJob == null)
            getImageBaseLocationJob =
                diaryUseCases.getSettingsString(imageBaseLocationKey).onEach { uriString ->
                    Log.d(LOG_TAG, "New element received in GetImageBaseLocationJob")
                    uriString?.let {
                        fileManager = DiaryFileManager(Uri.parse(uriString), context)
                    }
                }.launchIn(viewModelScope)


        if (getTodayJob == null) startTodayJob()

        if (getDatesJob == null)
            getDatesJob = diaryUseCases.getDiarySummary.dates()
                .onEach { dates -> entryDates = dates }
                .launchIn(viewModelScope)

        if (getHappinessCountsJob == null)
            getHappinessCountsJob = diaryUseCases.getDiarySummary.happinessCounts()
                .onEach { counts -> happinessCounts = counts }
                .launchIn(viewModelScope)

        if (getHappiestJob == null)
            getHappiestJob = diaryUseCases.getDiarySummary.happiest(HAPPIEST_CANDIDATES)
                .onEach { happiest -> happiestEntries = happiest }
                .launchIn(viewModelScope)

        if (getFlashbacksJob == null) startFlashbacksJob()

        if (getVisibleMonthJob == null) showMonth(visibleMonth)
    }

    private fun startTodayJob() {
        getTodayJob?.cancel()
        getTodayJob = diaryUseCases.getDiarySummary.entryOn(today)
            .onEach { entry -> todayEntry = entry }
            .launchIn(viewModelScope)
    }

    private fun startFlashbacksJob() {
        getFlashbacksJob?.cancel()
        getFlashbacksJob = diaryUseCases.getFlashbacks(today)
            .onEach { entries ->
                Log.d(LOG_TAG, "New element received in GetFlashbacksJob")
                // "Last very happy day" can resolve to today, and today is not a flashback.
                flashbacks = entries.filter { entry -> entry == null || entry.date != today }
            }.launchIn(viewModelScope)
    }

    /**
     * Re-read the current date and restart everything derived from it.
     *
     * Called when the app comes back to the foreground, so a day that passed while it sat in the
     * background is picked up instead of the view model keeping the date it was created with.
     */
    fun refreshDate() {
        val current = LocalDate.now()
        if (current == today) return

        Log.d(LOG_TAG, "Date rolled over from $today to $current")
        val wasOnCurrentMonth = visibleMonth == YearMonth.from(today)
        if (selectedHomeDate == today) selectedHomeDate = current
        today = current
        stats = diaryUseCases.getStats(ZonedDateTime.now())
        startFlashbacksJob()
        startTodayJob()
        if (wasOnCurrentMonth) showMonth(YearMonth.from(current))
    }

    /** Jump the roll calendar back to the month that contains today. */
    fun showCurrentMonth() = showMonth(YearMonth.from(today))

    /**
     * Load the roll of [month] into [visibleMonthEntries].
     *
     * One roll is one calendar month and a frame's index is its day of month, so nothing has to be
     * stored for this: the roll is derived from the entry date alone.
     */
    fun showMonth(month: YearMonth) {
        visibleMonth = month
        getVisibleMonthJob?.cancel()
        getVisibleMonthJob = diaryUseCases.getCalenderMonth(month)
            .onEach { entries ->
                Log.d(LOG_TAG, "New element received in GetVisibleMonthJob for $month")
                visibleMonthEntries = entries
            }
            .launchIn(viewModelScope)
    }


    /**
     * Create a dummy file which can be used from camara app to store image.
     *
     * @return Uri that can be used to store an image
     */
    fun createNewImageDummy(date: LocalDate = today): Uri? {
        return createNewImageDummyOrImport(copyImageUri = null, targetDate = date)
    }

    /** A day can be filled while it is today or already past, but never before it happens. */
    fun canCaptureFor(date: LocalDate): Boolean = !date.isAfter(today)

    private fun createNewImageDummyOrImport(
        copyImageUri: Uri? = null,
        targetDate: LocalDate = today
    ): Uri? {
        if (!canCaptureFor(targetDate)) return null

        val timestamp = System.currentTimeMillis()
        val currentDate =
            SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(timestamp)
        val filename = "OneShot_$currentDate.jpg"

        val uri = copyImageUri?.let {
            fileManager?.copyImage(srcUri = copyImageUri, filename = filename)
        } ?: run {
            fileManager?.createNewImageDummy(filename)
        }

        uri?.let {
            // Replacing the photo of a day keeps everything that was written about it: only the
            // image and its timestamp change, so happiness, motivation and text survive a
            // re-shoot. The previous file stays on disk, because photos are never deleted
            // automatically (#11).
            currentIsNewEntry = true
            currentImageAddEditEntry = DiaryEntry(
                targetDate,
                timestamp / 1000L,
                targetDate.dayOfYear,
                filename
            )
            // The caller needs the file Uri synchronously to hand to the camera, so the day's
            // existing entry is merged in just afterwards: the sheet that reads it opens later.
            viewModelScope.launch {
                val existing = diaryUseCases.getDiarySummary.entryOn(targetDate).first()
                if (existing != null && currentImageAddEditEntry?.date == targetDate) {
                    currentIsNewEntry = false
                    currentImageAddEditEntry = existing.copy(
                        created = timestamp / 1000L,
                        relativePath = filename
                    )
                }
            }
        }

        return uri
    }

    private fun hideModalBottomSheet() {
        isEntrySheetVisible = false
    }

    private fun showModalBottomSheet() {
        isEntrySheetVisible = true
    }

    /** Called when the sheet is dismissed by swiping it away or tapping outside it. */
    fun onEntrySheetDismissed() {
        isEntrySheetVisible = false
    }
}
