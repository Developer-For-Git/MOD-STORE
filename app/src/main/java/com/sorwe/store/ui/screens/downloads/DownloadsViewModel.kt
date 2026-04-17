package com.sorwe.store.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sorwe.store.util.DownloadInfo
import com.sorwe.store.util.DownloadManager
import com.sorwe.store.util.DownloadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: DownloadManager
) : ViewModel() {

    // Expose only active or completed downloads (filter out Idle ones unless they failed recently)
    val downloads: StateFlow<List<DownloadInfo>> = downloadManager.allDownloadsFlow
        .map { list ->
            list.filter { it.status !is DownloadStatus.Idle }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun cancelDownload(appName: String) {
        downloadManager.cancelDownload(appName)
    }

    fun removeDownloadRecord(appName: String) {
        downloadManager.resetDownload(appName)
    }
}
