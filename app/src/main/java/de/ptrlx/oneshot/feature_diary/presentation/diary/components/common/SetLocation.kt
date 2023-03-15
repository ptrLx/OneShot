package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel


@Composable
fun SetLocation(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    arrow: Boolean = true
) {
    val context = LocalContext.current
    val activity = context as Activity
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let {
                viewModel.fileManager?.let {
                    val toast =
                        Toast.makeText(
                            context,
                            "Please move old images manually to the new folder",
                            Toast.LENGTH_LONG
                        )
                    toast.show()
                }
                viewModel.onEvent(DiaryEvent.SetImageBaseLocation(activity, it))
            }
        }

    RoundedButton(
        modifier = modifier,
        text = "Set image file path",
        onClick = { launcher.launch(viewModel.fileManager?.baseLocation) },
        arrow = arrow
    )
}