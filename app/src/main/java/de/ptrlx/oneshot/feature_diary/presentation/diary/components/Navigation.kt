package de.ptrlx.oneshot.feature_diary.presentation.diary.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.diary.DiaryScreen
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.home.HomeScreen
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.settings.Settings
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.stats.StatsScreen
import de.ptrlx.oneshot.feature_diary.presentation.diary.util.BottomNavItem

/**
 * Composable that creates a bottom navigation bar.
 *
 * @param modifier
 * @param items
 * @param navController
 *
 */
@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    items: List<BottomNavItem>,
    navController: NavHostController,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentIsBottomNavItem =
        (backStackEntry?.destination?.route.let { it == "home" || it == "diary" || it == "stats" })

    AnimatedVisibility(
        visible = currentIsBottomNavItem,
        enter = slideInVertically(initialOffsetY = { it / 2 }),
        exit = slideOutVertically(targetOffsetY = { it / 2 })
    ) {
        Row(
            modifier = modifier
                .background(MaterialTheme.colors.background)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                AddItem(
                    item = item,
                    currentDestination = backStackEntry?.destination,
                    controller = navController
                )
            }
        }
    }
}

/**
 * Composable to create one bottom navigation item.
 *
 * @param item
 * @param currentDestination
 * @param controller
 */
@Composable
private fun AddItem(
    item: BottomNavItem,
    currentDestination: NavDestination?,
    controller: NavHostController
) {
    val selected = currentDestination?.hierarchy?.any {
        (it.route == item.route)
    } == true

    val background =
        if (selected) MaterialTheme.colors.secondary else MaterialTheme.colors.background

    val contentColor =
        if (selected) MaterialTheme.colors.onSecondary else MaterialTheme.colors.onBackground

    Box(
        modifier = Modifier
            .padding(top = 8.dp)
            .height(40.dp)
            .clip(CircleShape)
            .background(background)
            .clickable {
                if (!selected)
                    controller.navigate(item.route) {
                        popUpTo(controller.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
            }
    ) {
        Row(
            modifier = Modifier
                .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = item.painter,
                contentDescription = item.name,
                tint = contentColor
            )
            AnimatedVisibility(visible = selected) {
                Text(
                    text = item.name,
                    color = contentColor
                )
            }
        }
    }
}

/**
 * Root-Composable that handles navigation of main screen.
 *
 * @param viewModel
 * @param controller
 */
@Composable
fun Navigation(
    viewModel: DiaryViewModel,
    controller: NavHostController,
) {
    // do not overlay with bottom nav bar
    val modifier = Modifier
        .fillMaxSize()
        .padding(bottom = 48.dp)

    NavHost(navController = controller, startDestination = "home") {
        composable("home") {
            HomeScreen(modifier, viewModel, controller = controller)
        }
        composable("diary") {
            DiaryScreen(modifier, viewModel)
        }
        composable("stats") {
            StatsScreen(modifier, viewModel)
        }
        composable("settings") {
            Settings(
                Modifier.fillMaxSize(), viewModel, controller = controller
            )
        }
    }
}
