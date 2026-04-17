package com.sorwe.store.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sorwe.store.MainActivity
import com.sorwe.store.R
import com.sorwe.store.data.repository.AppRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodically checks for new versions of downloaded/installed apps via GitHub API.
 */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val appRepository: AppRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("UpdateCheckWorker", "Starting background update check...")
        return try {
            // Get all apps with forced refresh to ensure latest metadata
            val resource = appRepository.getApps(forceRefresh = true)
                .first { it is com.sorwe.store.data.model.Resource.Success }
            val catalogApps = (resource as? com.sorwe.store.data.model.Resource.Success)?.data ?: emptyList()

            // Filter out apps that are not installed on the device
            val localApps = catalogApps.filter { app ->
                com.sorwe.store.util.PackageUtil.isPackageInstalled(context, app.id)
            }

            if (localApps.isEmpty()) {
                Log.d("UpdateCheckWorker", "No apps to check for updates.")
                return Result.success()
            }

            var updatesFound = 0
            for (localApp in localApps) {
                // Here localApp IS the catalogApp (from the remote fetch), 
                // but we need to compare it with the locally installed app version.
                // PackageUtil.getPackageVersionName returns the version string of the installed app.
                val installedVersion = com.sorwe.store.util.PackageUtil.getPackageVersionName(context, localApp.id)
                
                if (com.sorwe.store.util.PackageUtil.isVersionNewer(installedVersion, localApp.version)) {
                    Log.d("UpdateCheckWorker", "Update available for ${localApp.name}: $installedVersion -> ${localApp.version}")
                    updatesFound++
                }
            }

            if (updatesFound > 0) {
                showUpdateNotification(updatesFound)
            }

            Log.d("UpdateCheckWorker", "Update check complete. Found $updatesFound apps with updates.")
            Result.success()
        } catch (e: Exception) {
            Log.e("UpdateCheckWorker", "Failed to check for updates", e)
            Result.retry()
        }
    }

    private fun showUpdateNotification(updatesCount: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "app_updates_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for available app updates"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = if (updatesCount == 1) "1 app has an update available." else "$updatesCount apps have updates available."

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentTitle("App Updates Available")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(1001, builder.build())
        } catch (e: SecurityException) {
            Log.e("UpdateCheckWorker", "Missing notification permission", e)
        }
    }
}
