package de.ptrlx.oneshot.contract

import de.ptrlx.oneshot.feature_diary.data.data_source.Converters
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Pins the Room type converter for `DiaryEntry.date` (docs/modernization/DATA_CONTRACT.md, section 2):
 * dates are stored as `LocalDate.toEpochDay()`, which is also the `date` value of the JSON export.
 */
class ConvertersContractTest {

    private val epochDays = linkedMapOf(
        LocalDate.of(1969, 12, 31) to -1L,
        LocalDate.of(1970, 1, 1) to 0L,
        LocalDate.of(2000, 2, 29) to 11016L,
        LocalDate.of(2020, 2, 29) to 18321L,
        LocalDate.of(2023, 4, 3) to 19450L,
        LocalDate.of(2024, 2, 29) to 19782L,
        LocalDate.of(2024, 12, 31) to 20088L,
        LocalDate.of(2100, 2, 28) to 47540L,
        LocalDate.of(2100, 3, 1) to 47541L, // 2100 is not a leap year
        LocalDate.of(2100, 12, 31) to 47846L,
    )

    @Test
    fun `dates are stored as epoch days`() {
        epochDays.forEach { (date, day) -> assertEquals("epoch day of $date", day, Converters.LocalDate_to_Long(date)) }
    }

    @Test
    fun `epoch days read back as the same date`() {
        epochDays.forEach { (date, day) -> assertEquals("date of epoch day $day", date, Converters.Long_to_LocalDate(day)) }
    }

    @Test
    fun `round trip is lossless for every day of a leap year and of 2100`() {
        listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2100, 1, 1)).forEach { start ->
            generateSequence(start) { it.plusDays(1) }.takeWhile { it.year == start.year }.forEach { date ->
                assertEquals(date, Converters.Long_to_LocalDate(Converters.LocalDate_to_Long(date)))
            }
        }
    }
}
