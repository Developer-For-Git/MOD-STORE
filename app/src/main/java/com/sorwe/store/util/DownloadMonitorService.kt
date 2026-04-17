package com.sorwe.store.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.sorwe.store.MainActivity
import com.sorwe.store.R
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Foreground service that shows real-time download progress notifications.
 *
 * Notifications:
 *  1. "Download started" — immediately when download begins
 *  2. Real-time progress — updates with percentage while downloading
 *  3. "Download complete" — separate dismissible notification on finish
 *  4. "Download failed"  — separate dismissible notification on error
 */
class DownloadMonitorService : Service() {

    companion object {
        const val APP_NAME = "app_name"
        const val CHANNEL_PROGRESS_ID = "downloads_progress_channel"
        const val CHANNEL_RESULT_ID   = "downloads_result_channel"

        // Use a unique ID per app so multiple concurrent downloads each get their own notification
        private val notifIdCounter = AtomicInteger(2000)
        private val appNotifIds    = ConcurrentHashMap<String, Int>()  // appName -> notifId
        private val activeCount    = AtomicInteger(0)

        private const val TAG = "DownloadMonitor"

        // ── Public API called from DownloadManager ──────────────────────────

        /**
         * Show the "Download started" notification and begin the foreground service.
         * Called right when download kicks off.
         */
        fun showStarted(context: Context, appName: String) {
            try {
                ensureChannels(context)
                activeCount.incrementAndGet()

                val notifId = appNotifIds.getOrPut(appName) {
                    notifIdCounter.incrementAndGet()
                }

                val nm = context.getSystemService(NotificationManager::class.java) ?: return

                // "Started" notification — indeterminate progress bar
                val notification = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
                    .setContentTitle("Downloading $appName")
                    .setContentText("Starting download…")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setProgress(0, 0, true)   // indeterminate until first progress update
                    .setContentIntent(openAppIntent(context))
                    .build()

                nm.notify(notifId, notification)

                // Start foreground service
                val intent = Intent(context, DownloadMonitorService::class.java).apply {
                    putExtra(APP_NAME, appName)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "showStarted error: ${e.message}", e)
            }
        }

        private val lastReportedPercent = ConcurrentHashMap<String, Int>()

        /**
         * Update the download progress notification.
         * Called repeatedly during the download with current percentage.
         */
        fun updateProgress(context: Context, appName: String, status: DownloadStatus.Downloading) {
            try {
                val nm = context.getSystemService(NotificationManager::class.java) ?: return
                val notifId = appNotifIds[appName] ?: return

                val percent = if (status.progress in 0f..1f) {
                    (status.progress * 100).toInt()
                } else -1

                // Throttle updates: only notify if percentage changed
                val lastPercent = lastReportedPercent[appName]
                if (lastPercent != null && lastPercent == percent && percent < 100) {
                    return // skip updating UI if it's identical
                }
                lastReportedPercent[appName] = percent

                val contentText = if (percent >= 0) "$percent% complete" else "Downloading…"

                val notification = NotificationCompat.Builder(context, CHANNEL_PROGRESS_ID)
                    .setContentTitle("Downloading $appName")
                    .setContentText(contentText)
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(openAppIntent(context))
                    .apply {
                        if (percent >= 0) {
                            setProgress(100, percent, false)
                        } else {
                            setProgress(0, 0, true)
                        }
                    }
                    .build()

                nm.notify(notifId, notification)
            } catch (e: Exception) {
                Log.e(TAG, "updateProgress error: ${e.message}", e)
            }
        }

        /**
         * Dismiss progress notification and show "Download Complete".
         */
        fun showCompleted(context: Context, appName: String) {
            try {
                ensureChannels(context)
                val nm = context.getSystemService(NotificationManager::class.java) ?: return

                // Dismiss the progress notification for this app
                val progressId = appNotifIds.remove(appName)
                if (progressId != null) nm.cancel(progressId)

                // Post a separate "complete" notification
                val completeId = (appName.hashCode() and 0x7FFFFFFF) + 1
                val notification = NotificationCompat.Builder(context, CHANNEL_RESULT_ID)
                    .setContentTitle("✅ Download Complete")
                    .setContentText("$appName is ready to install. Tap to open.")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(openAppIntent(context))
                    .build()

                nm.notify(completeId, notification)

                stopWhenIdle(context)
            } catch (e: Exception) {
                Log.e(TAG, "showCompleted error: ${e.message}", e)
            }
        }

        /**
         * Dismiss progress notification and show "Download Failed".
         */
        fun showFailed(context: Context, appName: String) {
            try {
                ensureChannels(context)
                val nm = context.getSystemService(NotificationManager::class.java) ?: return

                val progressId = appNotifIds.remove(appName)
                if (progressId != null) nm.cancel(progressId)

                val failedId = (appName.hashCode() and 0x7FFFFFFF) + 2
                val notification = NotificationCompat.Builder(context, CHANNEL_RESULT_ID)
                    .setContentTitle("❌ Download Failed")
                    .setContentText("$appName download failed. Tap to retry.")
                    .setSmallIcon(android.R.drawable.stat_notify_error)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(openAppIntent(context))
                    .build()

                nm.notify(failedId, notification)

                stopWhenIdle(context)
            } catch (e: Exception) {
                Log.e(TAG, "showFailed error: ${e.message}", e)
            }
        }

        /**
         * Dismiss all notifications for [appName] (e.g. user cancelled).
         */
        fun dismiss(context: Context, appName: String) {
            try {
                val nm = context.getSystemService(NotificationManager::class.java) ?: return
                val progressId = appNotifIds.remove(appName)
                if (progressId != null) nm.cancel(progressId)

                stopWhenIdle(context)
            } catch (e: Exception) {
                Log.e(TAG, "dismiss error: ${e.message}", e)
            }
        }

        private fun stopWhenIdle(context: Context) {
            if (activeCount.decrementAndGet() <= 0) {
                activeCount.set(0)
                try {
                    context.stopService(Intent(context, DownloadMonitorService::class.java))
                } catch (e: Exception) {
                    Log.e(TAG, "stopService error: ${e.message}", e)
                }
            }
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(NotificationManager::class.java) ?: return

                if (nm.getNotificationChannel(CHANNEL_PROGRESS_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_PROGRESS_ID,
                            "Download Progress",
                            NotificationManager.IMPORTANCE_LOW
                        ).apply { description = "Shows real-time download progress" }
                    )
                }

                if (nm.getNotificationChannel(CHANNEL_RESULT_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_RESULT_ID,
                            "Download Results",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply { description = "Notifies when downloads complete or fail" }
                    )
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appName = intent?.getStringExtra(APP_NAME) ?: "App"
        Log.d(TAG, "Service started for: $appName")

        ensureChannels(this)

        // Must call startForeground immediately with an existing notification
        val notifId = appNotifIds.getOrPut(appName) { notifIdCounter.incrementAndGet() }

        val notification = NotificationCompat.Builder(this, CHANNEL_PROGRESS_ID)
            .setContentTitle("Downloading $appName")
            .setContentText("Starting download…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(0, 0, true)
            .setContentIntent(openAppIntent(this))
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(notifId, notification)
        }

        return START_NOT_STICKY
    }
}
