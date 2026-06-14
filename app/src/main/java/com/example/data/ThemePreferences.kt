package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

object ThemePreferences {
    private val THEME_KEY = stringPreferencesKey("selected_theme_variant")

    fun getTheme(context: Context): Flow<String> {
        return context.themeDataStore.data.map { preferences ->
            preferences[THEME_KEY] ?: "INFERNO"
        }
    }

    suspend fun saveTheme(context: Context, themeName: String) {
        context.themeDataStore.edit { preferences ->
            preferences[THEME_KEY] = themeName
        }
    }

    fun getThemeSync(context: Context): String {
        return try {
            runBlocking {
                getTheme(context).first()
            }
        } catch (e: Exception) {
            "INFERNO"
        }
    }
}
