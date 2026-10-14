package de.ptrlx.oneshot.ui

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.ptrlx.oneshot.R
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.IMAGE_CARD_ERROR_TAG
import de.ptrlx.oneshot.feature_diary.presentation.diary.components.common.ImageCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * A card whose photo cannot be loaded must say so (#78). Before the fix the error painter was a
 * drawable tinted #000000 drawn on a black box, so a failed load looked like an empty black card.
 */
@RunWith(AndroidJUnit4::class)
class ImageCardErrorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val message: String
        get() = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.msg_img_load_failed)

    private fun missingImage(): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "OneShot_20230403101500.jpg")
        file.delete()
        return Uri.fromFile(file)
    }

    @Test
    fun an_image_that_cannot_be_loaded_shows_the_error_overlay() {
        composeRule.setContent {
            ImageCard(
                model = missingImage(),
                relativePath = "OneShot_20230403101500.jpg",
                happiness = HappinessType.HAPPY,
                description = "3 April 2023"
            )
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(IMAGE_CARD_ERROR_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(IMAGE_CARD_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }

    @Test
    fun a_card_without_a_model_shows_the_error_overlay() {
        // resolveUri returns null when the folder permission is gone, which must not look like a
        // black card either.
        composeRule.setContent {
            ImageCard(
                model = null,
                relativePath = "OneShot_20230403101500.jpg",
                happiness = HappinessType.NOT_SPECIFIED,
                description = "3 April 2023"
            )
        }

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(IMAGE_CARD_ERROR_TAG).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(IMAGE_CARD_ERROR_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(message).assertIsDisplayed()
    }
}
