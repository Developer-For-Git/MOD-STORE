package com.sorwe.store.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.sorwe.store.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Sealed interface representing the current state of a download.
 */
sealed interface DownloadStatus {
    /** No download has been started. */
    data object Idle : DownloadStatus

    /** Download is in progress with a value from 0f to 1f. */
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : DownloadStatus

    /** Download is in queue waiting to start. */
    data object Pending : DownloadStatus

    /** Download finished successfully. */
    data object Completed : DownloadStatus

    /** Download failed. */
    data class Failed(val reason: Int = 0) : DownloadStatus
}

object DownloadUtil {

    // OkHttp client dedicated to downloads — longer timeouts, follows redirects
    private val downloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    // Track current download state via Flow (observable by UI)
    private val _downloadState = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadState: StateFlow<DownloadStatus> = _downloadState

    // Track: last downloaded file path (for install)
    private var lastDownloadedFile: File? = null

    // ─── Active OkHttp calls — keyed by sanitised app name ───────────
    private val activeCalls = ConcurrentHashMap<String, Call>()

    /**
     * Cancels the active OkHttp call for [appName], if any.
     * This causes the blocking `execute()` or streaming read to throw an IOException.
     */
    fun cancelActiveCall(appName: String) {
        val key = sanitizeFileName(appName)
        activeCalls.remove(key)?.let { call ->
            if (!call.isCanceled()) {
                call.cancel()
                Log.d("Download", "OkHttp call cancelled for: $appName")
            }
        }
    }

    /**
     * Downloads an APK using OkHttp directly — starts IMMEDIATELY with no system queue delay.
     * Must be called from a coroutine (suspend function).
     *
     * @return The File where the APK was saved, or null on failure.
     */
    suspend fun downloadApk(
        context: Context,
        url: String,
        appName: String,
        onProgress: (DownloadStatus) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val callKey = sanitizeFileName(appName)
        try {
            Log.d("Download", "Starting OkHttp download: $url")
            onProgress(DownloadStatus.Downloading(-1f, 0L, -1L))

            // Sanitize filename
            val fileName = "${sanitizeFileName(appName)}.apk"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir?.mkdirs()
            val outputFile = File(downloadsDir, fileName)

            // Delete existing file to avoid conflicts
            if (outputFile.exists()) {
                outputFile.delete()
                Log.d("Download", "Deleted old file: ${outputFile.absolutePath}")
            }

            // Build request — keep it simple, just add auth for GitHub
            val requestBuilder = Request.Builder()
                .url(url)
                .header("User-Agent", "SorweStore/1.0")

            // Removed GitHub token here because it breaks the AWS S3 redirect
            // S3 will reject the request if it contains an unexpected Authorization header

            val request = requestBuilder.build()
            val call = downloadClient.newCall(request)
            activeCalls[callKey] = call   // track so we can cancel it

            // Execute — this starts the connection immediately
            val response = call.execute()

            // Log response details for debugging
            val finalUrl = response.request.url.toString()
            val contentType = response.header("Content-Type") ?: "unknown"
            Log.d("Download", "Response: HTTP ${response.code} | Content-Type: $contentType | Final URL: $finalUrl")

            if (!response.isSuccessful) {
                val errorMsg = "Download failed: HTTP ${response.code}"
                Log.e("Download", errorMsg)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
                onProgress(DownloadStatus.Failed(response.code))
                activeCalls.remove(callKey)
                return@withContext null
            }

            // Check if GitHub returned HTML instead of binary
            if (contentType.contains("text/html", ignoreCase = true)) {
                val errorMsg = "Server returned HTML page instead of file (Content-Type: $contentType)"
                Log.e("Download", errorMsg)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download error: received webpage instead of file", Toast.LENGTH_LONG).show()
                }
                response.close()
                onProgress(DownloadStatus.Failed(0))
                activeCalls.remove(callKey)
                return@withContext null
            }

            val body = response.body ?: run {
                Log.e("Download", "Empty response body")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Download error: empty response", Toast.LENGTH_LONG).show()
                }
                onProgress(DownloadStatus.Failed(0))
                activeCalls.remove(callKey)
                return@withContext null
            }

            val contentLength = body.contentLength()
            Log.d("Download", "Content-Length: $contentLength bytes")

            // Stream to file with progress reporting
            var bytesDownloaded = 0L
            val buffer = ByteArray(65536) // Use 64KB buffer for faster I/O
            var lastProgressReportTime = 0L

