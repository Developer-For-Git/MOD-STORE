package com.sorwe.store

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SorweApp : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        try {
            rikka.shizuku.Shizuku.addBinderReceivedListener {
                android.util.Log.d("SorweApp", "Shizuku binder received asynchronously")
            }
            if (rikka.shizuku.Shizuku.pingBinder()) {
                android.util.Log.d("SorweApp", "Shizuku binder already available on start")
            }
        } catch (e: Exception) {
            android.util.Log.e("SorweApp", "Failed to initialize Shizuku binder listener", e)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
