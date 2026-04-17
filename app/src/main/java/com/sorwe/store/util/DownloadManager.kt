package com.sorwe.store.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Holds the download info for a single app.
 */
data class DownloadInfo(
    val appName: String,
    val status: DownloadStatus = DownloadStatus.Idle,
    val file: File? = null
)

private data class DownloadJob(
    val appName: String,
    val url: String
)

/**
 * Application-scoped download manager.
 *
 * Downloads run in their own [CoroutineScope] with [SupervisorJob],
 * so navigating away from a screen will NOT cancel in-progress downloads.
 * The cancel button properly cancels the OkHttp call AND the coroutine.
 * Downloads are processed sequentially via a queue.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DownloadManager"
    }

    // Scope that lives as long as the application
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Per-app download states — keyed by appName
    private val _downloads = ConcurrentHashMap<String, MutableStateFlow<DownloadInfo>>()

    // Per-app coroutine jobs for cancellation
    private val jobs = ConcurrentHashMap<String, Job>()

    // Queue for sequential downloads
    private val downloadQueue = Channel<DownloadJob>(Channel.UNLIMITED)

    // Unified global state for UI
    private val _allDownloadsFlow = MutableStateFlow<List<DownloadInfo>>(emptyList())
    val allDownloadsFlow: StateFlow<List<DownloadInfo>> = _allDownloadsFlow

    private fun publishAllDownloads() {
        _allDownloadsFlow.value = _downloads.values.map { it.value }
    }

    init {
        // Start the single worker to process downloads sequentially
        scope.launch {
            for (job in downloadQueue) {
                processDownload(job.appName, job.url)
            }
        }
        
        // Pre-scan directory to populate existing downloaded apks
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        dir?.listFiles()?.forEach { file ->
            if (file.extension == "apk" && isValidApk(file)) {
                // Best effort unsanitize: replace underscores with spaces
                val fallbackAppName = file.nameWithoutExtension.replace("_", " ")
                val info = DownloadInfo(fallbackAppName, DownloadStatus.Completed, file)
                _downloads[fallbackAppName] = MutableStateFlow(info)
            }
        }
        publishAllDownloads()
    }

    /**
     * Returns a [StateFlow] for the given app's download state.
     * UI screens observe this to show progress / completed / idle state.
     */
    fun getDownloadState(appName: String): StateFlow<DownloadInfo> {
        return _downloads.getOrPut(appName) {
            // Check if a previously downloaded file already exists
            val existingFile = DownloadUtil.findDownloadedApk(context, appName)
            val initialStatus = if (existingFile != null && isValidApk(existingFile)) {
                DownloadInfo(appName, DownloadStatus.Completed, existingFile)
            } else {
                DownloadInfo(appName)
            }
            MutableStateFlow(initialStatus)
        }
    }

    /**
     * Enqueues a download for the given app. If a download is already active/pending, this is a no-op.
     */
    fun startDownload(appName: String, url: String) {
        val flow = _downloads.getOrPut(appName) { MutableStateFlow(DownloadInfo(appName)) }

        // Don't queue if already downloading or pending
        val current = flow.value
        if (current.status is DownloadStatus.Downloading || current.status is DownloadStatus.Pending) {
            Log.d(TAG, "Download already in progress/queue for: $appName")
            return
        }

        // Set state to Pending
        flow.value = DownloadInfo(appName, DownloadStatus.Pending)
        publishAllDownloads()
        
        // Push to queue
        downloadQueue.trySend(DownloadJob(appName, url))
    }

    /**
     * Actually processes the download synchronously.
     * Called one-by-one by the worker coroutine.
     */
    private suspend fun processDownload(appName: String, url: String) {
        val flow = _downloads[appName] ?: return
        
        // Update to downloading state
        flow.value = DownloadInfo(appName, DownloadStatus.Downloading(-1f, 0L, -1L))

        // Show "Download started" notification and start the foreground service
        DownloadMonitorService.showStarted(context, appName)

        val job = scope.launch {
            val file = DownloadUtil.downloadApk(
                context = context,
                url = url,
                appName = appName
            ) { status ->
                flow.value = DownloadInfo(appName, status, flow.value.file)

                // Update notification with progress
                when (status) {
                    is DownloadStatus.Downloading -> {
                        DownloadMonitorService.updateProgress(context, appName, status)
                    }
                    is DownloadStatus.Completed -> {
                        DownloadMonitorService.showCompleted(context, appName)
                    }
                    is DownloadStatus.Failed -> {
                        DownloadMonitorService.showFailed(context, appName)
                    }
                    else -> {}
                }
                publishAllDownloads()
            }

            // Update final state
            if (file != null) {
                flow.value = DownloadInfo(appName, DownloadStatus.Completed, file)
                // Note: showCompleted is already called inside the callback above
            }

            // Clean up job reference
            jobs.remove(appName)
            
            // Auto-install if Shizuku "Direct Install" is enabled
            if (file != null) {
                attemptAutoInstall(file)
            }
        }

        jobs[appName] = job

        // If the coroutine is cancelled (e.g. via cancelDownload), reset state
        job.invokeOnCompletion { throwable ->
            if (throwable is kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "Download cancelled for: $appName")
                flow.value = DownloadInfo(appName, DownloadStatus.Idle)
                publishAllDownloads()
                DownloadMonitorService.dismiss(context, appName)
            }
        }
    }

    /**
     * Cancels an active download. This cancels both the OkHttp call and the coroutine.
     */
    fun cancelDownload(appName: String) {
        Log.d(TAG, "Cancelling download for: $appName")

        // Cancel the OkHttp call first (this makes the blocking execute() throw)
        DownloadUtil.cancelActiveCall(appName)

        // Then cancel the coroutine
        jobs[appName]?.cancel()
        jobs.remove(appName)

        // Reset state
        _downloads[appName]?.let { flow ->
            flow.value = DownloadInfo(appName, DownloadStatus.Idle)
        }
        publishAllDownloads()

        // Dismiss notification
        DownloadMonitorService.dismiss(context, appName)

        // Delete partially downloaded file
        val partialFile = DownloadUtil.findDownloadedApk(context, appName)
        if (partialFile != null && partialFile.exists()) {
            partialFile.delete()
            Log.d(TAG, "Deleted partial download: ${partialFile.name}")
        }
    }

    /**
     * Resets the download state to Idle (e.g. after install or if file was deleted).
     */
    fun resetDownload(appName: String) {
        _downloads[appName]?.let { flow ->
            flow.value = DownloadInfo(appName, DownloadStatus.Idle)
        }
        publishAllDownloads()
    }

    /**
     * Manually sets the download state to Failed for an app.
     * Used when metadata resolution or other pre-download steps fail.
     */
    fun setDownloadFailed(appName: String) {
        _downloads.getOrPut(appName) { MutableStateFlow(DownloadInfo(appName)) }.let { flow ->
            flow.value = DownloadInfo(appName, DownloadStatus.Failed())
        }
        publishAllDownloads()
    }

    /**
     * Checks whether a download is currently active for the given app.
     */
    fun isDownloading(appName: String): Boolean {
        return _downloads[appName]?.value?.status is DownloadStatus.Downloading
    }

    private suspend fun attemptAutoInstall(file: File) {
        try {
            val userPreferences = dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                com.sorwe.store.ui.screens.settings.SettingsEntryPoint::class.java
            ).userPreferences()
            
            val isShizukuEnabled = userPreferences.isShizukuInstallerEnabled.first()
            
            if (isShizukuEnabled && ShizukuInstaller.hasShizukuPermission()) {
                Log.d(TAG, "Auto-installing ${file.name} via Shizuku")
                DownloadUtil.installApk(context, file)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Auto-install failed", e)
        }
    }

    private fun isValidApk(file: File): Boolean {
        return try {
            file.inputStream().use { stream ->
                val header = ByteArray(4)
                stream.read(header) == 4 &&
                    header[0] == 0x50.toByte() &&
                    header[1] == 0x4B.toByte() &&
                    header[2] == 0x03.toByte() &&
                    header[3] == 0x04.toByte()
            }
        } catch (e: Exception) { false }
    }
}
