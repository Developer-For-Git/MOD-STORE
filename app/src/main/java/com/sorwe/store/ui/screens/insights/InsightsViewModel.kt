package com.sorwe.store.ui.screens.insights

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.data.local.AppDao
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val mostUpdatedApps: List<AppItem> = emptyList(),
    val newestApps: List<AppItem> = emptyList(),
    val totalStorageSavedMB: Float = 0f,
    val totalModsCount: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository,
    private val appDao: AppDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    private fun loadInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val appsFlow = appDao.getAllApps()
            val mostUpdatedFlow = appDao.getMostUpdatedApps(10)
                .map { entities -> entities.map { it.toAppItem() } }
            
            val newestAppsFlow = appDao.getNewestApps(10)
                .map { entities -> entities.map { it.toAppItem() } }

            combine(appsFlow, mostUpdatedFlow, newestAppsFlow) { allApps, mostUpdated, newest ->
                val totalMB = allApps.sumOf { parseSizeToMB(it.size).toDouble() }.toFloat()
                // Estimate 35% savings as a realistic placeholder for MOD efficiency
                val savedMB = totalMB * 0.35f 
                
                InsightsUiState(
                    mostUpdatedApps = mostUpdated,
                    newestApps = newest,
                    totalStorageSavedMB = savedMB,
                    totalModsCount = allApps.size,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun parseSizeToMB(sizeStr: String): Float {
        return try {
            val upper = sizeStr.uppercase()
            val value = upper.filter { it.isDigit() || it == '.' }.toFloatOrNull() ?: 0f
            when {
                upper.contains("GB") -> value * 1024f
                upper.contains("KB") -> value / 1024f
                else -> value // Assume MB
            }
        } catch (e: Exception) {
            0f
        }
    }
}
