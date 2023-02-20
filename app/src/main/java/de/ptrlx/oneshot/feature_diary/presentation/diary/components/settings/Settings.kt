package de.ptrlx.oneshot.feature_diary.presentation.diary.components.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryEvent
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.RoundedButton
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.SetLocation

/**
 * Main Composable for settings screen.
 *
 * @param modifier
 * @param viewModel
 * @param controller
 */
@Composable
fun Settings(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    controller: NavController
) {
    AnimatedVisibility(
        modifier = modifier.padding(start = 8.dp, end = 8.dp),
        visible = (controller.currentDestination?.route == "settings"),
        exit = slideOutHorizontally(targetOffsetX = { it / 2 })
    ) {
        Column {
            SettingsTop(controller = controller)
            Column(
                modifier.verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Spacer(modifier = Modifier.size(8.dp))

                AboutText()

                Spacer(modifier = Modifier.size(48.dp))

                SettingsButtons(viewModel = viewModel)

                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun SettingsButtons(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    Column(modifier) {
        Spacer(modifier = Modifier.size(8.dp))
        SetLocation(viewModel = viewModel, arrow = false)
        Spacer(modifier = Modifier.size(8.dp))
        ImportDatabaseButton(viewModel = viewModel)
        Spacer(modifier = Modifier.size(8.dp))
        RoundedButton(
            text = "Export database",
            onClick = { viewModel.onEvent(DiaryEvent.WriteExport) },
            arrow = false
        )
        Spacer(modifier = Modifier.size(8.dp))
    }
}

@Composable
fun AboutText(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "Thank you for using OneShot!",
            style = MaterialTheme.typography.h4
        )

        Text(
            text = "About",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "OneShot is made to remind you of the special moments. " +
                    "Because every day has at least one. And that's what counts in life! " +
                    "So make it your habit and remember the happy days!"
        )
        Text(
            text = "Privacy",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your data is yours!\n" +
                    "OneShot will not send data to any server without your consent. " +
                    "The app requires currently no internet permission."

            //todo "Internet permission is only needed for fetching the motivation API. It can safely be removed in settings.\n"
        )
        Text(
            text = "License",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This app is made with passion and love by ptrLx️. " +
                    "It is free software released under GPLv3 and comes with absolutely no warranty!"
                    "Fork me on GitHub: https://github.com/ptrLx/OneShot"
        )
        Text(
            text = "Open Source Licenses",
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Coil - Apache License Version 2.0\n" +
                    "Dagger - Apache License Version 2.0\n" +
                    "Accompanist - Apache License Version 2.0"
        )
    }
}


@Composable
fun ImportDatabaseButton(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                viewModel.currentImportDatabaseUri = uri
                showDialog = true
            }
        }

    RoundedButton(
        modifier = modifier,
        text = "Import database",
        onClick = { launcher.launch(arrayOf("application/json")) },
        arrow = false
    )

    viewModel.currentImportDatabaseUri?.let {
        AnimatedVisibility(visible = showDialog) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.currentImportDatabaseUri = null
                    showDialog = false
                },
                title = {
                    Text(
                        "Import database?",
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.error
                    )
                },
                text = { Text("This may overwrite entries in your current diary") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.error),
                        onClick = {
                            showDialog = false
                            viewModel.onEvent(DiaryEvent.ReadImport)
                        }) {
                        Text("OK", color = MaterialTheme.colors.onError)
                    }
                },
                dismissButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.background),
                        onClick = {
                            showDialog = false
                        }) {
                        Text(text = "ABORT", color = MaterialTheme.colors.onBackground)
                    }
                }
            )
        }
    }
}