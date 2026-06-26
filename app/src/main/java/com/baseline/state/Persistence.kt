package com.baseline.state

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baseline.model.PersistedState
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persistence: one JSON blob under a single DataStore key — the Android analogue of the
 * prototype's `localStorage['baseline']`. Written only when not editing and not dragging
 * (the single-write discipline lives in [BaselineViewModel]).
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "baseline")

class BaselineRepository(context: Context) {
    private val ds = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun load(): PersistedState? {
        val raw = ds.data.first()[BLOB] ?: return null
        return try {
            json.decodeFromString<PersistedState>(raw)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun save(state: PersistedState) {
        ds.edit { it[BLOB] = json.encodeToString(state) }
    }

    private companion object {
        val BLOB = stringPreferencesKey("baseline")
    }
}
