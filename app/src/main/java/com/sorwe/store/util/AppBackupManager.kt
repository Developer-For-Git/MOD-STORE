package com.sorwe.store.util

import android.content.Context
import android.net.Uri
import com.sorwe.store.data.model.AppItem
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

@Serializable
data class AppBackupItem(
    val id: String,
    val name: String,
    val packageName: String,
    val isApk: Boolean = false,
    val apkUrl: String? = null,
    val repoUrl: String? = null
)

@Serializable
data class AppBackupList(
    val version: Int = 1,
    val apps: List<AppBackupItem>
)

class AppBackupManager(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    /**
     * Exports a list of installed/downloaded apps to a JSON format.
     */
    fun exportApps(apps: List<AppItem>, outputStream: OutputStream) {
        val backupItems = apps.map { app ->
            AppBackupItem(
                id = app.id,
                name = app.name,
                packageName = app.id,
                isApk = app.downloadUrl.endsWith(".apk", ignoreCase = true),
                apkUrl = app.downloadUrl,
                repoUrl = app.repoUrl
            )
        }
        val backupList = AppBackupList(apps = backupItems)
        val jsonString = json.encodeToString(backupList)
        
        outputStream.use { stream ->
            stream.write(jsonString.toByteArray())
        }
    }

    /**
     * Imports a list of apps from a JSON document.
     * Returns a list of application IDs and URLs to download.
     */
    fun importApps(inputStream: InputStream): List<AppBackupItem> {
        return inputStream.use { stream ->
            val jsonString = stream.bufferedReader().use { it.readText() }
            val backupList = json.decodeFromString<AppBackupList>(jsonString)
            backupList.apps
        }
    }
}
