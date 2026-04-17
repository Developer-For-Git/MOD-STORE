package com.sorwe.store.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sorwe_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        private val JSON_URL_KEY = stringPreferencesKey("json_url")
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val IS_LOGGED_IN_KEY = booleanPreferencesKey("is_logged_in")
        private val USER_NAME_KEY = stringPreferencesKey("user_name")
        private val USER_AVATAR_KEY = stringPreferencesKey("user_avatar") // URI or emoji
        private val AUTO_UPDATE_KEY = booleanPreferencesKey("auto_update_enabled")
        private val SEARCH_HISTORY_KEY = stringPreferencesKey("search_history")
        private val CUSTOM_ACCENT_KEY = stringPreferencesKey("custom_accent_color")
        private val BIOMETRIC_LOCK_KEY = booleanPreferencesKey("biometric_lock_enabled")
        private val SHIZUKU_INSTALLER_KEY = booleanPreferencesKey("shizuku_installer_enabled")
        private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        private val GITHUB_ACCESS_TOKEN_KEY = stringPreferencesKey("github_access_token")
        private val GITHUB_USER_NAME_KEY = stringPreferencesKey("github_user_name")
        private val GITHUB_USER_AVATAR_KEY = stringPreferencesKey("github_user_avatar")

        const val DEFAULT_JSON_URL =
            "https://raw.githubusercontent.com/Developer-For-Git/MOD-STORE-DATA-/refs/heads/main/apps.json"
    }

    val jsonUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[JSON_URL_KEY] ?: DEFAULT_JSON_URL
    }

    val isDarkMode: Flow<Boolean> = kotlinx.coroutines.flow.flowOf(true)

    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[IS_LOGGED_IN_KEY] ?: false
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_NAME_KEY] ?: ""
    }

    val userAvatar: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_AVATAR_KEY] ?: "😊"
    }

    val customAccentColor: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[CUSTOM_ACCENT_KEY] ?: "#FF3B5C"
    }

    val isAutoUpdateEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[AUTO_UPDATE_KEY] ?: true
    }

    val isBiometricLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_LOCK_KEY] ?: false
    }

    suspend fun setCustomAccentColor(hex: String) {
        context.dataStore.edit { prefs ->
            prefs[CUSTOM_ACCENT_KEY] = hex
        }
    }

    suspend fun setJsonUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[JSON_URL_KEY] = url
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        // Dark mode is now permanent. This is a no-op.
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[IS_LOGGED_IN_KEY] = loggedIn
        }
    }

    suspend fun setAutoUpdateEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[AUTO_UPDATE_KEY] = enabled
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[BIOMETRIC_LOCK_KEY] = enabled
        }
    }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
        }
    }

    suspend fun setUserAvatar(avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_AVATAR_KEY] = avatar
        }
    }

    val searchHistory: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val historyStr = prefs[SEARCH_HISTORY_KEY] ?: ""
        if (historyStr.isBlank()) emptyList() else historyStr.split(",")
    }

    val isShizukuInstallerEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SHIZUKU_INSTALLER_KEY] ?: false
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETED_KEY] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    suspend fun setShizukuInstallerEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHIZUKU_INSTALLER_KEY] = enabled
        }
    }

    val githubAccessToken: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[GITHUB_ACCESS_TOKEN_KEY]
    }

    val githubUserName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[GITHUB_USER_NAME_KEY] ?: ""
    }

    val githubUserAvatar: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[GITHUB_USER_AVATAR_KEY]
    }

    suspend fun setGitHubAuthData(token: String, name: String, avatar: String) {
        context.dataStore.edit { prefs ->
            prefs[GITHUB_ACCESS_TOKEN_KEY] = token
            prefs[GITHUB_USER_NAME_KEY] = name
            prefs[GITHUB_USER_AVATAR_KEY] = avatar
        }
    }

    suspend fun clearGitHubAuthData() {
        context.dataStore.edit { prefs ->
            prefs.remove(GITHUB_ACCESS_TOKEN_KEY)
            prefs.remove(GITHUB_USER_NAME_KEY)
            prefs.remove(GITHUB_USER_AVATAR_KEY)
        }
    }

    suspend fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        context.dataStore.edit { prefs ->
            val currentStr = prefs[SEARCH_HISTORY_KEY] ?: ""
            val currentList = if (currentStr.isBlank()) emptyList() else currentStr.split(",")
            val newList = (listOf(query.trim()) + currentList.filter { it.equals(query.trim(), ignoreCase = true).not() }).take(5)
            prefs[SEARCH_HISTORY_KEY] = newList.joinToString(",")
        }
    }
}
