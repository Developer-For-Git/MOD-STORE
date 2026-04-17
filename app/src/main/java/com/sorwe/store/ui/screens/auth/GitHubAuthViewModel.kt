package com.sorwe.store.ui.screens.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.data.preferences.UserPreferences
import com.sorwe.store.data.remote.GitHubAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class GitHubAuthState {
    object Idle : GitHubAuthState()
    object Loading : GitHubAuthState()
    data class Success(val userName: String) : GitHubAuthState()
    data class Error(val message: String) : GitHubAuthState()
}

@HiltViewModel
class GitHubAuthViewModel @Inject constructor(
    private val authService: GitHubAuthService,
    private val userPreferences: UserPreferences,
    private val moshi: com.squareup.moshi.Moshi
) : ViewModel() {

    // IMPORTANT: User should replace these with their own credentials from GitHub OAuth App
    private val CLIENT_ID = "Ov23liyN662L6qDRWe2o" 
    private val CLIENT_SECRET = "4363d65eb489e2faeb9cf05ca17e47d562fcea33"
    private val REDIRECT_URI = "sorwestore://oauth"

    private val _authState = MutableStateFlow<GitHubAuthState>(GitHubAuthState.Idle)
    val authState: StateFlow<GitHubAuthState> = _authState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferences.githubUserName.collect { name ->
                if (!name.isNullOrEmpty()) {
                    _authState.value = GitHubAuthState.Success(name)
                }
            }
        }
    }

    fun initiateLogin(context: Context) {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/login/oauth/authorize?client_id=$CLIENT_ID&scope=user&redirect_uri=$REDIRECT_URI")
        )
        context.startActivity(intent)
    }

    fun handleAuthCode(code: String) {
        viewModelScope.launch {
            _authState.value = GitHubAuthState.Loading
            try {
                // 1. Exchange code for access token
                val response = authService.getAccessToken(
                    clientId = CLIENT_ID,
                    clientSecret = CLIENT_SECRET,
                    code = code,
                    redirectUri = REDIRECT_URI
                )
                
                if (!response.isSuccessful) {
                    throw Exception("[v6] GitHub Server Error: ${response.code()}")
                }
                
                val rawResponse = response.body()?.string() ?: throw Exception("[v6] Empty response from GitHub")
                
                // Try JSON parsing first, then fallback to form-data
                val tokenResponse = try {
                    moshi.adapter(com.sorwe.store.data.remote.GitHubTokenResponse::class.java)
                        .lenient()
                        .fromJson(rawResponse)
                } catch (e: Exception) {
                    // Fallback to manual form-data parsing
                    parseFormEncoded(rawResponse)
                } ?: throw Exception("[v6] Failed to parse token response. Raw: ${rawResponse.take(50)}")

                val token = tokenResponse.accessToken
                
                if (token == null) {
                    val errorMsg = tokenResponse.errorDescription ?: tokenResponse.error ?: "Failed to get access token"
                    throw Exception("[v6] $errorMsg. Raw: ${rawResponse.take(50)}")
                }
                
                // 2. Fetch user profile to verify token and get user info
                val profileResponse = authService.getUserProfile("Bearer $token")
                if (!profileResponse.isSuccessful) {
                    throw Exception("[v6] Profile Error: ${profileResponse.code()}")
                }
                
                val rawProfile = profileResponse.body()?.string() ?: throw Exception("[v6] Empty profile response")
                val userProfile = try {
                    moshi.adapter(com.sorwe.store.data.remote.GitHubUserResponse::class.java)
                        .lenient()
                        .fromJson(rawProfile)
                } catch (e: Exception) {
                    null
                } ?: throw Exception("[v6] Failed to parse profile. Raw: ${rawProfile.take(50)}")
                
                // 3. Save to preferences
                userPreferences.setGitHubAuthData(
                    token = token,
                    name = userProfile.name ?: userProfile.login,
                    avatar = userProfile.avatarUrl
                )
                
                _authState.value = GitHubAuthState.Success(userProfile.login)
            } catch (e: Exception) {
                _authState.value = GitHubAuthState.Error(e.message ?: "[v5] Authentication failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userPreferences.clearGitHubAuthData()
            _authState.value = GitHubAuthState.Idle
        }
    }

    private fun parseFormEncoded(raw: String): com.sorwe.store.data.remote.GitHubTokenResponse {
        val params = raw.split("&").associate { 
            val parts = it.split("=", limit = 2)
            parts.getOrNull(0) to parts.getOrNull(1)
        }
        return com.sorwe.store.data.remote.GitHubTokenResponse(
            accessToken = params["access_token"],
            tokenType = params["token_type"],
            scope = params["scope"],
            error = params["error"],
            errorDescription = params["error_description"]
        )
    }
}
