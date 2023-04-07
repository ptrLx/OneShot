package de.ptrlx.oneshot.feature_diary.presentation.diary.components.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import de.ptrlx.oneshot.R

/**
 * Composable for top of home screen.
 *
 * @param modifier
 * @param controller
 */
@Composable
fun HomeTop(
    modifier: Modifier = Modifier,
    controller: NavController
) {
    Column(
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold
            )

            Icon(
                Icons.Default.Settings, contentDescription = stringResource(R.string.settings) ,
                modifier = Modifier
                    .size(30.dp)
                    .clickable {
                        controller.navigate("settings") {
                            launchSingleTop = true
                        }
                    },
                tint = MaterialTheme.colors.onBackground
            )
        }

        Text(
            text = stringResource(R.string.app_slogan),
            style = MaterialTheme.typography.body1
        )
    }
}