            FileOutputStream(outputFile).use { fos ->
                body.byteStream().use { inputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        // Check for coroutine cancellation (cooperative cancellation)
                        coroutineContext.ensureActive()

                        fos.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        // Report progress throttled to avoid UI/StateFlow overhead
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastProgressReportTime > 100) {
                            lastProgressReportTime = currentTime
                            val progress = if (contentLength > 0) {
                                bytesDownloaded.toFloat() / contentLength.toFloat()
                            } else {
                                -1f // indeterminate
                            }
                            onProgress(
                                DownloadStatus.Downloading(
                                    progress = progress,
                                    downloadedBytes = bytesDownloaded,
                                    totalBytes = contentLength
                                )
                            )
                        }
                    }
                }
            }

            Log.d("Download", "Download complete: ${outputFile.absolutePath} ($bytesDownloaded bytes)")

            // Validate downloaded file is a real APK (ZIP magic bytes: PK\x03\x04)
            val isValidApk = try {
                outputFile.inputStream().use { stream ->
                    val header = ByteArray(4)
                    stream.read(header) == 4 &&
                        header[0] == 0x50.toByte() &&
                        header[1] == 0x4B.toByte() &&
                        header[2] == 0x03.toByte() &&
                        header[3] == 0x04.toByte()
                }
            } catch (e: Exception) { false }

            if (!isValidApk) {
                // Read first 200 chars to see what was actually downloaded
                val preview = try {
                    outputFile.readText().take(200)
                } catch (e: Exception) { "unreadable" }
                Log.e("Download", "NOT a valid APK! First 200 chars: $preview")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloaded file is not a valid APK", Toast.LENGTH_LONG).show()
                }
                outputFile.delete()
                onProgress(DownloadStatus.Failed(0))
                activeCalls.remove(callKey)
                return@withContext null
            }

            lastDownloadedFile = outputFile
            activeCalls.remove(callKey)
            onProgress(DownloadStatus.Completed)
            return@withContext outputFile

        } catch (e: kotlinx.coroutines.CancellationException) {
            // Coroutine was cancelled (user pressed cancel) — clean up partial file
            Log.d("Download", "Download coroutine cancelled for: $appName")
            activeCalls.remove(callKey)
            val partialFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "${sanitizeFileName(appName)}.apk"
            )
            if (partialFile.exists()) {
                partialFile.delete()
                Log.d("Download", "Deleted partial file: ${partialFile.name}")
            }
            throw e  // Re-throw so the coroutine machinery handles it properly
        } catch (e: java.io.IOException) {
            // OkHttp call.cancel() causes an IOException — check if it was intentional
            if (activeCalls[callKey]?.isCanceled() == true) {
                Log.d("Download", "Download cancelled (IOException) for: $appName")
                activeCalls.remove(callKey)
                return@withContext null
            }
            Log.e("Download", "Download failed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            onProgress(DownloadStatus.Failed(0))
            activeCalls.remove(callKey)
            return@withContext null
        } catch (e: Exception) {
            Log.e("Download", "Download failed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            onProgress(DownloadStatus.Failed(0))
            activeCalls.remove(callKey)
            return@withContext null
        }
    }

    /**
     * Launches the system package installer or uses Shizuku for silent install.
     */
    suspend fun installApk(context: Context, file: File, onComplete: ((Boolean) -> Unit)? = null) {
        if (!file.exists()) {
            Log.e("Install", "File not found: ${file.absolutePath}")
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Downloaded file not found", Toast.LENGTH_SHORT).show()
            }
            onComplete?.invoke(false)
            return
        }

        val userPreferences = dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.sorwe.store.ui.screens.settings.SettingsEntryPoint::class.java
        ).userPreferences()
        
        val isShizukuEnabled = userPreferences.isShizukuInstallerEnabled.first()

        if (isShizukuEnabled && ShizukuInstaller.hasShizukuPermission()) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Directing installation via Shizuku...", Toast.LENGTH_SHORT).show()
            }
            val success = ShizukuInstaller.installApk(file)
            withContext(Dispatchers.Main) {
                if (success) {
                    Toast.makeText(context, "Installation complete!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Shizuku install failed, using system installer", Toast.LENGTH_SHORT).show()
                    installApkViaSystem(context, file)
                }
            }
            if (success) {
                // Wait briefly for system to register package, then refresh global cache
                delay(500)
                val myAppsCache = dagger.hilt.android.EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    com.sorwe.store.ui.screens.myapps.MyAppsCacheEntryPoint::class.java
                ).myAppsCache()
                myAppsCache.refresh()
            }
            onComplete?.invoke(success)
        } else {
            // Fallback to system install if Shizuku not enabled, not authorized, or not available
            installApkViaSystem(context, file)
            onComplete?.invoke(true)
        }
    }

    private fun installApkViaSystem(context: Context, file: File) {
        try {
            if (!file.exists()) {
                Log.e("Install", "File not found: ${file.absolutePath}")
                Toast.makeText(context, "Downloaded file not found", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("Install", "Installing: ${file.absolutePath} (${file.length()} bytes)")

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e("Install", "Install failed: ${e.message}", e)
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Returns the last downloaded file (for install after download completes).
     */
    fun getLastDownloadedFile(): File? = lastDownloadedFile

    /**
     * Finds an existing downloaded APK file by app name.
     */
    fun findDownloadedApk(context: Context, appName: String): File? {
        val fileName = "${sanitizeFileName(appName)}.apk"
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Checks whether this app is allowed to install unknown packages (API 26+).
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Opens the system settings screen for enabling "Install from unknown sources" (API 26+).
     */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    /**
     * Sanitize app name into a safe filename.
     */
    private fun sanitizeFileName(appName: String): String {
        return appName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }
}
