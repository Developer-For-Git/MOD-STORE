package com.sorwe.store

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sorwe.store.ui.navigation.SorweNavGraph
import com.sorwe.store.ui.screens.myapps.MyAppsCache
import com.sorwe.store.ui.screens.onboarding.PermissionOnboardingScreen
import com.sorwe.store.ui.theme.SorweStoreTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    @Inject lateinit var myAppsCache: MyAppsCache
    @Inject lateinit var userPreferences: com.sorwe.store.data.preferences.UserPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _: Boolean -> }

    private val intentState = androidx.compose.runtime.mutableStateOf<android.content.Intent?>(null)

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentState.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Unlock High Refresh Rate (120Hz)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            
            display?.supportedModes?.maxByOrNull { it.refreshRate }?.let { mode ->
                val lp = window.attributes
                lp.preferredDisplayModeId = mode.modeId
                window.attributes = lp
            }
        }

        enableEdgeToEdge()

        // Schedule Daily Background Update Check
        lifecycleScope.launch {
            userPreferences.isAutoUpdateEnabled.collect { enabled ->
                if (enabled) {
                    val updateWorkRequest = PeriodicWorkRequestBuilder<com.sorwe.store.worker.UpdateCheckWorker>(24, TimeUnit.HOURS)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build()
                        )
                        .build()
                        
                    WorkManager.getInstance(this@MainActivity).enqueueUniquePeriodicWork(
                        "AutoUpdateCheck",
                        ExistingPeriodicWorkPolicy.KEEP,
                        updateWorkRequest
                    )
                } else {
                    WorkManager.getInstance(this@MainActivity).cancelUniqueWork("AutoUpdateCheck")
                }
            }
        }

        // Pre-load My Apps data in background so it's instant
        myAppsCache.ensureLoaded()

        // Biometric Lock State
        val isAuthenticated = androidx.compose.runtime.mutableStateOf(false)
        val biometricEnabled = androidx.compose.runtime.mutableStateOf(false)
        val preferencesLoaded = androidx.compose.runtime.mutableStateOf(false)

        // Onboarding State
        val onboardingCompleted = androidx.compose.runtime.mutableStateOf(false)

        lifecycleScope.launch {
            userPreferences.isOnboardingCompleted.collect { completed ->
                onboardingCompleted.value = completed
            }
        }

        lifecycleScope.launch {
            userPreferences.isBiometricLockEnabled.collect { enabled ->
                biometricEnabled.value = enabled
                if (!enabled) isAuthenticated.value = true
                preferencesLoaded.value = true
            }
        }

        // Shizuku permission result listener
        try {
            rikka.shizuku.Shizuku.addRequestPermissionResultListener { _, grantResult ->
                if (grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    android.util.Log.d("MainActivity", "Shizuku permission granted")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to add Shizuku permission listener", e)
        }

        setContent {
            SorweStoreTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val loaded by preferencesLoaded
                    val authenticated by isAuthenticated
                    val isLocked by biometricEnabled

                    if (!loaded) {
                        // Keep a neutral dark background while loading preferences to avoid flash
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            // Optional: Add a subtle loading indicator or logo if loading takes > 100ms
                        }
                    } else if (isLocked && !authenticated) {
                        BiometricLockScreen(
                            onAuthenticated = { isAuthenticated.value = true }
                        )
                    } else if (!onboardingCompleted.value) {
                        PermissionOnboardingScreen(
                            onComplete = {
                                lifecycleScope.launch {
                                    userPreferences.setOnboardingCompleted(true)
                                }
                            }
                        )
                    } else {
                        val currentIntent by intentState
                        SorweNavGraph(intent = currentIntent)
                    }
                }
            }
        }
    }
}

@Composable
fun BiometricLockScreen(onAuthenticated: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current as androidx.fragment.app.FragmentActivity
    val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
    
    val biometricPrompt = androidx.biometric.BiometricPrompt(
        context,
        executor,
        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthenticated()
            }
        }
    )

    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle("MOD Store Lock")
        .setSubtitle("Use biometric or device PIN/Pattern to unlock")
        .setAllowedAuthenticators(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        biometricPrompt.authenticate(promptInfo)
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                contentDescription = null,
                modifier = androidx.compose.ui.Modifier.size(80.dp),
                tint = com.sorwe.store.ui.theme.CrimsonRed
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(24.dp))
            androidx.compose.material3.Text(
                "App Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.height(32.dp))
            androidx.compose.material3.Button(
                onClick = { biometricPrompt.authenticate(promptInfo) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = com.sorwe.store.ui.theme.CrimsonRed
                )
            ) {
                androidx.compose.material3.Text("Unlock App")
            }
        }
    }
}
