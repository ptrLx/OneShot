package de.ptrlx.oneshot.feature_diary.data.data_source

import androidx.paging.PagingSource
import androidx.room.*
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DiaryEntryDao {
    @Query("SELECT * FROM DiaryEntry")
    fun getDiaryEntries(): Flow<List<DiaryEntry>>

    /**
     * The whole diary newest first, as a [PagingSource].
     *
     * Room keeps this in step with writes and loads only the pages the gallery actually shows,
     * so a diary that grows by 365 rows a year is never read in full to draw one screen.
     */
    @Query("SELECT * FROM DiaryEntry ORDER BY date DESC")
    fun pagingSourceNewestFirst(): PagingSource<Int, DiaryEntry>

    /**
     * Just the dates of every entry, newest first.
     *
     * Eight bytes a row, so the month dividers, the roll strips and the rolls overview can know
     * which days exist across all time without loading a single photo path or diary text.
     */
    @Query("SELECT date FROM DiaryEntry ORDER BY date DESC")
    fun getAllDates(): Flow<List<LocalDate>>

    /**
     * Every entry, once.
     *
     * The JSON export has to contain the whole diary, so it reads this deliberately rather than
     * whatever the paged gallery happens to have loaded.
     */
    @Query("SELECT * FROM DiaryEntry ORDER BY date ASC")
    suspend fun getAllEntriesOnce(): List<DiaryEntry>

    /** How many entries carry each happiness, counted in SQL rather than over a loaded list. */
    @Query("SELECT happiness AS happiness, COUNT(*) AS count FROM DiaryEntry GROUP BY happiness")
    fun countByHappiness(): Flow<List<HappinessCount>>

    /**
     * The happiest days, happiest first and most recent within the same happiness.
     *
     * happiness is stored as its enum name, so ordering by the column would sort "HAPPY" before
     * "VERY_HAPPY" alphabetically; the CASE restores the intended order.
     */
    @Query(
        "SELECT * FROM DiaryEntry " +
                "WHERE happiness IN ('VERY_HAPPY', 'HAPPY') " +
                "ORDER BY CASE happiness WHEN 'VERY_HAPPY' THEN 0 ELSE 1 END, date DESC " +
                "LIMIT :limit"
    )
    fun getHappiestEntries(limit: Int): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM DiaryEntry WHERE textContent LIKE '%' || :keyword || '%'") //todo search for multiple keywords instead; add case sensitivity
    fun getDiaryEntries(keyword: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM DiaryEntry WHERE happiness = :happiness") //todo search for multiple happinesses
    fun getDiaryEntries(happiness: HappinessType): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM DiaryEntry WHERE textContent LIKE '%' || :keyword || '%' AND happiness = :happiness") //todo search for multiple keywords instead; add case sensitivity
    fun getDiaryEntries(keyword: String, happiness: HappinessType): Flow<List<DiaryEntry>>

//    @Query("SELECT * FROM DiaryEntry WHERE created <= :end AND created >= :start")
//    suspend fun getDiaryEntryBetween(start: Timestamp, end: Timestamp): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM DiaryEntry WHERE date = :date")
    fun getDiaryEntry(date: LocalDate): Flow<DiaryEntry?>

    @Query("SELECT * FROM DiaryEntry WHERE dayOfYear = :dayOfYear ")
    fun getDiaryEntriesDateOfYear(dayOfYear: Int): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM DiaryEntry WHERE date >= :start AND date < :end ORDER BY date DESC")
    fun getDiaryEntriesBetween(start: LocalDate, end: LocalDate): Flow<List<DiaryEntry>>

    @Query("SELECT happiness FROM DiaryEntry WHERE date >= :start AND date < :end ORDER BY date DESC")
    fun getHappinessBetween(start: LocalDate, end: LocalDate): Flow<List<HappinessType>>

    @Query(
        "SELECT * " +
                "FROM DiaryEntry " +
                "WHERE created = (SELECT max(created) FROM DiaryEntry WHERE happiness = 'VERY_HAPPY')"
    )
    fun getLastVeryHappyDay(): Flow<DiaryEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntry)

    @Delete
    suspend fun deleteDiaryEntry(entry: DiaryEntry)
}
