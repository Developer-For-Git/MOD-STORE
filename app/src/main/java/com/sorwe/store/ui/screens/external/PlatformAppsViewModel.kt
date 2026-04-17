package com.sorwe.store.ui.screens.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.data.model.Resource
import com.sorwe.store.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PlatformAppsUiState(
    val apps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val favoriteIds: Set<String> = emptySet(),
    val sortOption: com.sorwe.store.ui.screens.home.SortOption = com.sorwe.store.ui.screens.home.SortOption.RECENTLY_ADDED
)

@HiltViewModel
class PlatformAppsViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    // Set by the composable before loading starts
    private var platform: String = ""
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(PlatformAppsUiState())
    val uiState: StateFlow<PlatformAppsUiState> = _uiState.asStateFlow()

    /**
     * Initialize with the platform type. Called once from the composable.
     * This replaces SavedStateHandle since we use separate routes.
     */
    fun init(platformType: String) {
        if (platform == platformType) return // already initialized
        platform = platformType
        loadApps()
        observeFavorites()
    }

    fun loadApps(forceRefresh: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repository.getApps(forceRefresh).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        // Filter by the platform field from apps.json
                        val platformName = when (platform) {
                            "pc" -> "PC"
                            "tv" -> "TV"
                            else -> ""
                        }
                        // Reverse so the last entry in JSON (most recently added) shows first
                        val apps = resource.data
                            .filter { it.platform.equals(platformName, ignoreCase = true) }
                            .reversed()
                        val currentState = _uiState.value

                        val (categories, filtered) = withContext(Dispatchers.Default) {
                            val cats = apps.map { it.category }.distinct().sorted()
                            val filt = applyFilter(apps, currentState.selectedCategory, currentState.searchQuery, currentState.sortOption)
                            Pair(cats, filt)
                        }

                        _uiState.update {
                            it.copy(
                                apps = apps,
                                categories = categories,
                                filteredApps = filtered,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Resource.Error -> {
                        _uiState.update {
                            it.copy(isLoading = false, error = resource.message)
                        }
                    }
                }
            }
        }
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getFavoriteApps().collect { favorites ->
                val ids = favorites.map { it.id }.toSet()
                _uiState.update { it.copy(favoriteIds = ids) }
            }
        }
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        refilter()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        refilter()
    }

    fun toggleFavorite(appId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(appId)
        }
    }

    fun refresh() {
        loadApps(forceRefresh = true)
    }

    fun onSortOptionSelected(sortOption: com.sorwe.store.ui.screens.home.SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        refilter()
    }

    private fun refilter() {
        viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val filtered = applyFilter(state.apps, state.selectedCategory, state.searchQuery, state.sortOption)
            _uiState.update { it.copy(filteredApps = filtered) }
        }
    }

    private fun parseSizeToBytes(size: String): Long {
        val lower = size.lowercase().trim()
        if (lower == "varies" || lower == "unknown") return Long.MAX_VALUE

        val regex = Regex("""([\d.]+)\s*(kb|mb|gb|b)""")
        val match = regex.find(lower) ?: return Long.MAX_VALUE
        val value = match.groupValues[1].toDoubleOrNull() ?: return Long.MAX_VALUE
        val unit = match.groupValues[2]

        return when (unit) {
            "b" -> value.toLong()
            "kb" -> (value * 1024).toLong()
            "mb" -> (value * 1024 * 1024).toLong()
            "gb" -> (value * 1024 * 1024 * 1024).toLong()
            else -> Long.MAX_VALUE
        }
    }

    private fun applyFilter(apps: List<AppItem>, category: String?, query: String, sortOption: com.sorwe.store.ui.screens.home.SortOption): List<AppItem> {
        var result = apps
        if (category != null) {
            result = result.filter { it.category == category }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.developer.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }
        
        return when (sortOption) {
            com.sorwe.store.ui.screens.home.SortOption.RECENTLY_ADDED -> result
            com.sorwe.store.ui.screens.home.SortOption.OLDEST_ADDED -> result.reversed()
            com.sorwe.store.ui.screens.home.SortOption.NAME_AZ -> result.sortedBy { it.name.lowercase() }
            com.sorwe.store.ui.screens.home.SortOption.NAME_ZA -> result.sortedByDescending { it.name.lowercase() }
            com.sorwe.store.ui.screens.home.SortOption.SIZE_SMALLEST -> result.sortedBy { parseSizeToBytes(it.size) }
            com.sorwe.store.ui.screens.home.SortOption.SIZE_LARGEST -> result.sortedByDescending { parseSizeToBytes(it.size) }
        }
    }
}
