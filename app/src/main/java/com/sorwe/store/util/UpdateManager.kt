package com.sorwe.store.util

import android.util.Log
import com.sorwe.store.BuildConfig
import com.sorwe.store.data.remote.AppApiService
import com.sorwe.store.data.remote.ReleaseNotesDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val releaseNotes: ReleaseNotesDto, val isForceUpdate: Boolean) : UpdateState()
    object UpToDate : UpdateState()
    data class Error(val message: String) : UpdateState()
}

@Singleton
class UpdateManager @Inject constructor(
    private val apiService: AppApiService
) {
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private var cachedReleaseNotes: ReleaseNotesDto? = null

    suspend fun checkForUpdates() {
        val currentState = _updateState.value
        if (currentState is UpdateState.UpdateAvailable || currentState is UpdateState.UpToDate) {
            return // Skip redundant checks in the same session
        }
        
        _updateState.value = UpdateState.Checking
        
        try {
            val url = "https://raw.githubusercontent.com/Developer-For-Git/MOD-STORE-DATA-/refs/heads/main/updates.json"
            val releaseNotes = apiService.getLatestReleaseNotes(url)
            cachedReleaseNotes = releaseNotes
            
            val currentVersion = BuildConfig.VERSION_NAME
            val newVersion = releaseNotes.version
            
            Log.d("UpdateManager", "Current version: $currentVersion, New version: $newVersion")
            
            if (isNewerVersion(currentVersion, newVersion)) {
                // If version difference is "greater than current version" implies force update.
                // Or simply any update is forced as per user request (no later button).
                // "If the version difference is greater than the current version" usually means 
                // something like a major version change, but the user says "the user must NOT continue using the app".
                val isForce = true // Forcing all newer versions as per Requirement 3
                _updateState.value = UpdateState.UpdateAvailable(releaseNotes, isForce)
            } else {
                _updateState.value = UpdateState.UpToDate
            }
        } catch (e: Exception) {
            Log.e("UpdateManager", "Update check failed", e)
            _updateState.value = UpdateState.Error(e.localizedMessage ?: "Unknown error")
            // Requirement 6: App should continue normally if fetch fails
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val s1 = sanitizeVersion(current)
            val s2 = sanitizeVersion(latest)
            
            if (s1 == s2) return false
            
            val currentParts = s1.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = s2.split(".").map { it.toIntOrNull() ?: 0 }
            
            for (i in 0 until maxOf(currentParts.size, latestParts.size)) {
                val curr = currentParts.getOrElse(i) { 0 }
                val late = latestParts.getOrElse(i) { 0 }
                if (late > curr) return true
                if (curr > late) return false
            }
            false
        } catch (e: Exception) {
            latest != current
        }
    }

    private fun sanitizeVersion(v: String): String {
        return v.lowercase()
            .removePrefix("v")
            .filter { it.isDigit() || it == '.' }
            .trim()
    }
}
