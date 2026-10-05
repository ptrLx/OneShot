package de.ptrlx.oneshot.contract

import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the persisted names of [HappinessType] (docs/modernization/DATA_CONTRACT.md, sections 2 and 5).
 *
 * Room stores the enum name as TEXT in the `happiness` column and the JSON export writes
 * `happiness.toString()`. Constants may be added at the end (with approval) but never renamed,
 * removed or reordered.
 */
class HappinessTypeContractTest {

    private val storedNames = listOf("VERY_HAPPY", "HAPPY", "NEUTRAL", "SAD", "VERY_SAD", "NOT_SPECIFIED")

    @Test
    fun `existing constants keep their names and order`() {
        val names = HappinessType.values().map { it.name }
        assertEquals(storedNames, names.take(storedNames.size))
        storedNames.forEachIndexed { ordinal, name ->
            assertEquals("ordinal of $name", ordinal, HappinessType.valueOf(name).ordinal)
        }
    }

    @Test
    fun `no constant was added without updating the contract`() {
        // Adding a constant is allowed only with the data-change-approved label: old app versions map
        // unknown values to NOT_SPECIFIED on import. Extend storedNames in the same pull request.
        assertEquals(storedNames, HappinessType.values().map { it.name })
    }

    @Test
    fun `toString is the stored name because the JSON export writes toString`() {
        HappinessType.values().forEach { assertEquals(it.name, it.toString()) }
    }

    @Test
    fun `stored names parse back to the same constant`() {
        storedNames.forEach { assertEquals(it, HappinessType.valueOf(it).name) }
    }
}
