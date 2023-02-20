package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_settings

import de.ptrlx.oneshot.feature_diary.domain.repository.DiarySettingsRepository
import kotlinx.coroutines.flow.Flow

class GetSettingsString(
    private val repository: DiarySettingsRepository
) {

    /**
     * UseCase to read a String from the diary settings.
     *
     * @param key
     */
    operator fun invoke(key: String): Flow<String?> {
        return repository.readString(key)
    }
}

