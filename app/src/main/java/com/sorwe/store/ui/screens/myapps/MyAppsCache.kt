package com.sorwe.store.ui.screens.myapps

import android.content.Context
import android.os.Environment
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.data.model.Resource
import com.sorwe.store.data.repository.AppRepository
import com.sorwe.store.util.PackageUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MyAppsCacheEntryPoint {
    fun myAppsCache(): MyAppsCache
}

data class ApkFileInfo(
    val file: File,
    val appName: String,
    val matchedApp: AppItem?
)

data class MyAppsUiState(
    val isLoading: Boolean = true,
    val installedApps: List<AppItem> = emptyList(),
    val updatesAvailable: List<AppItem> = emptyList(),
    val downloadedNotInstalled: List<AppItem> = emptyList(),
    val leftoverApkFiles: List<ApkFileInfo> = emptyList()
)

/**
 * Singleton cache that loads My Apps data once and keeps it in memory.
 * Injected everywhere via Hilt — survives navigation changes.
 */
@Singleton
class MyAppsCache @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AppRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(MyAppsUiState())
    val state: StateFlow<MyAppsUiState> = _state.asStateFlow()

    private var hasLoaded = false

    /** Call once from Application / MainActivity — loads in background */
    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        refresh()
    }

    /** Force reload (pull-to-refresh / manual refresh) */
    fun refresh() {
        scope.launch {
            if (_state.value.installedApps.isEmpty() && _state.value.downloadedNotInstalled.isEmpty() && _state.value.leftoverApkFiles.isEmpty()) {
                _state.update { it.copy(isLoading = true) }
            }

            val allApps = try {
                repository.getApps().first { it is Resource.Success }
                    .let { (it as Resource.Success).data }
            } catch (_: Exception) {
                emptyList()
            }

            val apkNameToApp = allApps.associateBy { app ->
                app.name.replace(Regex("[^a-zA-Z0-9._-]"), "_").lowercase()
            }

            // 1. Installed
            val installed = allApps.filter { app ->
                PackageUtil.isPackageInstalled(context, app.id) ||
                PackageUtil.findPackageNameByLabel(context, app.name) != null
            }

            // 1b. Updates Available
            val updatesAvailable = installed.filter { app ->
                val pkgName = PackageUtil.findPackageNameByLabel(context, app.name) ?: app.id
                val installedVersion = PackageUtil.getPackageVersionName(context, pkgName)
                PackageUtil.isVersionNewer(installedVersion, app.version)
            }

            // 2. APK files on device
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val allApkFiles = downloadsDir?.listFiles { file ->
                file.isFile && file.name.endsWith(".apk", ignoreCase = true) && file.length() > 0
            }?.toList() ?: emptyList()

            val apkFileInfos = allApkFiles.map { file ->
                val stemLower = file.nameWithoutExtension.lowercase()
                val matched = apkNameToApp.entries.find { (key, _) ->
                    stemLower.contains(key) || key.contains(stemLower)
                }?.value
                ApkFileInfo(
                    file = file,
                    appName = matched?.name ?: file.nameWithoutExtension.replace("_", " "),
                    matchedApp = matched
                )
            }

            val installedIds = installed.map { it.id }.toSet()

            // 3. Downloaded but not installed
            val downloadedNotInstalled = apkFileInfos
                .filter { it.matchedApp != null && it.matchedApp.id !in installedIds }
                .mapNotNull { it.matchedApp }
                .distinctBy { it.id }

            _state.update {
                it.copy(
                    isLoading = false,
                    installedApps = installed,
                    updatesAvailable = updatesAvailable,
                    downloadedNotInstalled = downloadedNotInstalled,
                    leftoverApkFiles = apkFileInfos
                )
            }
        }
    }

    fun deleteApkFile(info: ApkFileInfo) {
        scope.launch {
            info.file.delete()
            _state.update { s -> s.copy(leftoverApkFiles = s.leftoverApkFiles - info) }
        }
    }

    fun deleteAllApkFiles() {
        scope.launch {
            _state.value.leftoverApkFiles.forEach { it.file.delete() }
            _state.update { it.copy(leftoverApkFiles = emptyList()) }
        }
    }
}
