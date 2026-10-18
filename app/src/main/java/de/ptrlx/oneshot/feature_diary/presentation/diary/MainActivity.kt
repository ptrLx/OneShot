package de.ptrlx.oneshot.feature_diary.presentation.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.AppTopBar
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.BottomNavigationBar
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.Navigation
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.AddEditEntryModalBottomSheetContent
import de.ptrlx.oneshot.feature_diary.presentation.diary.util.BottomNavItem
import de.ptrlx.oneshot.ui.theme.OneShotTheme

@AndroidEntryPoint
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Draw behind the system bars (enforced from targetSdk 35); the Scaffold below pads the
        // content by the bar, cutout and keyboard insets.
        enableEdgeToEdge()

        setContent {
            val viewModel: DiaryViewModel = hiltViewModel()
            viewModel.startGetEntriesJobs(LocalContext.current)
            OneShotTheme {
                val navController = rememberNavController()

                // A day can pass while the app sits in the background; re-read it on resume so
                // "today" never goes stale.
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshDate()
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val snackbarHostState = remember { SnackbarHostState() }
                val sheetState = rememberModalBottomSheetState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = { AppTopBar(navController) },
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
                    }
                ) { innerPadding ->
                    Navigation(
                        viewModel = viewModel,
                        controller = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                if (viewModel.isSnackbarShowing) {
                    val context = LocalContext.current
                    LaunchedEffect(viewModel.isSnackbarShowing) {
                        try {
                            val result = snackbarHostState.showSnackbar(
                                message = viewModel.snackbarMessage(context.resources),
                                actionLabel = context.getString(viewModel.snackbarCause.actionLabel())
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.onEvent(DiaryEvent.SnackbarDismissed)
                            }
                        } finally {
                            viewModel.isSnackbarShowing = false
                        }
                    }
                }

                // Material 3 shows a modal sheet by composing it, so the former show/hide event
                // pair collapses into this one condition.
                // Home already carries a sheet, and stacking a modal one on top of it looked like
                // two sheets fighting over the same corner. There it hosts the editor itself; the
                // modal is for the screens that have no sheet of their own.
                val currentRoute by navController.currentBackStackEntryAsState()
                val homeOwnsTheSheet = currentRoute?.destination?.route == "home"

                if (viewModel.isEntrySheetVisible && !homeOwnsTheSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { viewModel.onEntrySheetDismissed() },
                        sheetState = sheetState
                    ) {
                        AddEditEntryModalBottomSheetContent(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
