package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel

private const val LOG_TAG = "ImageCard"

/** Test tag of the overlay that replaces the photo when it cannot be loaded. */
const val IMAGE_CARD_ERROR_TAG = "image-card-error"

@Composable
fun ImageCard(
    modifier: Modifier = Modifier,
    viewModel: DiaryViewModel,
    entry: DiaryEntry,
    description: String,
    expandOnClick: Boolean = false
) = ImageCard(
    modifier = modifier,
    model = viewModel.fileManager?.resolveUri(entry.relativePath),
    relativePath = entry.relativePath,
    happiness = entry.happiness,
    description = description,
    expandOnClick = expandOnClick
)

/**
 * The card without the view model, so the states of the image load can be tested.
 */
@Composable
internal fun ImageCard(
    modifier: Modifier = Modifier,
    model: Any?,
    relativePath: String,
    happiness: HappinessType,
    description: String,
    expandOnClick: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    // A card whose photo is missing used to stay plain black: the error painter was a drawable tinted
    // #000000 on a black box, and Coil's error was never logged either (#78).
    var loadFailed by remember(model) { mutableStateOf(false) }

    Card(
        modifier = if (expandOnClick)
            modifier.clickable {
                expanded = !expanded
            }
        else modifier,
        shape = RoundedCornerShape(15.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Box(
            modifier = (if (!expanded)
                Modifier
                    .height(200.dp)
                    .width(200.dp)
            else Modifier)
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                modifier = Modifier.fillMaxWidth(),
                model = model,
                contentDescription = stringResource(R.string.desc_img_card, description),
                contentScale = if (!expanded) ContentScale.Crop else ContentScale.FillWidth,
                onSuccess = { loadFailed = false },
                onError = { state ->
                    loadFailed = true
                    Log.e(LOG_TAG, "Could not load the image of $relativePath", state.result.throwable)
                }
            )

            if (!expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            // Fractions, not pixels: `startY = 300f` was past the bottom of a card
                            // shorter than 300 px, and a gradient that starts after it ends clamps
                            // to its last color, so the whole card turned black over the photo.
                            brush = Brush.verticalGradient(
                                0.55f to Color.Transparent,
                                1f to Color.Black
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        description,
                        style = TextStyle(color = Color.White, fontSize = 16.sp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    HappinessFace(happiness = happiness, size = 22.dp)
                }
            }

            // Last child, so the error stays readable above the gradient and the labels.
            if (loadFailed) {
                Column(
                    modifier = Modifier
                        .testTag(IMAGE_CARD_ERROR_TAG)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_error_24),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        stringResource(R.string.msg_img_load_failed),
                        style = TextStyle(color = Color.White, fontSize = 14.sp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

}
