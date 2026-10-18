package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType

/**
 * One photo, always square.
 *
 * Every surface that holds a one-shot uses this, so a photo is never cropped to one ratio here
 * and another one there, and the empty slot in [CaptureBand] already has the shape the photo
 * will take.
 */
@Composable
fun SquarePhoto(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    label: String? = null,
    happiness: HappinessType? = null,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (label != null || happiness != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                if (label != null) {
                    Text(
                        text = label,
                        modifier = Modifier.align(Alignment.CenterStart),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (happiness != null) {
                    HappinessFace(
                        happiness = happiness,
                        modifier = Modifier.align(Alignment.CenterEnd),
                        size = 20.dp,
                        placeholderTint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
