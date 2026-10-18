package de.ptrlx.oneshot.feature_diary.presentation.diary.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry

/**
 * Up to three happy memories: one large square with two stacked beside it.
 *
 * With fewer than three the survivors grow to fill the same block rather than leaving a hole, so
 * the section is always the same width.
 *
 * The column is sized so that two small squares plus the gap are exactly as tall as the large
 * one, which keeps the block a clean rectangle at any width.
 */
@Composable
fun HappyMemories(
    memories: List<DiaryEntry>,
    label: (DiaryEntry) -> String,
    model: (DiaryEntry) -> Any?,
    onOpen: (DiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        memories.isEmpty() -> Unit

        memories.size == 1 -> {
            val entry = memories[0]
            SquarePhoto(
                model = model(entry),
                contentDescription = label(entry),
                modifier = modifier.fillMaxWidth(),
                label = label(entry),
                happiness = entry.happiness,
                onClick = { onOpen(entry) }
            )
        }

        memories.size == 2 -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            memories.forEach { entry ->
                SquarePhoto(
                    model = model(entry),
                    contentDescription = label(entry),
                    modifier = Modifier.weight(1f),
                    label = label(entry),
                    happiness = entry.happiness,
                    onClick = { onOpen(entry) }
                )
            }
        }

        else -> BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            // Weights alone cannot make the stack match the large square, because the gap is a
            // fixed length rather than a share of the width. Solving
            //   large = 2 * small + gap   and   large + gap + small = width
            // gives small = (width - 2 * gap) / 3, which makes the block an exact rectangle.
            val gap = 8.dp
            val small = (maxWidth - gap * 2) / 3
            val large = small * 2 + gap
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                val first = memories[0]
                SquarePhoto(
                    model = model(first),
                    contentDescription = label(first),
                    modifier = Modifier.width(large),
                    label = label(first),
                    happiness = first.happiness,
                    onClick = { onOpen(first) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    memories.subList(1, minOf(3, memories.size)).forEach { entry ->
                        SquarePhoto(
                            model = model(entry),
                            contentDescription = label(entry),
                            modifier = Modifier.width(small),
                            happiness = entry.happiness,
                            cornerRadius = 12.dp,
                            onClick = { onOpen(entry) }
                        )
                    }
                }
            }
        }
    }
}
