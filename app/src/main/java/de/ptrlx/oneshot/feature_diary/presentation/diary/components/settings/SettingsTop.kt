package de.ptrlx.oneshot.feature_diary.presentation.diary.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import de.ptrlx.oneshot.R

/**
 * Composable for top of settings screen.
 *
 * @param modifier
 * @param controller
 */
@Composable
fun SettingsTop(
    modifier: Modifier = Modifier,
    controller: NavController
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            Icons.Default.ArrowBack, contentDescription = stringResource(
                R.string.back
            ),
            modifier = Modifier
                .size(50.dp)
                .padding(end = 8.dp)
                .clickable {
                    controller.navigate("home") {
                        popUpTo(controller.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
            tint = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )

    }

}