package de.ptrlx.oneshot.feature_diary.presentation.diary.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.diary.DiaryScreen
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.home.HomeScreen
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.rolls.RollDetailScreen
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.rolls.RollsOverviewScreen
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.settings.Settings
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.stats.MoodScreen
import de.ptrlx.oneshot.feature_diary.presentation.diary.util.BottomNavItem
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.DayPart
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** The three places you live in. Everything else is reached from them and dismissed with back. */
private val TOP_LEVEL_ROUTES = setOf("home", "diary", "stats")

private val ROLL_MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

/** Route of the roll detail for [month]. */
fun rollRoute(month: YearMonth): String = "roll/${month.format(ROLL_MONTH_FORMAT)}"

/**
 * The top bar of the three top-level screens.
 *
 * No scroll behaviour is attached on purpose, so it stays put while the content scrolls under it.
 * The detail screens bring their own back bar instead, which is why this is hidden there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    if (route !in TOP_LEVEL_ROUTES) return

    val dayPart = DayPart.of()
    val title = when (route) {
        "diary" -> stringResource(R.string.route_diary)
        "stats" -> stringResource(R.string.route_statistics)
        else -> "${stringResource(dayPart.greeting)}  ${dayPart.emoji}"
    }

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = {
                navController.navigate("settings") { launchSingleTop = true }
            }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }
    )
}

/**
 * The Material 3 navigation bar.
 *
 * It is only shown on the three top-level destinations; a roll, a day or the settings are detail
 * screens pushed on top, so they keep the bar hidden and return with back.
 */
@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    items: List<BottomNavItem>,
    navController: NavHostController
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    if (currentDestination?.route !in TOP_LEVEL_ROUTES) return

    NavigationBar(modifier = modifier) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painter = item.painter, contentDescription = item.name) },
                label = { Text(item.name) }
            )
        }
    }
}

/**
 * Root composable that handles navigation of the main screen.
 */
@Composable
fun Navigation(
    viewModel: DiaryViewModel,
    controller: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = controller,
        startDestination = "home",
        modifier = modifier.fillMaxSize()
    ) {
        composable("home") { HomeScreen(viewModel = viewModel, controller = controller) }
        composable("diary") { DiaryScreen(viewModel = viewModel, controller = controller) }
        composable("stats") { MoodScreen(viewModel = viewModel, controller = controller) }
        composable("settings") {
            Settings(Modifier.fillMaxSize(), viewModel, controller = controller)
        }
        composable("rolls") {
            RollsOverviewScreen(viewModel = viewModel, controller = controller)
        }
        composable(
            route = "roll/{month}",
            arguments = listOf(navArgument("month") { type = NavType.StringType })
        ) { entry ->
            val month = entry.arguments?.getString("month")
                ?.let { runCatching { YearMonth.parse(it, ROLL_MONTH_FORMAT) }.getOrNull() }
                ?: YearMonth.from(viewModel.today)
            RollDetailScreen(
                viewModel = viewModel,
                controller = controller,
                month = month
            )
        }
    }
}
