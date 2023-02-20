package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun RoundedButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    text: String,
    arrow: Boolean = true
) {
    Button(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = text)
            if (arrow) {
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = text,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}