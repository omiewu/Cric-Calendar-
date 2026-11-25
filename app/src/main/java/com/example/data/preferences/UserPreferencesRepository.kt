package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.UserPreferences
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cricPulseDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cricpulse_user_preferences"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITE_TEAMS = stringSetPreferencesKey("favorite_teams")
        val AUTO_NOTIFY_FAVORITES = booleanPreferencesKey("auto_notify_favorites")
        val GLOBAL_NOTIFY_12H = booleanPreferencesKey("global_notify_12h")
        val GLOBAL_NOTIFY_2H = booleanPreferencesKey("global_notify_2h")
        val GLOBAL_NOTIFY_5M = booleanPreferencesKey("global_notify_5m")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color_enabled")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
        val API_BASE_URL = stringPreferencesKey("api_base_url")
    }

    val preferencesFlow: Flow<UserPreferences> = context.cricPulseDataStore.data.map { prefs ->
        UserPreferences(
            favoriteTeams = prefs[Keys.FAVORITE_TEAMS] ?: emptySet(),
            autoNotifyFavorites = prefs[Keys.AUTO_NOTIFY_FAVORITES] ?: false,
            globalNotify12h = prefs[Keys.GLOBAL_NOTIFY_12H] ?: true,
            globalNotify2h = prefs[Keys.GLOBAL_NOTIFY_2H] ?: true,
            globalNotify5m = prefs[Keys.GLOBAL_NOTIFY_5M] ?: true,
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name)
            }.getOrDefault(ThemeMode.SYSTEM),
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: false,
            customApiKey = prefs[Keys.CUSTOM_API_KEY] ?: "",
            apiBaseUrl = prefs[Keys.API_BASE_URL]?.ifBlank { UserPreferences.DEFAULT_CRIC_API_BASE_URL }
                ?: UserPreferences.DEFAULT_CRIC_API_BASE_URL
        )
    }

    suspend fun getPreferencesOnce(): UserPreferences = preferencesFlow.first()

    suspend fun toggleFavoriteTeam(teamName: String): Set<String> {
        var updated: Set<String> = emptySet()
        context.cricPulseDataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_TEAMS]?.toMutableSet() ?: mutableSetOf()
            if (!current.add(teamName)) {
                current.remove(teamName)
            }
            prefs[Keys.FAVORITE_TEAMS] = current
            updated = current.toSet()
        }
        return updated
    }

    suspend fun setAutoNotifyFavorites(enabled: Boolean) {
        context.cricPulseDataStore.edit { prefs ->
            prefs[Keys.AUTO_NOTIFY_FAVORITES] = enabled
        }
    }

    suspend fun setGlobalIntervals(notify12h: Boolean, notify2h: Boolean, notify5m: Boolean) {
        context.cricPulseDataStore.edit { prefs ->
            prefs[Keys.GLOBAL_NOTIFY_12H] = notify12h
            prefs[Keys.GLOBAL_NOTIFY_2H] = notify2h
            prefs[Keys.GLOBAL_NOTIFY_5M] = notify5m
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.cricPulseDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.cricPulseDataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun saveApiConfig(apiKey: String, baseUrl: String) {
        val sanitizedUrl = baseUrl.trim().let { url ->
            if (url.isBlank()) UserPreferences.DEFAULT_CRIC_API_BASE_URL
            else if (url.endsWith("/")) url else "$url/"
        }
        context.cricPulseDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_API_KEY] = apiKey.trim()
            prefs[Keys.API_BASE_URL] = sanitizedUrl
        }
    }
}
