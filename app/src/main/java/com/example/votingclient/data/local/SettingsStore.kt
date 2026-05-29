package com.example.votingclient.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

class SettingsStore(private val context: Context) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    val darkTheme: Flow<Boolean> = context.settingsDataStore.data.map { it[darkThemeKey] ?: false }

    fun historyFor(userId: String?): Flow<List<String>> {
        if (userId.isNullOrBlank()) return flowOf(emptyList())
        val key = historyKey(userId)
        return context.settingsDataStore.data.map { prefs ->
            prefs[key]
                ?.split("\n")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        }
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun addHistory(userId: String?, text: String) {
        val key = historyKey(userId ?: return)
        val value = text.trim()
        if (value.isBlank()) return
        context.settingsDataStore.edit { prefs ->
            val old = prefs[key]
            ?.split("\n")
            ?.filter { it.isNotBlank() }
            ?: emptyList()
            prefs[key] = (listOf(value) + old.filterNot { it.equals(value, ignoreCase = true) })
                .take(10)
                .joinToString("\n")
        }
    }

    suspend fun clearHistory(userId: String?) {
        val key = historyKey(userId ?: return)
        context.settingsDataStore.edit { it.remove(key) }
    }

    private fun historyKey(userId: String) = stringPreferencesKey("search_history_$userId")
}
