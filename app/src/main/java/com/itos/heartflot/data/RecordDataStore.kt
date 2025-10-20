package com.itos.heartflot.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "heart_rate_records")

class RecordDataStore(private val context: Context) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    companion object {
        private val SESSIONS_KEY = stringPreferencesKey("sessions")
    }
    
    val sessions: Flow<List<RecordSession>> = context.dataStore.data.map { preferences ->
        val jsonString = preferences[SESSIONS_KEY] ?: "[]"
        try {
            json.decodeFromString<List<RecordSession>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun saveSessions(sessions: List<RecordSession>) {
        context.dataStore.edit { preferences ->
            preferences[SESSIONS_KEY] = json.encodeToString(sessions)
        }
    }
    
    suspend fun addSession(session: RecordSession) {
        context.dataStore.edit { preferences ->
            val current = try {
                val jsonString = preferences[SESSIONS_KEY] ?: "[]"
                json.decodeFromString<List<RecordSession>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
            
            val updated = current.toMutableList().apply {
                add(0, session) // 新会话添加到最前面
            }
            
            preferences[SESSIONS_KEY] = json.encodeToString(updated)
        }
    }
    
    suspend fun updateSession(sessionId: String, updater: (RecordSession) -> RecordSession) {
        context.dataStore.edit { preferences ->
            val current = try {
                val jsonString = preferences[SESSIONS_KEY] ?: "[]"
                json.decodeFromString<List<RecordSession>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
            
            val updated = current.map { session ->
                if (session.sessionId == sessionId) {
                    updater(session)
                } else {
                    session
                }
            }
            
            preferences[SESSIONS_KEY] = json.encodeToString(updated)
        }
    }
    
    suspend fun deleteSession(sessionId: String) {
        context.dataStore.edit { preferences ->
            val current = try {
                val jsonString = preferences[SESSIONS_KEY] ?: "[]"
                json.decodeFromString<List<RecordSession>>(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
            
            val updated = current.filter { it.sessionId != sessionId }
            preferences[SESSIONS_KEY] = json.encodeToString(updated)
        }
    }
    
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences[SESSIONS_KEY] = "[]"
        }
    }
}

