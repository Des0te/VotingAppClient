package com.example.votingclient.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.votingclient.data.model.UserResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore("session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val idKey = stringPreferencesKey("user_id")
    private val nameKey = stringPreferencesKey("user_name")
    private val emailKey = stringPreferencesKey("user_email")
    private val roleKey = stringPreferencesKey("user_role")

    val token: Flow<String?> = context.sessionDataStore.data.map { it[tokenKey] }

    val user: Flow<UserResponse?> = context.sessionDataStore.data.map { prefs ->
        val id = prefs[idKey] ?: return@map null
        UserResponse(
            id = id,
            name = prefs[nameKey].orEmpty(),
            email = prefs[emailKey].orEmpty(),
            role = prefs[roleKey].orEmpty(),
        )
    }

    suspend fun save(token: String, user: UserResponse) {
        context.sessionDataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[idKey] = user.id
            prefs[nameKey] = user.name
            prefs[emailKey] = user.email
            prefs[roleKey] = user.role
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
