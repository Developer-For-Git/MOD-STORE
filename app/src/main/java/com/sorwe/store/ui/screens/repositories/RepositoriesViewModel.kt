package com.sorwe.store.ui.screens.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.data.local.RepoDao
import com.sorwe.store.data.local.RepoSourceEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RepositoriesUiState(
    val repos: List<RepoSourceEntity> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RepositoriesViewModel @Inject constructor(
    private val repoDao: RepoDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepositoriesUiState())
    val uiState: StateFlow<RepositoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repoDao.getAllReposFlow().collect { reposList ->
                _uiState.value = RepositoriesUiState(repos = reposList, isLoading = false)
            }
        }
    }

    fun addRepo(name: String, url: String) {
        viewModelScope.launch {
            repoDao.insertRepo(RepoSourceEntity(url = url, name = name, isEnabled = true))
        }
    }

    fun toggleRepo(repo: RepoSourceEntity, isEnabled: Boolean) {
        if (repo.url == "https://raw.githubusercontent.com/Developer-For-Git/MOD-STORE-DATA-/refs/heads/main/apps.json") return
        viewModelScope.launch {
            repoDao.insertRepo(repo.copy(isEnabled = isEnabled))
        }
    }

    fun deleteRepo(repo: RepoSourceEntity) {
        if (repo.url == "https://raw.githubusercontent.com/Developer-For-Git/MOD-STORE-DATA-/refs/heads/main/apps.json") return
        viewModelScope.launch {
            repoDao.deleteRepo(repo)
        }
    }
}
