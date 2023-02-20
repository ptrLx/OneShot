package de.ptrlx.oneshot.feature_diary.data.data_source

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import de.ptrlx.oneshot.feature_diary.domain.model.DiaryEntry
import java.time.LocalDate


class Converters {
    companion object {
        @TypeConverter
        @JvmStatic
        fun Long_to_LocalDate(value: Long): LocalDate {
            return LocalDate.ofEpochDay(value)
        }

        @TypeConverter
        @JvmStatic
        fun LocalDate_to_Long(date: LocalDate): Long {
            return date.toEpochDay()
        }
    }
}

@Database(
    entities = [DiaryEntry::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class DiaryEntryDatabase: RoomDatabase() {
    abstract val diaryEntryDao: DiaryEntryDao

    companion object {
        const val DATABASE_NAME = "diary_entry_db"
    }
}