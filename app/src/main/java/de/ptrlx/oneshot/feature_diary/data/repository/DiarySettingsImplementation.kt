package de.ptrlx.oneshot.feature_diary.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.ptrlx.oneshot.feature_diary.domain.repository.DiarySettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class DiarySettingsImplementation @Inject constructor(@ApplicationContext private val appContext: Context) :
    DiarySettingsRepository {
    private val Context.dataStore by preferencesDataStore("diary_settings")

    private suspend fun <T> DataStore<Preferences>.getFromLocalStorage(
        PreferencesKey: Preferences.Key<T>, func: T.() -> Unit
    ) {
        data.catch {
            if (it is IOException) {
                emit(emptyPreferences())
            } else {
                throw it
            }
        }.map {
            it[PreferencesKey]
        }.collect {
            it?.let { func.invoke(it as T) }
        }
    }

    override suspend fun <T> storeValue(key: Preferences.Key<T>, value: T) {
        appContext.dataStore.edit {
            it[key] = value
        }
    }

    override fun readString(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return appContext.dataStore.data.map {
            it[prefKey]
        }
    }

//    override suspend fun <T> readValue(key: Preferences.Key<T>, responseFunc: T.() -> Unit) {
//        appContext.dataStore.getFromLocalStorage(key) {
//            responseFunc.invoke(this)
//        }
//    }
}