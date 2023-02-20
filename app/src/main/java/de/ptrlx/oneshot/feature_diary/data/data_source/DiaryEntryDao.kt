package de.ptrlx.oneshot.feature_diary.data.data_source

import androidx.room.*
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DiaryEntryDao {
    @Query("SELECT * FROM DiaryEntry")
    fun getDiaryEntries(): Flow<List<DiaryEntry>>

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
