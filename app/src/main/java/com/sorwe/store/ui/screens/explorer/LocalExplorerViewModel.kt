package com.sorwe.store.ui.screens.explorer

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.util.ShizukuInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class LocalApkItem(
    val file: File,
    val name: String,
    val packageName: String?,
    val version: String?,
    val size: String,
    val isInstalling: Boolean = false
)

data class LocalExplorerUiState(
    val apks: List<LocalApkItem> = emptyList(),
    val filteredApks: List<LocalApkItem> = emptyList(),
    val searchQuery: String = "",
    val isScanning: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LocalExplorerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocalExplorerUiState())
    val uiState: StateFlow<LocalExplorerUiState> = _uiState.asStateFlow()

    init {
        scanForApks()
    }

    fun scanForApks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true, error = null)
            try {
                val foundApks = withContext(Dispatchers.IO) {
                    val downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val backupFolder = File(context.getExternalFilesDir(null), "backups")
                    
                    val allFiles = mutableListOf<File>()
                    scanDirectory(downloadFolder, allFiles)
                    if (backupFolder.exists()) scanDirectory(backupFolder, allFiles)
                    
                    allFiles.map { file ->
                        val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                        LocalApkItem(
                            file = file,
                            name = packageInfo?.applicationInfo?.loadLabel(context.packageManager)?.toString() ?: file.name,
                            packageName = packageInfo?.packageName,
                            version = packageInfo?.versionName,
                            size = formatSize(file.length())
                        )
                    }
                }
                _uiState.value = _uiState.value.copy(
                    apks = foundApks, 
                    filteredApks = filterApks(foundApks, _uiState.value.searchQuery),
                    isScanning = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isScanning = false, error = e.message)
            }
        }
    }

    private fun filterApks(apks: List<LocalApkItem>, query: String): List<LocalApkItem> {
        if (query.isBlank()) return apks
        return apks.filter { 
            it.name.contains(query, ignoreCase = true) || 
            (it.packageName?.contains(query, ignoreCase = true) == true)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredApks = filterApks(_uiState.value.apks, query)
        )
    }

    private fun scanDirectory(directory: File, resultList: MutableList<File>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                // Optional: scan subdirectories? Limiting to 1 level for performance
                // scanDirectory(file, resultList) 
            } else if (file.name.endsWith(".apk", ignoreCase = true)) {
                resultList.add(file)
            }
        }
    }

    fun installApk(item: LocalApkItem) {
        viewModelScope.launch {
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    item.file
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error starting installation: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun deleteApk(item: LocalApkItem) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (item.file.delete()) {
                    withContext(Dispatchers.Main) {
                        val newAllApks = _uiState.value.apks.filter { it.file != item.file }
                        _uiState.value = _uiState.value.copy(
                            apks = newAllApks,
                            filteredApks = filterApks(newAllApks, _uiState.value.searchQuery)
                        )
                        android.widget.Toast.makeText(context, "Deleted ${item.name}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, "Failed to delete file", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun formatSize(sizeInBytes: Long): String {
        val kb = sizeInBytes / 1024.0
        val mb = kb / 1024.0
        return if (mb > 1) "%.2f MB".format(mb) else "%.2f KB".format(kb)
    }
}
