package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "edulens_user_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_STUDY_STREAK = intPreferencesKey("study_streak")
        val KEY_LAST_STUDY_DATE = stringPreferencesKey("last_study_date")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val KEY_MEMBER_SINCE = stringPreferencesKey("member_since")
    }

    val studyStreak: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEY_STUDY_STREAK] ?: 0
    }

    val lastStudyDate: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_STUDY_DATE]
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_THEME_MODE] ?: "system"
    }

    val selectedLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SELECTED_LANGUAGE] ?: "en"
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL]
    }

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_LOGGED_IN] ?: false
    }

    val memberSince: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_MEMBER_SINCE] ?: "June 2026"
    }

    suspend fun saveLoginState(email: String, isLoggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_EMAIL] = email
            preferences[KEY_IS_LOGGED_IN] = isLoggedIn
            if (isLoggedIn && preferences[KEY_MEMBER_SINCE] == null) {
                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                preferences[KEY_MEMBER_SINCE] = sdf.format(Date())
            }
        }
    }

    suspend fun updateTheme(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode
        }
    }

    suspend fun updateLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SELECTED_LANGUAGE] = lang
        }
    }

    suspend fun incrementStreak() {
        context.dataStore.edit { preferences ->
            val currentStreak = preferences[KEY_STUDY_STREAK] ?: 0
            val lastDateStr = preferences[KEY_LAST_STUDY_DATE]
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (lastDateStr != todayStr) {
                if (lastDateStr == null) {
                    preferences[KEY_STUDY_STREAK] = 1
                } else {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    try {
                        val lastDate = sdf.parse(lastDateStr)
                        val today = sdf.parse(todayStr)
                        val diff = today.time - lastDate.time
                        val diffDays = diff / (1000 * 60 * 60 * 24)
                        if (diffDays == 1L) {
                            preferences[KEY_STUDY_STREAK] = currentStreak + 1
                        } else if (diffDays > 1L) {
                            preferences[KEY_STUDY_STREAK] = 1
                        }
                    } catch (e: Exception) {
                        preferences[KEY_STUDY_STREAK] = 1
                    }
                }
                preferences[KEY_LAST_STUDY_DATE] = todayStr
            }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
