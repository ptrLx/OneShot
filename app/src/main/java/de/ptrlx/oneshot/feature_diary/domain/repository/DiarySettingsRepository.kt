package de.ptrlx.oneshot.feature_diary.domain.repository

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface DiarySettingsRepository {
    suspend fun <T> storeValue(key: Preferences.Key<T>, value: T)

    // suspend fun <T> readValue(key: Preferences.Key<T>, responseFunc: T.() -> Unit)

    fun readString(key: String): Flow<String?>
}