package com.example.subnetpro

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreference(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
    }

    val themeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_KEY] ?: "auto"
    }

    suspend fun saveTheme(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = mode
        }
    }
}