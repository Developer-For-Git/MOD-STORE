package com.sorwe.store.ui.screens.onboarding

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.glassCard
import com.sorwe.store.ui.theme.bounceOnClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

@Composable
fun PermissionOnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    // Permission states
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasUsagePermission by remember { mutableStateOf(checkUsagePermission(context)) }
    var hasInstallPermission by remember { mutableStateOf(checkInstallPermission(context)) }
    var hasShizukuPermission by remember { mutableStateOf(checkShizukuPermission()) }
    var hasStoragePermission by remember { mutableStateOf(checkStoragePermission(context)) }
    
    // Biometric state from preferences
    val userPreferences = remember { 
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            com.sorwe.store.ui.screens.settings.SettingsEntryPoint::class.java
        ).userPreferences()
    }
    val biometricEnabledInPrefs by userPreferences.isBiometricLockEnabled.collectAsState(initial = false)
    val coroutineScope = rememberCoroutineScope()
    val isBiometricAvailable = remember { 
        val bm = androidx.biometric.BiometricManager.from(context)
        bm.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL) == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    // Launchers
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    // Refresh on return from settings
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = checkUsagePermission(context)
                hasInstallPermission = checkInstallPermission(context)
                hasShizukuPermission = checkShizukuPermission()
                hasStoragePermission = checkStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CrimsonRed.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = CrimsonRed
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Premium Setup",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Text(
            text = "Enable these features for the best experience",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(40.dp))

        // 1. Notifications (Essential for downloads)
        PermissionCard(
            title = "Notifications",
            description = "Stay updated on download and installation progress.",
            icon = Icons.Default.Notifications,
            isGranted = hasNotificationPermission,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    hasNotificationPermission = true
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Usage Stats (For Insights)
        PermissionCard(
            title = "Usage Insights",
            description = "Analyze your app usage habits and performance.",
            icon = Icons.Default.BarChart,
            isGranted = hasUsagePermission,
            onGrant = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Install Unknown Apps (For fallback installs)
        PermissionCard(
            title = "App Installation",
            description = "Allow the store to install apps directly to your device.",
            icon = Icons.Default.Download,
            isGranted = hasInstallPermission,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Storage (For Downloads & Cache)
        PermissionCard(
            title = "File Management",
            description = "Required to download and manage app installer files.",
            icon = Icons.Default.Storage,
            isGranted = hasStoragePermission,
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                } else {
                    // For older versions, we could use a standard launcher, but let's stick to settings for consistency in this UI
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Shizuku (Optional but recommended)
        PermissionCard(
            title = "Shizuku Service",
            description = "Enable silent, hands-free installations (Recommended).",
            icon = Icons.Default.Bolt,
            isGranted = hasShizukuPermission,
            onGrant = {
                try {
                    Shizuku.requestPermission(0)
                } catch (e: Exception) {
                    val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                    if (intent != null) context.startActivity(intent)
                    else {
                        val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=moe.shizuku.privileged.api"))
                        context.startActivity(playStoreIntent)
                    }
                }
            }
        )

        if (isBiometricAvailable) {
            Spacer(modifier = Modifier.height(16.dp))

            // 5. Biometric Vault (Security)
            PermissionCard(
                title = "Biometric Vault",
                description = "Secure your store with a fingerprint, face, or PIN lock.",
                icon = Icons.Default.Fingerprint,
                isGranted = biometricEnabledInPrefs,
                grantLabel = if (biometricEnabledInPrefs) "Enabled" else "Enable",
                onGrant = {
                    coroutineScope.launch {
                        userPreferences.setBiometricLockEnabled(!biometricEnabledInPrefs)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Restricted Settings (OEM fix)
        PermissionCard(
            title = "Advanced Access",
            description = "On some devices, you must enable 'Allow restricted settings' in App Info.",
            icon = Icons.Default.Settings,
            isGranted = false,
            grantLabel = "Open App Info",
            onGrant = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceOnClick(onClick = onComplete),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CrimsonRed
            )
        ) {
            Text(
                text = if (hasNotificationPermission && hasUsagePermission && hasInstallPermission && hasStoragePermission) "Let's Go!" else "Continue Anyway",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    grantLabel: String = "Grant",
    onGrant: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGranted) Color(0xFF4CAF50).copy(alpha = 0.1f) else CrimsonRed.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) Color(0xFF4CAF50) else CrimsonRed,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            if (isGranted && grantLabel == "Grant") {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(28.dp)
                )
            } else {
                TextButton(
                    onClick = onGrant,
                    colors = ButtonDefaults.textButtonColors(contentColor = if (isGranted) Color(0xFF4CAF50) else CrimsonRed)
                ) {
                    Text(if (isGranted) grantLabel else "Grant", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkUsagePermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun checkInstallPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.packageManager.canRequestPackageInstalls()
    } else {
        true
    }
}

private fun checkShizukuPermission(): Boolean {
    return try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (e: Exception) {
        false
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        android.os.Environment.isExternalStorageManager()
    } else {
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }
}
