package de.ptrlx.oneshot.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.ImageCard
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * The label gradient must not swallow the photo (#78, #17).
 *
 * It used to start at a hardcoded `startY = 300f` **pixels**. On a card shorter than that the
 * gradient starts after it ends, clamps to its last color and paints the whole card black, which
 * looks exactly like an image that failed to load even though it loaded fine.
 */
@RunWith(AndroidJUnit4::class)
class ImageCardGradientTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val magenta = android.graphics.Color.rgb(255, 0, 255)

    private fun solidColorImage(): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(magenta)
        val file = File(context.cacheDir, "card-gradient-${System.nanoTime()}.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
        return Uri.fromFile(file)
    }

    @Test
    fun a_short_card_still_shows_the_photo() {
        val description = "3 April 2023"
        composeRule.setContent {
            ImageCard(
                modifier = Modifier.size(100.dp),   // well below the old 300 px start
                model = solidColorImage(),
                relativePath = "OneShot_20230403101500.jpg",
                happiness = HappinessType.HAPPY,
                description = description
            )
        }

        fun topOfCard(): Color = composeRule
            .onNodeWithContentDescription("Diary entry: $description")
            .captureToImage()
            .toPixelMap()
            .let { it[it.width / 2, it.height / 6] }

        composeRule.waitUntil(timeoutMillis = 15_000) { topOfCard() != Color.Black }
        assertNotEquals("top of the card", Color.Black, topOfCard())
    }
}
