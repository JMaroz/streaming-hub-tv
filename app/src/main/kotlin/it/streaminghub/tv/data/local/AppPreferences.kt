package it.streaminghub.tv.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tv_streaming_hub_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        private val KEY_ACTIVE_PROFILE_NAME = stringPreferencesKey("active_profile_name")
        private val KEY_ACTIVE_PROFILE_AVATAR = stringPreferencesKey("active_profile_avatar")
        private val KEY_PREFER_FHD = booleanPreferencesKey("prefer_fhd")
    }

    val serverUrl: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_SERVER_URL]
    }

    val activeProfileId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_ID] ?: "default"
    }

    val activeProfileName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_NAME] ?: "Principale"
    }

    val activeProfileAvatar: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_AVATAR] ?: "avatar_1"
    }

    val preferFhd: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PREFER_FHD] ?: true
    }

    suspend fun setServerUrl(url: String) {
        val cleanUrl = url.trim().removeSuffix("/")
        context.dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = cleanUrl
        }
    }

    suspend fun setActiveProfile(id: String, name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE_ID] = id
            prefs[KEY_ACTIVE_PROFILE_NAME] = name
            prefs[KEY_ACTIVE_PROFILE_AVATAR] = avatar
        }
    }

    suspend fun setPreferFhd(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PREFER_FHD] = enabled
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
