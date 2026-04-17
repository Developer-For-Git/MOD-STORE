package com.sorwe.store.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object PackageUtil {
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    fun getPackageVersionName(context: Context, packageName: String): String? {
        return try {
            val pInfo = context.packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE)
            intent.data = Uri.parse("package:$packageName")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PackageUtil", "Failed to launch uninstall intent", e)
            android.widget.Toast.makeText(context, "Could not launch uninstaller", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun findPackageNameByLabel(context: Context, label: String): String? {
        val pm = context.packageManager
        // Requires QUERY_ALL_PACKAGES permission
        val packages = pm.getInstalledPackages(0)
        
        // 1. Try exact match first
        val exactMatch = packages.find {
            val appLabel = it.applicationInfo.loadLabel(pm).toString().trim()
            appLabel.equals(label.trim(), ignoreCase = true)
        }
        if (exactMatch != null) return exactMatch.packageName
        
        // 2. Try fuzzy match (if one label completely starts with the other)
        val fuzzyMatch = packages.find {
            val appLabel = it.applicationInfo.loadLabel(pm).toString().trim()
            if (appLabel.length > 3 && label.length > 3) {
                appLabel.startsWith(label, ignoreCase = true) || label.startsWith(appLabel, ignoreCase = true)
            } else {
                false
            }
        }
        
        return fuzzyMatch?.packageName
    }

    /**
     * Compares installed and remote version strings robustly.
     * Returns true ONLY if remote version is strictly newer/different enough to justify an update.
     */
    fun isVersionNewer(installed: String?, remote: String?): Boolean {
        if (installed.isNullOrBlank() || remote.isNullOrBlank()) return false
        if (remote == "Unknown") return false
        
        val s1 = sanitizeVersion(installed)
        val s2 = sanitizeVersion(remote)
        
        // Literal match after sanitization covers 99% of cases
        if (s1 == s2) return false
        
        // If one contains the other (e.g. "1.1.0" vs "1.1"), they might be the same.
        // We only want to update if they are significantly different.
        // Let's split by dots and compare numeric parts.
        val parts1 = s1.split(".")
        val parts2 = s2.split(".")
        
        val length = maxOf(parts1.size, parts2.size)
        for (i in 0 until length) {
            val v1 = parts1.getOrNull(i)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            val v2 = parts2.getOrNull(i)?.filter { it.isDigit() }?.toIntOrNull() ?: 0
            
            if (v2 > v1) return true
            if (v1 > v2) return false
        }
        
        // If numeric parts are equal, check if remote has extra info (like build number)
        // This is conservative: if remote is longer but identical in numbers, we don't necessarily update.
        return false
    }

    private fun sanitizeVersion(version: String): String {
        return version.lowercase()
            .removePrefix("v")
            .replace(Regex("[^0-9.]"), "") // Keep only digits and dots for numeric comparison
            .trim()
    }
}
