package de.ptrlx.oneshot.feature_diary.domain.use_case.diary_settings

import androidx.datastore.preferences.core.Preferences
import de.ptrlx.oneshot.feature_diary.domain.repository.DiarySettingsRepository

class SetSettingsValue(
    private val repository: DiarySettingsRepository
) {

    /**
     * UseCase to store a value in the diary settings.
     *
     * @param T
     * @param key
     * @param value
     */
    suspend operator fun <T> invoke(key: Preferences.Key<T>, value: T) {
        repository.storeValue(key, value)
    }
}
