package com.nathanprazeres.walkunlock.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

class ThemePreferences(private val context: Context) {
    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val USE_SYSTEM_THEME_KEY = booleanPreferencesKey("use_system_theme")
    }

    val isDarkTheme: Flow<Boolean> = context.themeDataStore.data
        .map { preferences -> preferences[DARK_THEME_KEY] == true }

    val useSystemTheme: Flow<Boolean> = context.themeDataStore.data
        .map { preferences -> preferences[USE_SYSTEM_THEME_KEY] != false }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDark
        }
    }

    suspend fun setUseSystemTheme(useSystem: Boolean) {
        context.themeDataStore.edit { preferences ->
            preferences[USE_SYSTEM_THEME_KEY] = useSystem
        }
    }
}
