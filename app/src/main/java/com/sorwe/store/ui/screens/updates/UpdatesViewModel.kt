package com.sorwe.store.ui.screens.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.data.repository.AppRepository
import com.sorwe.store.util.DownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpdatesUiState(
    val isLoading: Boolean = true,
    val availableUpdates: List<AppUpdateInfo> = emptyList(),
    val error: String? = null,
    // Arch picker state
    val showArchPicker: Boolean = false,
    val archVariants: List<ApkAssetVariant> = emptyList(),
    val isLoadingVariants: Boolean = false,
    val archPickerError: String? = null,
    val currentPickingApp: AppItem? = null
)

data class ApkAssetVariant( // Local redefinition or import if needed, but better to import from remote
    val label: String,
    val fileName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val tagName: String
)

data class AppUpdateInfo(
    val app: AppItem,
    val localVersion: String,
    val remoteVersion: String
)

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val appRepository: AppRepository,
    private val githubMetadataProvider: com.sorwe.store.data.remote.GitHubMetadataProvider,
    val downloadManager: DownloadManager
) : ViewModel() {

    private val prefs: android.content.SharedPreferences = context.getSharedPreferences("app_updates_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(UpdatesUiState())
    val uiState: StateFlow<UpdatesUiState> = _uiState.asStateFlow()

    init {
        checkForUpdates()
    }

    fun showArchitecturePicker(app: AppItem) {
        _uiState.update { it.copy(
            showArchPicker = true, 
            isLoadingVariants = true, 
            archVariants = emptyList(), 
            archPickerError = null,
            currentPickingApp = app
        ) }
        viewModelScope.launch {
            try {
                val repoStr = app.repoUrl.removePrefix("https://github.com/").trimEnd('/')
                val entry = com.sorwe.store.data.remote.AppEntryDto(
                    id = app.id,
                    name = app.name,
                    githubRepo = repoStr,
                    repoUrl = app.repoUrl,
                    releaseKeyword = app.releaseKeyword,
                    downloadUrl = app.downloadUrl,
                    type = "github"
                )
                val variants = githubMetadataProvider.fetchAllApkVariants(entry).map { v ->
                    ApkAssetVariant(
                        label = v.label,
                        fileName = v.fileName,
                        downloadUrl = v.downloadUrl,
                        sizeBytes = v.sizeBytes,
                        tagName = v.tagName
                    )
                }
                
                when {
                    variants.isNotEmpty() -> {
                        _uiState.update { it.copy(archVariants = variants, isLoadingVariants = false, archPickerError = null) }
                    }
                    app.downloadUrl.isNotBlank() && app.downloadUrl != "#" -> {
                        val fallback = listOf(ApkAssetVariant(
                            label = "Direct Download",
                            fileName = app.name + ".apk",
                            downloadUrl = app.downloadUrl,
                            sizeBytes = 0L,
                            tagName = app.version
                        ))
                        _uiState.update { it.copy(archVariants = fallback, isLoadingVariants = false, archPickerError = null) }
                    }
                    else -> {
                        _uiState.update { it.copy(archVariants = emptyList(), isLoadingVariants = false, archPickerError = "No direct download found.") }
                        // Trigger Failed status on the main button
                        downloadManager.setDownloadFailed(app.name)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingVariants = false, archPickerError = "Error: ${e.message}") }
                // Trigger Failed status on the main button
                _uiState.value.currentPickingApp?.name?.let { downloadManager.setDownloadFailed(it) }
            }
        }
    }

    fun dismissArchitecturePicker() {
        _uiState.update { it.copy(showArchPicker = false) }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Get all apps with forced refresh to ensure latest metadata
                val resource = appRepository.getApps(forceRefresh = true)
                    .first { it is com.sorwe.store.data.model.Resource.Success }
                val catalogApps = (resource as? com.sorwe.store.data.model.Resource.Success)?.data ?: emptyList()

                // Filter out apps that are not installed on the device
                val localApps = catalogApps.filter { app ->
                    com.sorwe.store.util.PackageUtil.isPackageInstalled(context, app.id)
                }

                if (localApps.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, availableUpdates = emptyList()) }
                    return@launch
                }

                val updates = mutableListOf<AppUpdateInfo>()
                for (localApp in localApps) {
                    val pInfo = try {
                        context.packageManager.getPackageInfo(localApp.id, 0)
                    } catch (e: Exception) { null }
                    
                    val installedVersion = pInfo?.versionName
                    val lastUpdateTime = pInfo?.lastUpdateTime ?: 0L
                    
                    // Check if we previously marked a pre-install time for this app
                    val preInstallTimeKey = "pre_install_time_${localApp.id}"
                    val preInstallTime = prefs.getLong(preInstallTimeKey, 0L)
                    
                    // If the app was updated since we last marked it
                    if (preInstallTime > 0 && lastUpdateTime > preInstallTime) {
                        prefs.edit()
                            .putString("installed_remote_version_${localApp.id}", localApp.version)
                            .remove(preInstallTimeKey)
                            .apply()
                    }

                    val savedInstalledRemoteVersion = prefs.getString("installed_remote_version_${localApp.id}", null)
                    val ignoredVersion = prefs.getString("ignored_version_${localApp.id}", null)
                    
                    if (com.sorwe.store.util.PackageUtil.isVersionNewer(installedVersion, localApp.version)) {
                        // Skip if it matches the ignored version, or if it's the exact remote version we just installed
                        if (localApp.version != savedInstalledRemoteVersion && localApp.version != ignoredVersion) {
                            updates.add(
                                AppUpdateInfo(
                                    app = localApp,
                                    localVersion = installedVersion ?: "Unknown",
                                    remoteVersion = localApp.version
                                )
                            )
                        }
                    }
                }

                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        availableUpdates = updates.sortedBy { u -> u.app.name }
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to check for updates") }
            }
        }
    }

    fun updateAll() {
        // Enqueue all updates into the DownloadManager
        val updates = _uiState.value.availableUpdates
        updates.forEach { updateInfo ->
            val urlToDownload = updateInfo.app.downloadUrl
            if (urlToDownload.isNotBlank() && urlToDownload != "#") {
                downloadManager.startDownload(updateInfo.app.name, urlToDownload)
            }
        }
    }

    fun getDownloadState(appName: String) = downloadManager.getDownloadState(appName)
    fun startDownload(appName: String, url: String) = downloadManager.startDownload(appName, url)
    fun cancelDownload(appName: String) = downloadManager.cancelDownload(appName)
    fun resetDownload(appName: String) = downloadManager.resetDownload(appName)

    fun markPreInstallTime(packageName: String, time: Long) {
        prefs.edit().putLong("pre_install_time_$packageName", time).apply()
    }
    
    fun ignoreUpdate(packageName: String, version: String) {
        prefs.edit().putString("ignored_version_$packageName", version).apply()
        checkForUpdates()
    }
}
