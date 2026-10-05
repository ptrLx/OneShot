package de.ptrlx.oneshot.contract

import androidx.datastore.preferences.core.stringPreferencesKey
import de.ptrlx.oneshot.feature_diary.data.repository.DiarySettingsImplementation
import de.ptrlx.oneshot.feature_diary.domain.util.imageBaseLocationKey
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the Preferences DataStore of v1.1.1 (docs/modernization/DATA_CONTRACT.md, section 3).
 *
 * The DataStore file `files/datastore/diary_settings.preferences_pb` holds the folder the user picked.
 * Renaming the store or the key makes the app forget the folder after an update.
 */
class SettingsContractTest {

    @Test
    fun `image folder key is image_base_location`() {
        assertEquals("image_base_location", imageBaseLocationKey)
        assertEquals("image_base_location", stringPreferencesKey(imageBaseLocationKey).name)
    }

    @Test
    fun `settings are stored in the DataStore named diary_settings`() {
        // preferencesDataStore("diary_settings") is a private delegate, so read the compiled literal.
        val constants = ClassFileStrings.of(DiarySettingsImplementation::class.java)
        assertTrue("DataStore name diary_settings changed", "diary_settings" in constants)
    }

    @Test
    fun `view model reads and writes the folder under the pinned key`() {
        val constants = ClassFileStrings.of(DiaryViewModel::class.java)
        assertTrue("DiaryViewModel no longer uses the key image_base_location", "image_base_location" in constants)
    }
}
