package de.ptrlx.oneshot.contract

import de.ptrlx.oneshot.feature_diary.domain.util.DiaryFileManager
import de.ptrlx.oneshot.feature_diary.presentation.diary.DiaryViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Pins image and export file naming (docs/modernization/DATA_CONTRACT.md, sections 4 and 5).
 *
 * Entries reference their image by file name only (`relativePath`), so the folder contents written by
 * every released version must keep resolving. The names are built in private code of [DiaryViewModel]:
 * the tests check the compiled literals and the shape of names produced with the same format.
 */
class FileNamingContractTest {

    private val imageName = Regex("^OneShot_\\d{14}\\.jpg$")
    private val exportName = Regex("^OneShot_DB_\\d{14}\\.json$")

    /** 2023-04-03T10:15:00Z, the example of DATA_CONTRACT.md. */
    private val timestamp = 1680516900_000L

    private fun format(locale: Locale): String =
        SimpleDateFormat("yyyyMMddHHmmss", locale).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(timestamp)

    @Test
    fun `view model builds names from the pinned pattern`() {
        val constants = ClassFileStrings.of(DiaryViewModel::class.java)
        assertTrue("timestamp pattern yyyyMMddHHmmss changed", "yyyyMMddHHmmss" in constants)
        assertTrue("image name OneShot_<timestamp>.jpg changed", constants.containsTemplate("OneShot_", ".jpg"))
        assertTrue("export name OneShot_DB_<timestamp>.json changed", constants.containsTemplate("OneShot_DB_", ".json"))
    }

    @Test
    fun `image names have 26 characters`() {
        val name = "OneShot_${format(Locale.ROOT)}.jpg"
        assertEquals("OneShot_20230403101500.jpg", name)
        assertEquals(26, name.length)
        assertTrue(imageName.matches(name))
    }

    @Test
    fun `export names follow OneShot_DB_timestamp json`() {
        val name = "OneShot_DB_${format(Locale.ROOT)}.json"
        assertEquals("OneShot_DB_20230403101500.json", name)
        assertTrue(exportName.matches(name))
    }

    @Test
    fun `names keep their shape in common locales with ASCII digits`() {
        // The app formats with Locale.getDefault() (see #68). With ASCII digits every locale yields the
        // same name. Locales with other digits (for example Arabic-Indic) still yield 26 characters,
        // so the length check in CreateUpdateDiaryEntryUseCase accepts them, but the name then contains
        // non-ASCII digits: see CreateUpdateDiaryEntryUseCaseContractTest.
        listOf(Locale.ROOT, Locale.US, Locale.UK, Locale.GERMANY, Locale.FRANCE, Locale.JAPAN, Locale.CHINA).forEach { locale ->
            val name = "OneShot_${format(locale)}.jpg"
            assertEquals("image name in $locale", "OneShot_20230403101500.jpg", name)
        }
    }

    @Test
    fun `file manager writes images and exports with the pinned MIME types and resolves by file name`() {
        val constants = ClassFileStrings.of(DiaryFileManager::class.java)
        assertTrue("image MIME type image/jpg changed", "image/jpg" in constants)
        assertTrue("export MIME type application/json changed", "application/json" in constants)
        assertTrue(
            "document URI building document/<tree path>%2F<filename> changed",
            constants.containsTemplate("document/", "%2F"),
        )
    }
}
