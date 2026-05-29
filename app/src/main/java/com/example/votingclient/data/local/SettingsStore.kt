package com.example.votingclient.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val historyKey = stringPreferencesKey("search_history")

    val darkTheme: Flow<Boolean> = context.settingsDataStore.data.map { it[darkThemeKey] ?: false }

    val history: Flow<List<String>> = context.settingsDataStore.data.map { prefs ->
        prefs[historyKey]
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun addHistory(text: String) {
        val value = text.trim()
        if (value.isBlank()) return
        context.settingsDataStore.edit { prefs ->
            val old = prefs[historyKey]
                ?.split("\n")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            prefs[historyKey] = (listOf(value) + old.filterNot { it.equals(value, ignoreCase = true) })
                .take(10)
                .joinToString("\n")
        }
    }

    suspend fun clearHistory() {
        context.settingsDataStore.edit { it.remove(historyKey) }
    }
}
