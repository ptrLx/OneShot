package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry

/** How much of the band's width the square slot takes. */
private const val SLOT_FRACTION = 0.78f

/**
 * The capture band: a warm block spanning the width, with a square slot centred inside it.
 *
 * The band carries the colour and the slot carries the photo, so capture is unmistakably the
 * first thing on Home while the slot still shows the shape the photo will take. The actions sit
 * inside the band and match the slot's width, so the whole block reads as one thing.
 *
 * Both ways of filling the slot stay available once today is captured: re-shooting and importing
 * are deliberate, named buttons rather than a tap on the photo, because replacing today's image
 * should never be one careless tap away.
 */
@Composable
fun CaptureBand(
    todayEntry: DiaryEntry?,
    todayModel: Any?,
    onCapture: () -> Unit,
    onImport: () -> Unit,
    onOpenToday: (DiaryEntry) -> Unit,
    @androidx.annotation.StringRes captureLabel: Int = R.string.btn_capture_today,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.primaryContainer)
            .padding(16.dp)
    ) {
        val slotWidth: Dp = maxWidth * SLOT_FRACTION
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (todayEntry != null) {
                SquarePhoto(
                    model = todayModel,
                    contentDescription = stringResource(R.string.desc_today_photo),
                    modifier = Modifier.width(slotWidth),
                    happiness = todayEntry.happiness,
                    onClick = { onOpenToday(todayEntry) }
                )
                Row(
                    modifier = Modifier.width(slotWidth),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BandButton(
                        modifier = Modifier.weight(1f),
                        icon = { CameraIcon() },
                        label = stringResource(R.string.btn_retake),
                        onClick = onCapture
                    )
                    BandButton(
                        modifier = Modifier.weight(1f),
                        icon = { GalleryIcon() },
                        label = stringResource(R.string.btn_replace_from_gallery),
                        onClick = onImport
                    )
                }
            } else {
                CaptureSlot(
                    modifier = Modifier.width(slotWidth),
                    label = stringResource(captureLabel),
                    onCapture = onCapture
                )
                BandButton(
                    modifier = Modifier.width(slotWidth),
                    icon = { GalleryIcon() },
                    label = stringResource(R.string.btn_import_from_gallery),
                    onClick = onImport
                )
            }
        }
    }
}

@Composable
private fun CameraIcon() = Icon(
    painter = painterResource(R.drawable.ic_camera_24),
    contentDescription = null,
    modifier = Modifier.size(18.dp)
)

@Composable
private fun GalleryIcon() = Icon(
    painter = painterResource(R.drawable.ic_baseline_photo_library_24),
    contentDescription = null,
    modifier = Modifier.size(18.dp)
)

/** A quiet action on the band: outlined in the container's own ink so it stays inside the block. */
@Composable
private fun BandButton(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    val onContainer = MaterialTheme.colorScheme.onPrimaryContainer
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = onContainer),
        border = androidx.compose.foundation.BorderStroke(1.dp, onContainer.copy(alpha = 0.45f)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 10.dp,
            vertical = 10.dp
        )
    ) {
        icon()
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
private fun CaptureSlot(modifier: Modifier, label: String, onCapture: () -> Unit) {
    val outline = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.55f)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .dashedBorder(outline, 16.dp)
            .clickable { onCapture() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_oneshot_outline),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

/** Compose has no dashed border modifier, so the slot draws its own. */
private fun Modifier.dashedBorder(color: Color, corner: Dp) =
    drawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(corner.toPx(), corner.toPx()),
            style = Stroke(
                width = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(7.dp.toPx(), 5.dp.toPx())
                )
            )
        )
    }
