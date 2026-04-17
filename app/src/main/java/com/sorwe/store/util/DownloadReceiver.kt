package com.sorwe.store.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Legacy BroadcastReceiver for DownloadManager completions.
 * Downloads now use OkHttp directly, so this receiver only handles
 * edge cases where DownloadManager might still be used by the system.
 */
class DownloadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return

        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return

        Log.d("DownloadReceiver", "Download complete broadcast for ID: $downloadId")
        // Downloads are now handled via OkHttp coroutines in DetailScreen,
        // so this receiver is kept as a no-op for any residual system broadcasts.
    }
}
