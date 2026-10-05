package de.ptrlx.oneshot.contract

import de.ptrlx.oneshot.feature_diary.data.data_source.DiaryEntryDao
import de.ptrlx.oneshot.feature_diary.data.data_source.DiaryEntryDatabase
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.Modifier
import java.time.LocalDate

/**
 * Pins the Room database of v1.1.1 (docs/modernization/DATA_CONTRACT.md, section 2).
 *
 * Every database on users' phones was created with exactly these names. A failure here means a change
 * would make existing databases unreadable or silently lose data: do not adapt the expected values,
 * revert the rename or add a Room migration with the `data-change-approved` label.
 */
class RoomContractTest {

    private val databaseClass = "de.ptrlx.oneshot.feature_diary.data.data_source.DiaryEntryDatabase"
    private val identityHash = "78c91e56607bf67a0e206c573e5cd700"
    private val createSql = "CREATE TABLE IF NOT EXISTS `DiaryEntry` (" +
        "`date` INTEGER NOT NULL, `created` INTEGER NOT NULL, `dayOfYear` INTEGER NOT NULL, " +
        "`relativePath` TEXT NOT NULL, `happiness` TEXT NOT NULL, `motivation` TEXT NOT NULL, " +
        "`textContent` TEXT NOT NULL, PRIMARY KEY(`date`))"

    /** Column name to (SQLite affinity, NOT NULL), in table order. */
    private val columns = linkedMapOf(
        "date" to ("INTEGER" to true),
        "created" to ("INTEGER" to true),
        "dayOfYear" to ("INTEGER" to true),
        "relativePath" to ("TEXT" to true),
        "happiness" to ("TEXT" to true),
        "motivation" to ("TEXT" to true),
        "textContent" to ("TEXT" to true),
    )

    @Test
    fun `database file name is diary_entry_db`() {
        assertEquals("diary_entry_db", DiaryEntryDatabase.DATABASE_NAME)
    }

    @Test
    fun `database class keeps its fully qualified name used by the exported schema directory`() {
        assertEquals(databaseClass, DiaryEntryDatabase::class.java.name)
        assertTrue("exported schema of version 1 missing", schemaFile().isFile)
    }

    @Test
    fun `exported schema version 1 describes the DiaryEntry table`() {
        val database = Json.parseToJsonElement(schemaFile().readText()).jsonObject.getValue("database").jsonObject
        assertEquals(1, database.getValue("version").jsonPrimitive.int)
        assertEquals(identityHash, database.getValue("identityHash").jsonPrimitive.content)

        val entities = database.getValue("entities").jsonArray
        assertEquals(1, entities.size)
        val table = entities.single().jsonObject
        assertEquals("DiaryEntry", table.getValue("tableName").jsonPrimitive.content)
        assertEquals(createSql, table.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", "DiaryEntry"))

        val fields = table.getValue("fields").jsonArray.map { it.jsonObject }
        assertEquals(columns.keys.toList(), fields.map { it.string("columnName") })
        assertEquals(columns.keys.toList(), fields.map { it.string("fieldPath") })
        fields.forEach { field ->
            val (affinity, notNull) = columns.getValue(field.string("columnName"))
            assertEquals("affinity of ${field.string("columnName")}", affinity, field.string("affinity"))
            assertEquals("notNull of ${field.string("columnName")}", notNull, field.getValue("notNull").jsonPrimitive.boolean)
        }

        val primaryKey = table.getValue("primaryKey").jsonObject
        assertEquals(listOf("date"), primaryKey.getValue("columnNames").jsonArray.map { it.jsonPrimitive.content })
        assertFalse(primaryKey.getValue("autoGenerate").jsonPrimitive.boolean)
        assertTrue(table.getValue("indices").jsonArray.isEmpty())
        assertTrue(table.getValue("foreignKeys").jsonArray.isEmpty())
    }

    @Test
    fun `entity class maps to the DiaryEntry table and its columns`() {
        // @Entity without tableName: the table is named after the class, the columns after the fields.
        assertEquals("DiaryEntry", DiaryEntry::class.java.simpleName)
        val fields = DiaryEntry::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .associate { it.name to it.type }
        assertEquals(
            mapOf(
                "date" to LocalDate::class.java,
                "created" to Long::class.javaPrimitiveType,
                "dayOfYear" to Int::class.javaPrimitiveType,
                "relativePath" to String::class.java,
                "happiness" to HappinessType::class.java,
                "motivation" to String::class.java,
                "textContent" to String::class.java,
            ),
            fields,
        )
    }

    @Test
    fun `generated database code creates the version 1 table with the frozen identity hash`() {
        val constants = ClassFileStrings.of(DiaryEntryDatabase::class.java.name + "_Impl")
        assertTrue("CREATE TABLE statement changed", createSql in constants)
        assertTrue("identity hash changed", identityHash in constants)
        assertTrue(
            "room_master_table identity row changed",
            "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$identityHash')" in constants,
        )
    }

    @Test
    fun `generated DAO inserts with REPLACE and filters on the stored enum name`() {
        val constants = ClassFileStrings.of(DiaryEntryDao::class.java.name + "_Impl")
        val insert = constants.filter { it.startsWith("INSERT OR REPLACE INTO `DiaryEntry`") }
        assertEquals("expected exactly one INSERT OR REPLACE statement, found $insert", 1, insert.size)
        columns.keys.forEach { column ->
            assertTrue("insert misses column $column: ${insert.single()}", "`$column`" in insert.single())
        }
        assertTrue(
            "getLastVeryHappyDay must compare against the stored enum name 'VERY_HAPPY'",
            constants.any { it.contains("WHERE happiness = 'VERY_HAPPY'") },
        )
    }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content

    private fun schemaFile(): File {
        val relative = "db-schema/${DiaryEntryDatabase::class.java.name}/1.json"
        return listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
            ?: throw AssertionError("Exported Room schema $relative not found (working directory ${File("").absolutePath})")
    }
}
