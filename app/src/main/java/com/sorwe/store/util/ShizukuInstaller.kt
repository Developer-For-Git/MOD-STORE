package com.sorwe.store.util

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import com.sorwe.store.BuildConfig
import com.sorwe.store.IShellService
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ShizukuInstaller {
    const val REQUEST_CODE = 1001
    private const val TAG = "ShizukuInstaller"

    private var shellService: IShellService? = null
    
    private val userServiceArgs by lazy {
        rikka.shizuku.Shizuku.UserServiceArgs(ComponentName("com.sorwe.store", ShellService::class.java.name))
            .daemon(false)
            .processNameSuffix("shizuku_service")
            .debuggable(BuildConfig.DEBUG)
    }

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return if (isShizukuAvailable()) {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
    }

    fun requestShizukuPermission() {
        if (isShizukuAvailable() && !hasShizukuPermission()) {
            Shizuku.requestPermission(REQUEST_CODE)
        }
    }

    private suspend fun getService(): IShellService? {
        if (shellService != null && (shellService as IShellService).asBinder().isBinderAlive) return shellService
        
        return suspendCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    val instance = IShellService.Stub.asInterface(service)
                    shellService = instance
                    continuation.resume(instance)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    shellService = null
                    continuation.resume(null)
                }
            }
            try {
                Shizuku.bindUserService(userServiceArgs, connection)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to bind user service", e)
                continuation.resume(null)
            }
        }
    }

    suspend fun installApk(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) {
            Log.e(TAG, "Shizuku permission not granted")
            return@withContext false
        }

        var pfd: android.os.ParcelFileDescriptor? = null
        try {
            val service = getService() ?: run {
                Log.e(TAG, "Failed to get ShellService")
                return@withContext false
            }
            
            Log.d(TAG, "Opening PFD for: ${apkFile.absolutePath}")
            pfd = android.os.ParcelFileDescriptor.open(apkFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            
            val exitCode = service.installApk(pfd)
            Log.d(TAG, "Shizuku install finished with code: $exitCode")
            
            return@withContext exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install via Shizuku PFD", e)
            return@withContext false
        } finally {
            try { pfd?.close() } catch (e: Exception) {}
        }
    }

    suspend fun uninstallApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) return@withContext false

        try {
            val service = getService() ?: return@withContext false
            val command = "pm uninstall $packageName"
            val exitCode = service.runCommand(command)
            return@withContext exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed to uninstall via Shizuku", e)
            return@withContext false
        }
    }
}
