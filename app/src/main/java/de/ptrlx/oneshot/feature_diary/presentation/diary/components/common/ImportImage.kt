package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel

@Composable
fun ImportImageButton(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    if (viewModel.fileManager != null) {
        val launcher =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                it?.let {
                    viewModel.onEvent(DiaryEvent.ImageImport(it))
                }
            }

        RoundedButton(
            modifier = modifier,
            text = stringResource(R.string.btn_import_img),
            onClick = {
                    launcher.launch("image/*")
            }
        )
    }
}