package chat.stoat.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.ConcurrentHashMap

val Context.stoatKVStorage: DataStore<Preferences> by preferencesDataStore(name = "revolt_kv")

class KVStorage(
    private val mContext: Context
) {
    private val dataStore = mContext.stoatKVStorage

    companion object {
        private val keyCache = ConcurrentHashMap<String, Preferences.Key<String>>()
        private fun prefKey(key: String): Preferences.Key<String> =
            keyCache.computeIfAbsent(key) { stringPreferencesKey(it) }
    }

    suspend fun set(key: String, value: String) {
        dataStore.edit { preferences ->
            preferences[prefKey(key)] = value
        }
    }

    suspend fun get(key: String): String? {
        return dataStore.data.firstOrNull()?.get(prefKey(key))
    }

    suspend fun set(key: String, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[prefKey(key)] = value.toString()
        }
    }

    suspend fun getBoolean(key: String): Boolean? {
        return dataStore.data.firstOrNull()?.get(prefKey(key))?.toBoolean()
    }

    suspend fun set(key: String, value: Int) {
        dataStore.edit { preferences ->
            preferences[prefKey(key)] = value.toString()
        }
    }

    suspend fun getInt(key: String): Int? {
        return dataStore.data.firstOrNull()?.get(prefKey(key))?.toInt()
    }

    suspend fun remove(key: String) {
        dataStore.edit { preferences ->
            preferences.remove(prefKey(key))
        }
    }
}
