package com.sorwe.store.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.data.remote.ApkAssetVariant
import com.sorwe.store.data.remote.AppEntryDto
import com.sorwe.store.data.remote.GitHubMetadataProvider
import com.sorwe.store.data.repository.AppRepository
import com.sorwe.store.util.DownloadInfo
import com.sorwe.store.util.DownloadManager
import com.sorwe.store.util.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.sorwe.store.util.PackageUtil
import com.sorwe.store.ui.screens.myapps.MyAppsCache
import kotlinx.coroutines.Dispatchers

data class DetailUiState(
    val app: AppItem? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val isInstalled: Boolean = false,
    val targetPackageName: String = "",
    val apkExists: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val isResolving: Boolean = false,
    // Architecture picker state
    val showArchPicker: Boolean = false,
    val archVariants: List<ApkAssetVariant> = emptyList(),
    val isLoadingVariants: Boolean = false,
    val archPickerError: String? = null
)



@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: AppRepository,
    private val githubMetadataProvider: GitHubMetadataProvider,
    @ApplicationContext private val context: Context,
    val downloadManager: DownloadManager,
    private val myAppsCache: MyAppsCache
) : ViewModel() {

    private val appId: String = savedStateHandle.get<String>("appId") ?: ""

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadApp()
        // React immediately to global app list changes (e.g. from Silent Install refresh)
        viewModelScope.launch {
            myAppsCache.state.collect {
                checkInstallation()
            }
        }
    }

    /**
     * Returns the [StateFlow] of [DownloadInfo] for the current app.
     * UI observes this to show progress / completed / idle state.
     */
    fun getDownloadState(): StateFlow<DownloadInfo> {
        val appName = _uiState.value.app?.name ?: ""
        return downloadManager.getDownloadState(appName)
    }

    /**
     * Starts a download via the application-scoped [DownloadManager].
     * This survives navigation and shows real notifications with progress.
     */
    fun startDownload(url: String) {
        val appName = _uiState.value.app?.name ?: return
        downloadManager.startDownload(appName, url)
    }

    /**
     * Cancels an active download and resets state.
     */
    fun cancelDownload() {
        val appName = _uiState.value.app?.name ?: return
        downloadManager.cancelDownload(appName)
    }

    /**
     * Resets download state to Idle (e.g. after install or if file was deleted).
     */
    fun resetDownload() {
        val appName = _uiState.value.app?.name ?: return
        downloadManager.resetDownload(appName)
    }

    /**
     * Fetches all APK variant download links from GitHub releases and shows the architecture picker dialog.
     * Falls back to the app's existing downloadUrl if the GitHub API returns no APK assets.
     */
    fun showArchitecturePicker() {
        val app = _uiState.value.app ?: return
        _uiState.update { it.copy(showArchPicker = true, isLoadingVariants = true, archVariants = emptyList(), archPickerError = null) }
        viewModelScope.launch {
            try {
                val repoStr = app.repoUrl.removePrefix("https://github.com/").trimEnd('/')
                android.util.Log.d("ArchPicker", "VM: app.id=${app.id} repoUrl=${app.repoUrl} derived_repo=$repoStr keyword=${app.releaseKeyword}")
                val entry = AppEntryDto(
                    id = app.id,
                    name = app.name,
                    githubRepo = repoStr,
                    repoUrl = app.repoUrl,
                    releaseKeyword = app.releaseKeyword,
                    downloadUrl = app.downloadUrl,
                    type = "github"
                )
                val variants = githubMetadataProvider.fetchAllApkVariants(entry)
                when {
                    variants.isNotEmpty() -> {
                        // Happy path: API returned real variants
                        _uiState.update { it.copy(archVariants = variants, isLoadingVariants = false, archPickerError = null) }
                    }
                    app.downloadUrl.isNotBlank() && app.downloadUrl != "#" -> {
                        // Fallback: show the existing resolved download URL as a single option
                        val fallback = listOf(ApkAssetVariant(
                            label = "Direct Download",
                            fileName = app.name + ".apk",
                            downloadUrl = app.downloadUrl,
                            sizeBytes = 0L,
                            tagName = app.version
                        ))
                        android.util.Log.d("ArchPicker", "No API variants found, using fallback downloadUrl=${app.downloadUrl}")
                        _uiState.update { it.copy(archVariants = fallback, isLoadingVariants = false, archPickerError = null) }
                    }
                    else -> {
                        // No info at all — show the repo link
                        _uiState.update { it.copy(archVariants = emptyList(), isLoadingVariants = false, archPickerError = "No direct download found.\nTry opening the repository page.") }
                        // Trigger Failed status on the main button so "Open Repo" appears
                        downloadManager.setDownloadFailed(app.name)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ArchPicker", "showArchitecturePicker failed", e)
                // Even on exception, try the existing download URL
                val app2 = _uiState.value.app
                if (app2 != null && app2.downloadUrl.isNotBlank() && app2.downloadUrl != "#") {
                    val fallback = listOf(ApkAssetVariant(
                        label = "Direct Download",
                        fileName = app2.name + ".apk",
                        downloadUrl = app2.downloadUrl,
                        sizeBytes = 0L,
                        tagName = app2.version
                    ))
                    _uiState.update { it.copy(archVariants = fallback, isLoadingVariants = false, archPickerError = null) }
                } else {
                    _uiState.update { it.copy(isLoadingVariants = false, archPickerError = "Error: ${e.javaClass.simpleName}: ${e.message}") }
                    // Trigger Failed status on the main button
                    _uiState.value.app?.name?.let { downloadManager.setDownloadFailed(it) }
                }
            }
        }
    }

    fun dismissArchitecturePicker() {
        _uiState.update { it.copy(showArchPicker = false) }
    }

    private fun loadApp() {
        viewModelScope.launch {
            // Load favorite status asynchronously to avoid blocking the initial UI render
            launch {
                val isFav = repository.isFavorite(appId)
                _uiState.update { it.copy(isFavorite = isFav) }
            }

            var isFirstEmission = true
            _uiState.update { it.copy(isResolving = true) }
            repository.getAppWithResolution(appId).collect { app ->
                val needsResolution = app != null && (app.downloadUrl.isBlank() || app.downloadUrl == "#")
                
                // Keep resolving state only if we're on the first emission and it's missing a link.
                // Once we get the second emission (resolved) or if it already had a link, stop resolving.
                val resolvingState = isFirstEmission && needsResolution

                _uiState.update {
                    it.copy(
                        app = app,
                        isLoading = false,
                        isResolving = resolvingState
                    )
                }
                if (app != null) {
                    checkInstallation(app)
                }
                isFirstEmission = false
            }
        }
    }
    
    fun checkInstallation(app: AppItem? = _uiState.value.app) {
        val currentApp = app ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val isPkgInstalled = PackageUtil.isPackageInstalled(context, currentApp.id)
            val apkFile = com.sorwe.store.util.DownloadUtil.findDownloadedApk(context, currentApp.name)
            val apkExists = apkFile != null && apkFile.exists()

            if (isPkgInstalled) {
                val installedVersion = PackageUtil.getPackageVersionName(context, currentApp.id)
                val isUpdate = PackageUtil.isVersionNewer(installedVersion, currentApp.version)
                _uiState.update { 
                    it.copy(isInstalled = true, targetPackageName = currentApp.id, apkExists = apkExists, isUpdateAvailable = isUpdate) 
                }
            } else {
                var fallback: String? = null
                
                // 1. Try resolving package name natively from the downloaded APK file
                if (apkExists && apkFile != null) {
                    try {
                        val pInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
                        val apkPkgName = pInfo?.packageName
                        if (apkPkgName != null && PackageUtil.isPackageInstalled(context, apkPkgName)) {
                            fallback = apkPkgName
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DetailViewModel", "Failed to parse APK package name", e)
                    }
                }
                
                // 2. Try matching by label (fuzzy match)
                if (fallback == null) {
                    fallback = PackageUtil.findPackageNameByLabel(context, currentApp.name)
                }

                if (fallback != null) {
                    val installedVersion = PackageUtil.getPackageVersionName(context, fallback)
                    val isUpdate = PackageUtil.isVersionNewer(installedVersion, currentApp.version)
                    _uiState.update { 
                        it.copy(isInstalled = true, targetPackageName = fallback, apkExists = apkExists, isUpdateAvailable = isUpdate) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(isInstalled = false, targetPackageName = currentApp.id, apkExists = apkExists, isUpdateAvailable = false) 
                    }
                }
            }
        }
    }

    fun deleteApk() {
        val appName = _uiState.value.app?.name ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val file = com.sorwe.store.util.DownloadUtil.findDownloadedApk(context, appName)
            if (file != null && file.exists()) {
                file.delete()
            }
            _uiState.update { it.copy(apkExists = false) }
            resetDownload()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavorite(appId)
            val isFav = repository.isFavorite(appId)
            _uiState.update { it.copy(isFavorite = isFav) }
        }
    }
}
