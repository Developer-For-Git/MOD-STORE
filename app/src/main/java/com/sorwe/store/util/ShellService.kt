package com.sorwe.store.util

import com.sorwe.store.IShellService
import kotlin.system.exitProcess
import android.util.Log

class ShellService : IShellService.Stub() {
    
    override fun runCommand(cmd: String): Int {
        return try {
            Log.d("ShellService", "Executing command: $cmd")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val exitCode = process.waitFor()
            Log.d("ShellService", "Command exited with code: $exitCode")
            exitCode
        } catch (e: Exception) {
            Log.e("ShellService", "Error executing command", e)
            -1
        }
    }

    override fun installApk(pfd: android.os.ParcelFileDescriptor?): Int {
        if (pfd == null) {
            Log.e("ShellService", "installApk: pfd is null")
            return -1
        }
        val tempFile = java.io.File("/data/local/tmp/shizuku_install.apk")
        return try {
            Log.d("ShellService", "Reading APK from file descriptor...")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                java.io.FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Run pm install using the temp file shell can see
            val cmd = "pm install -r -d -g \"${tempFile.absolutePath}\""
            Log.d("ShellService", "Executing silent install: $cmd")
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val exitCode = process.waitFor()
            
            // Log results
            Log.d("ShellService", "Silent install finished with code: $exitCode")
            
            exitCode
        } catch (e: Exception) {
            Log.e("ShellService", "Silent install failed", e)
            -1
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
                Log.d("ShellService", "Temp file cleaned up")
            }
        }
    }

    override fun onTransact(code: Int, data: android.os.Parcel, reply: android.os.Parcel?, flags: Int): Boolean {
        if (code == 16777215) {
            destroy()
            return true
        }
        return super.onTransact(code, data, reply, flags)
    }

    override fun destroy() {
        Log.d("ShellService", "Destroying service")
        exitProcess(0)
    }
}
