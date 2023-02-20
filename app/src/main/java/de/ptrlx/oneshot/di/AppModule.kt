package de.ptrlx.oneshot.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.ptrlx.oneshot.feature_diary.data.data_source.DiaryEntryDatabase
import de.ptrlx.oneshot.feature_diary.data.repository.DiarySettingsImplementation
import de.ptrlx.oneshot.feature_diary.data.repository.DiaryEntryImplementation
import de.ptrlx.oneshot.feature_diary.domain.repository.DiaryEntryRepository
import de.ptrlx.oneshot.feature_diary.domain.repository.DiarySettingsRepository
import de.ptrlx.oneshot.feature_diary.domain.use_case.*
import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_entry.*
import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_settings.GetSettingsString
import de.ptrlx.oneshot.feature_diary.domain.use_case.diary_settings.SetSettingsValue
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideDiaryEntryDatabase(app: Application): DiaryEntryDatabase =
        Room.databaseBuilder(
            app,
            DiaryEntryDatabase::class.java,
            DiaryEntryDatabase.DATABASE_NAME
        ).build()

    @Provides
    @Singleton
    fun provideDiaryEntryRepository(db: DiaryEntryDatabase): DiaryEntryRepository =
        DiaryEntryImplementation(db.diaryEntryDao)

    @Provides
    @Singleton
    fun provideDiarySettingsRepository(@ApplicationContext appContext: Context): DiarySettingsRepository =
        DiarySettingsImplementation(appContext)

    @Provides
    @Singleton
    fun provideDiaryUseCases(
        diaryEntryRepository: DiaryEntryRepository,
        diarySettingsRepository: DiarySettingsRepository
    ): DiaryUseCases {
        return DiaryUseCases(
            createUpdateDiaryEntry = CreateUpdateDiaryEntryUseCase(diaryEntryRepository),
            deleteDiaryEntry = DeleteDiaryEntryUseCase(diaryEntryRepository),
            getCalenderMonth = GetCalenderMonthUseCase(diaryEntryRepository),
            getDiaryEntries = GetDiaryEntriesUseCase(diaryEntryRepository),
            getFlashbacks = GetFlashbacksUseCase(diaryEntryRepository),
            getStats = GetStatsUseCase(diaryEntryRepository),
            setSettingsValue = SetSettingsValue(diarySettingsRepository),
            getSettingsString = GetSettingsString(diarySettingsRepository)
        )
    }
}