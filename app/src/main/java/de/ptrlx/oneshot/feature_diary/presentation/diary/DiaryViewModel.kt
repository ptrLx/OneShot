package de.ptrlx.oneshot.feature_diary.presentation.diary


import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.use_case.DiaryUseCases
import de.ptrlx.oneshot.feature_diary.domain.util.DiaryFileManager
import de.ptrlx.oneshot.feature_diary.domain.util.StatsType
import de.ptrlx.oneshot.feature_diary.domain.util.imageBaseLocationKey
import de.ptrlx.oneshot.feature_diary.presentation.diary.util.SnackbarCause
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.util.*

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val diaryUseCases: DiaryUseCases,
) : ViewModel() {
    private val LOG_TAG = "DiaryViewModel"
    private val modeRW =
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    private val now = ZonedDateTime.now()

    var isSnackbarShowing: Boolean by mutableStateOf(false)

    var modalBottomSheetShowEvent: Boolean by mutableStateOf(false)
    var modalBottomSheetHideEvent: Boolean by mutableStateOf(false)

//todo
//    private var searchQuery: String by mutableStateOf("")
//    private var searchEntries: List<DiaryEntry> by mutableStateOf(emptyList())

    var fielManager: DiaryFileManager? by mutableStateOf(null)
        private set

    var currentImageAddEditEntry: DiaryEntry? by mutableStateOf(null)
        private set
    var currentImportDatabaseUri: Uri? by mutableStateOf(null)

    var newModalDialog: Boolean by mutableStateOf(false)

    var entries: List<DiaryEntry> by mutableStateOf(emptyList())
        private set
    var flashbacks: List<DiaryEntry?> by mutableStateOf(emptyList())
        private set
    var last28Days: Array<Pair<String, DiaryEntry?>> by mutableStateOf(generateLast28Days(now.toLocalDate()))
        private set

    var stats: StatsType = diaryUseCases.getStats(now)
        private set

    private var recentlyDeleteDiaryEntry: DiaryEntry? = null

    private var searchDiaryEntriesJob: Job? = null
    private var getImageBaseLocationJob: Job? = null
    private var getDiaryEntriesJob: Job? = null
    private var getFlashbacksJob: Job? = null
    private var getLast28DaysJob: Job? = null

    var snackbarCause: SnackbarCause = SnackbarCause.DELETE_ENTRY
        private set

    var currentIsCapture: Boolean = true
        private set

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
            is DiaryEvent.CaptureImageAborted -> {
                // cleanup created (but empty) file
                currentImageAddEditEntry?.let { entry ->
                    fielManager?.deleteImage(entry.relativePath)
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
                currentIsCapture = false
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
                } else if (snackbarCause == SnackbarCause.SUCCESS) {
                    // nothing to do here
                } else if (snackbarCause == SnackbarCause.SUCCESS) {
                    // nothing to do here
                }
            }
            is DiaryEvent.WriteExport -> {
                if (entries.isNotEmpty()) {
                    val timestamp = System.currentTimeMillis()
                    val currentDate =
                        SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(timestamp)
                    val filename = "OneShot_DB_$currentDate.json"
                    val success = fielManager?.writeJSONExport(filename, entries)
                    snackbarCause =
                        (if (success == true) SnackbarCause.SUCCESS else SnackbarCause.ERROR)
                    isSnackbarShowing = true
                }
            }
            is DiaryEvent.ReadImport -> {
                var success = false
                fielManager?.let { fileManager ->
                    currentImportDatabaseUri?.let { uri ->
                        Log.d(LOG_TAG, "Importing entries from $uri")
                        val importEntries = fileManager.readJSONExport(uri)
                        if (importEntries.isNotEmpty()) {
                            Log.d(LOG_TAG, importEntries.toString())
                            success = true
                            viewModelScope.launch {
                                importEntries.forEach {
                                    diaryUseCases.createUpdateDiaryEntry(it)
                                    currentImportDatabaseUri = null
                                    isSnackbarShowing = true
                                    snackbarCause =
                                        (if (success) SnackbarCause.SUCCESS else SnackbarCause.ERROR)
                                }
                            }
                        }
                    }
                }

                if (!success) {
                    currentImportDatabaseUri = null
                    isSnackbarShowing = true
                    snackbarCause = SnackbarCause.ERROR
                }
            }
        }
    }

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
                        fielManager = DiaryFileManager(Uri.parse(uriString), context)
                    }
                }.launchIn(viewModelScope)


        if (getDiaryEntriesJob == null)
            getDiaryEntriesJob =
                diaryUseCases.getDiaryEntries()
                    .onEach { entries ->
                        Log.d(LOG_TAG, "New element received in GetDiaryEntriesJob")
                        this.entries = entries
                    }
                    .launchIn(viewModelScope)

        if (getFlashbacksJob == null) {
            getFlashbacksJob = diaryUseCases.getFlashbacks(now.toLocalDate())
                .onEach { entries ->
                    Log.d(LOG_TAG, "New element received in GetFlashbacksJob")
                    flashbacks = entries
                }.launchIn(viewModelScope)
        }

        if (getLast28DaysJob == null) {
            getLast28DaysJob = stats.lastDays28.onEach { entries ->
                Log.d(LOG_TAG, "New element received in GetLast28DaysJob")
                val current = now.toLocalDate()
                val last28DaysTemp = generateLast28Days(current)
                entries.forEach { entry ->
                    val index = Period.between(entry.date, current).days
                    last28DaysTemp[index] = Pair(
                        last28DaysTemp[index].first,
                        entry
                    )
                }
                last28Days = last28DaysTemp
            }.launchIn(viewModelScope)
        }
    }


    /**
     * Create a dummy file which can be used from camara app to store image.
     *
     * @return Uri that can be used to store an image
     */
    fun createNewImageDummy(): Uri? {
        val localDate = LocalDate.now()
        val timestamp = System.currentTimeMillis()
        val currentDate =
            SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(timestamp)
        val filename = "OneShot_$currentDate.jpg"

        val uri = fielManager?.createNewImageDummy(filename)

        uri?.let {
            currentIsCapture = true
            currentImageAddEditEntry = DiaryEntry(
                localDate,
                timestamp / 1000L,
                localDate.dayOfYear,
                filename
            )
        }

        return uri
    }

    private fun generateLast28Days(current: LocalDate): Array<Pair<String, DiaryEntry?>> {
        return Array(28) { i ->
            current.minusDays(i.toLong()).let {
                Pair(
                    "${
                        String.format("%02d", it.monthValue)
                    }-${
                        String.format("%02d", it.dayOfMonth)
                    }",
                    null
                )
            }
        }
    }

    private fun hideModalBottomSheet() {
//        newModalDialog = false => will be set in main activity automatically
        modalBottomSheetHideEvent = true
        modalBottomSheetShowEvent = false
    }

    private fun showModalBottomSheet() {
        newModalDialog = true
        modalBottomSheetShowEvent = true
        modalBottomSheetHideEvent = false
    }
}