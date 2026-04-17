package com.sorwe.store.ui.screens.myapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.util.AppBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.sorwe.store.util.DownloadManager
import javax.inject.Inject

@HiltViewModel
class MyAppsViewModel @Inject constructor(
    private val cache: MyAppsCache,
    private val appBackupManager: AppBackupManager,
    private val downloadManager: DownloadManager
) : ViewModel() {

    val uiState: StateFlow<MyAppsUiState> = cache.state

    fun load() = cache.refresh()
    fun deleteApkFile(info: ApkFileInfo) = cache.deleteApkFile(info)
    fun deleteAllApkFiles() = cache.deleteAllApkFiles()

    fun updateAll(context: android.content.Context) {
        viewModelScope.launch {
            val updates = uiState.value.updatesAvailable
            if (updates.isEmpty()) return@launch

            android.widget.Toast.makeText(context, "Queued ${updates.size} updates into Download Manager", android.widget.Toast.LENGTH_SHORT).show()
            updates.forEach { app ->
                downloadManager.startDownload(app.name, app.downloadUrl)
            }
        }
    }

    fun backupApk(context: android.content.Context, packageName: String, appName: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                val sourceFile = java.io.File(appInfo.sourceDir)
                if (sourceFile.exists()) {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val backupDir = java.io.File(downloadsDir, "MODstore_Backups")
                    if (!backupDir.exists()) backupDir.mkdirs()
                    
                    val destFile = java.io.File(backupDir, "${appName.replace(" ", "_")}_backup.apk")
                    sourceFile.copyTo(destFile, overwrite = true)
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "APK Backed up to Downloads/MODstore_Backups", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Failed to backup APK: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportApps(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                val installedApps = cache.state.value.installedApps
                val downloadedApps = cache.state.value.downloadedNotInstalled
                val allAppsToExport = installedApps + downloadedApps

                context.contentResolver.openOutputStream(uri)?.let { outputStream ->
                    appBackupManager.exportApps(allAppsToExport, outputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importApps(context: android.content.Context, uri: android.net.Uri, onAppsImported: (List<com.sorwe.store.util.AppBackupItem>) -> Unit) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.let { inputStream ->
                    val importedApps = appBackupManager.importApps(inputStream)
                    onAppsImported(importedApps)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
