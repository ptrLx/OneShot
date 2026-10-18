package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType

/**
 * The drawable for a happiness value.
 *
 * The faces are app drawables rather than system emoji because U+1FAE5, used for
 * [HappinessType.NOT_SPECIFIED], has no glyph on Android 10 and 11 and renders as a tofu box (#77).
 * Drawables also keep the rating identical on every Android version.
 */
@DrawableRes
fun HappinessType.iconRes(): Int = when (this) {
    HappinessType.VERY_HAPPY -> R.drawable.ic_happiness_very_happy
    HappinessType.HAPPY -> R.drawable.ic_happiness_happy
    HappinessType.NEUTRAL -> R.drawable.ic_happiness_neutral
    HappinessType.SAD -> R.drawable.ic_happiness_sad
    HappinessType.VERY_SAD -> R.drawable.ic_happiness_very_sad
    HappinessType.NOT_SPECIFIED -> R.drawable.ic_happiness_not_specified
}

/**
 * A happiness face.
 *
 * The five moods are full colour, because they have to read over arbitrary photos. The
 * placeholder marks the absence of a rating rather than a mood, so it is monochrome and follows
 * the surface it sits on.
 *
 * @param placeholderTint colour for [HappinessType.NOT_SPECIFIED]; defaults to a faint onSurface.
 */
@Composable
fun HappinessFace(
    happiness: HappinessType,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    placeholderTint: Color? = null
) {
    val painter = painterResource(happiness.iconRes())
    if (happiness == HappinessType.NOT_SPECIFIED) {
        Icon(
            painter = painter,
            contentDescription = null,
            modifier = modifier.size(size),
            tint = placeholderTint ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    } else {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier.size(size)
        )
    }
}
