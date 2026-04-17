package com.sorwe.store.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.data.model.Resource
import com.sorwe.store.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class SortOption(val label: String) {
    RECENTLY_ADDED("Recently Added"),
    OLDEST_ADDED("Oldest Added"),
    NAME_AZ("Name (A-Z)"),
    NAME_ZA("Name (Z-A)"),
    SIZE_SMALLEST("Size (Smallest)"),
    SIZE_LARGEST("Size (Largest)")
}


data class HomeUiState(
    val allApps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val featuredApps: List<AppItem> = emptyList(),
    val categories: List<String> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val isGridView: Boolean = true,
    val favoriteIds: Set<String> = emptySet(),
    val sortOption: SortOption = SortOption.RECENTLY_ADDED
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: AppRepository,
    private val updateManager: com.sorwe.store.util.UpdateManager,
    private val downloadManager: com.sorwe.store.util.DownloadManager
) : ViewModel() {

    private var filterJob: Job? = null
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val updateState = updateManager.updateState

    init {
        loadApps()
        observeFavorites()
        checkUpdates()
    }


    private fun checkUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdates()
        }
    }

    /**
     * Starts the internal update download for the MOD Store app.
     */
    fun startUpdate(url: String) {
        // Use "MOD Store" as the identifier for the app's own updates
        downloadManager.startDownload("MOD Store", url)
    }

    /**
     * Returns the download state for the MOD Store update.
     */
    fun getUpdateDownloadState(): kotlinx.coroutines.flow.StateFlow<com.sorwe.store.util.DownloadInfo> {
        return downloadManager.getDownloadState("MOD Store")
    }

    fun loadApps(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getApps(forceRefresh).collect { resource ->
                when (resource) {
                    is Resource.Loading -> {
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    }
                    is Resource.Success -> {
                        // Filter by platform field — Home shows only Android apps
                        Log.d("HomeViewModel", "Received ${resource.data.size} apps from repository")
                        val apps = resource.data.filter { it.platform.equals("Android", ignoreCase = true) }
                        Log.d("HomeViewModel", "Filtered to ${apps.size} Android apps")
                        
                        val (categories, featured) = withContext(Dispatchers.Default) {
                            val cats = apps.map { it.category }.distinct().sorted()
                            val feats = apps.filter { it.featured }
                            Pair(cats, feats)
                        }
                        
                        // Update the master list first
                        _uiState.update {
                            it.copy(
                                allApps = apps,
                                categories = categories,
                                featuredApps = featured,
                                isLoading = false,
                                error = null
                            )
                        }
                        
                        // Now apply filters using the LATEST state (including current searchQuery)
                        applyFilters()
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

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    fun onSortOptionSelected(sortOption: SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
        applyFilters()
    }


    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleFavorite(appId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(appId)
        }
    }

    fun refresh() {
        loadApps(forceRefresh = true)
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

    private fun applyFilters() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch(Dispatchers.Default) {
            val state = _uiState.value
            val filtered = filterAppsList(
                allApps = state.allApps,
                category = state.selectedCategory,
                query = state.searchQuery,
                sortOption = state.sortOption
            )

            _uiState.update { it.copy(filteredApps = filtered) }
        }
    }

    private fun filterAppsList(
        allApps: List<AppItem>,
        category: String?,
        query: String,
        sortOption: SortOption
    ): List<AppItem> {
        var filtered = allApps


        // Category filter
        if (category != null) {
            filtered = filtered.filter { it.category == category }
        }

        // Search filter
        if (query.isNotBlank()) {
            val q = query.lowercase()
            filtered = filtered.filter {
                it.name.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.developer.lowercase().contains(q) ||
                it.category.lowercase().contains(q)
            }
        }

        // Sort
        return when (sortOption) {
            SortOption.RECENTLY_ADDED -> filtered.sortedByDescending { it.addedAt }
            SortOption.OLDEST_ADDED -> filtered.sortedBy { it.addedAt }
            SortOption.NAME_AZ -> filtered.sortedBy { it.name.lowercase() }
            SortOption.NAME_ZA -> filtered.sortedByDescending { it.name.lowercase() }
            SortOption.SIZE_SMALLEST -> filtered.sortedBy { parseSizeToBytes(it.size) }
            SortOption.SIZE_LARGEST -> filtered.sortedByDescending { parseSizeToBytes(it.size) }
        }
    }
}
