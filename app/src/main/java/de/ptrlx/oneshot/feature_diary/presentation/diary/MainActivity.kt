package de.ptrlx.oneshot.feature_diary.presentation.diary

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.BottomNavigationBar
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.Navigation
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.AddEditEntryModalBottomSheetContent
import de.ptrlx.oneshot.feature_diary.presentation.diary.util.BottomNavItem
import de.ptrlx.oneshot.ui.theme.OneShotTheme


@AndroidEntryPoint
@OptIn(ExperimentalMaterialApi::class)
class MainActivity : ComponentActivity() {
    private val LOG_TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OneShotTheme {
                val viewModel: DiaryViewModel = hiltViewModel()
                viewModel.startGetEntriesJobs(LocalContext.current)
                val navController = rememberNavController()
                val sheetState =
                    rememberModalBottomSheetState(
                        initialValue = ModalBottomSheetValue.Hidden
                    )
                ModalBottomSheetLayout(
                    sheetState = sheetState,
                    sheetShape = RoundedCornerShape(16.dp, 16.dp, 0.dp, 0.dp),
                    sheetContent = {
                        AddEditEntryModalBottomSheetContent(
                            viewModel = viewModel
                        )
                    }
                ) {
                    val scaffoldState = rememberScaffoldState()
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        scaffoldState = scaffoldState,
                        bottomBar = {
                            BottomNavigationBar(
                                items = listOf(
                                    BottomNavItem(
                                        name = stringResource(R.string.route_home),
                                        route = "home",
                                        painter = painterResource(id = R.drawable.ic_baseline_cottage_24)
                                    ),
                                    BottomNavItem(
                                        name = stringResource(R.string.route_diary),
                                        route = "diary",
                                        painter = painterResource(id = R.drawable.ic_baseline_photo_library_24)
                                    ),
                                    BottomNavItem(
                                        name = stringResource(R.string.route_statistics),
                                        route = "stats",
                                        painter = painterResource(id = R.drawable.ic_baseline_leaderboard_24)
                                    ),
                                ),
                                navController = navController
                            )
                        },

                        ) { _ ->
                        if (viewModel.isSnackbarShowing) {
                            val context = LocalContext.current
                            LaunchedEffect(viewModel.isSnackbarShowing) {
                                try {
                                    when (scaffoldState.snackbarHostState.showSnackbar(
                                        context.getString(viewModel.snackbarCause.msg()),
                                        actionLabel = context.getString(viewModel.snackbarCause.actionLabel())
                                    )) {
                                        SnackbarResult.ActionPerformed -> {
                                            viewModel.onEvent(DiaryEvent.SnackbarDismissed)
                                        }
                                        else -> {}
                                    }
                                } finally {
                                    viewModel.isSnackbarShowing = false
                                }
                            }
                        }

                        if (viewModel.modalBottomSheetHideEvent) {
                            Log.d(LOG_TAG, "Hide modal bottom-sheet")
                            LaunchedEffect(viewModel.modalBottomSheetHideEvent) {
                                sheetState.hide()
                                viewModel.modalBottomSheetHideEvent = false
                            }
                        } else if (viewModel.modalBottomSheetShowEvent) {
                            Log.d(LOG_TAG, "Show modal bottom-sheet")
                            LaunchedEffect(viewModel.modalBottomSheetShowEvent) {
                                sheetState.show()
                                viewModel.modalBottomSheetShowEvent = false
                            }
                            //In case sheet was dismissed by clicking outside of it
                        } else if (!sheetState.isVisible) {
                            Log.d(LOG_TAG, "Dismissed modal bottom-sheet")
                            viewModel.newModalDialog = false
                        }

                        Navigation(viewModel, navController)
                    }
                }
            }
        }
    }
}
