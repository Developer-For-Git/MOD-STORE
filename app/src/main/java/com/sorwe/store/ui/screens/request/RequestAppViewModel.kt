package com.sorwe.store.ui.screens.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.BuildConfig
import com.sorwe.store.data.remote.GitHubIssueService
import com.sorwe.store.data.remote.IssueRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RequestAppViewModel @Inject constructor(
    private val issueService: GitHubIssueService
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _submitResult = MutableStateFlow<Boolean?>(null)
    val submitResult: StateFlow<Boolean?> = _submitResult.asStateFlow()

    fun resetState() {
        _submitResult.value = null
    }

    fun submitRequest(appName: String, playStoreLink: String, reason: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val token = BuildConfig.GITHUB_TOKEN
                if (token.isBlank() || token.contains("paste_your_token_here")) {
                    _submitResult.value = false
                    return@launch
                }

                val title = "App Request: $appName"
                val body = """
                    ### App Name
                    $appName
                    
                    ### Play Store / Source Link
                    $playStoreLink
                    
                    ### Note / Reason
                    ${reason.ifBlank { "No extra details provided." }}
                """.trimIndent()

                val request = IssueRequest(title, body)
                
                // Using the specific owner and repo from the user's data branch
                val response = issueService.createIssue(
                    owner = "Developer-For-Git",
                    repo = "MOD-STORE-DATA-",
                    authHeader = "token $token",
                    request = request
                )
                
                _submitResult.value = response.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
                _submitResult.value = false
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
