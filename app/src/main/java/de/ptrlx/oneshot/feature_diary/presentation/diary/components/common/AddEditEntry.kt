package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun AddEntryButton(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) {
            if (it) {
                viewModel.onEvent(DiaryEvent.ImageSaved)
            } else {
                viewModel.onEvent(DiaryEvent.CaptureImageAborted)
            }
        }

    RoundedButton(
        modifier = modifier,
        text = "Capture today",
        onClick = {
            viewModel.createNewImageDummy()?.let {
                launcher.launch(it)
            } ?: run {
                val toast =
                    Toast.makeText(context, "Please set your base location path", Toast.LENGTH_LONG)
                toast.show()
            }
        }
    )
}

@Composable
fun AddEditEntryModalBottomSheetContent(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    val entry = viewModel.currentImageAddEditEntry

    Box(
        modifier
            .fillMaxWidth()
            .height(670.dp)
    ) {
        if (!viewModel.currentIsCapture)
            Icon(
                Icons.Default.Delete,
                contentDescription = "delete",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(32.dp)
                    .clickable {
                        entry?.let {
                            viewModel.onEvent(DiaryEvent.DeleteDiaryEntry(it))
                        }
                    })
        Icon(
            Icons.Default.Done,
            contentDescription = "done",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(32.dp)
                .clickable {
                    viewModel.onEvent(DiaryEvent.CaptureUpdateEntry)
                })
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_drag_handle_24),
                contentDescription = "drag"
            )

            Spacer(modifier = Modifier.size(18.dp))

            if (viewModel.newModalDialog && entry != null)
                AnimatedVisibility(visible = (viewModel.newModalDialog)) {
                    ImageCard(
                        modifier = Modifier.padding(16.dp),
                        viewModel = viewModel,
                        entry = entry,
                        description = entry.date.toString(),
                        expandOnClick = true
                    )
                }

            entry?.created?.let {
                Text(
                    text = Instant.ofEpochSecond(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime().toString(),
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(fontSize = 16.sp)
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                HappinessType.values().reversedArray().drop(1).forEach { happiness ->
                    Text(
                        happiness.emoticon(),
                        fontSize = 32.sp,
                        modifier = Modifier
                            .clickable {
                                viewModel.onEvent(DiaryEvent.CaptureUpdateEntryProperty(happiness = happiness))
                            })
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            val progress by animateFloatAsState(
                targetValue = entry?.happiness?.progress()
                    ?: run { HappinessType.NOT_SPECIFIED.progress() },
                animationSpec = tween(durationMillis = 250)
            )
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 35.dp)
            )

            //todo display entry.motivation

            TextField(
                value = entry?.textContent ?: run { "" },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                onValueChange = { text ->
                    viewModel.onEvent(DiaryEvent.CaptureUpdateEntryProperty(text = text))
                }
            )
        }
    }
}

@Composable
fun AddEditOrSetLocationButton(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    hideWhenCaptured: Boolean = false
) {
    var lastEntry: DiaryEntry? = null
    try {
      lastEntry = viewModel.entries.last()
    } catch (e: NoSuchElementException) {}

    if (viewModel.fileManager == null)
        SetLocation(modifier.padding(top = 4.dp), viewModel = viewModel)
    else if ((!hideWhenCaptured) || (lastEntry?.date != LocalDate.now()))
        AddEntryButton(modifier.padding(top = 4.dp), viewModel = viewModel)

}

