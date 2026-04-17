package com.sorwe.store.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sorwe.store.R
import com.sorwe.store.data.preferences.UserPreferences
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.GlassTeal
import com.sorwe.store.ui.theme.accentGradient
import com.sorwe.store.ui.theme.glassCard
import com.sorwe.store.ui.theme.glassPill
import com.sorwe.store.ui.theme.bounceOnClick
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sorwe.store.util.ShizukuInstaller

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SettingsEntryPoint {
    fun userPreferences(): UserPreferences
}

@Composable
fun SettingsScreen(
    onNavigateToRepositories: () -> Unit,
    onRequestAppClick: () -> Unit,
    onLocalExplorerClick: () -> Unit,
    onLoginClick: () -> Unit,
    onReportBugClick: () -> Unit
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        SettingsEntryPoint::class.java
    )
    val userPreferences = entryPoint.userPreferences()
    val scope = rememberCoroutineScope()

    val isDarkMode by userPreferences.isDarkMode.collectAsState(initial = true)
    val accentColorHex by userPreferences.customAccentColor.collectAsState(initial = "#FF3B5C")
    val isAutoUpdateEnabled by userPreferences.isAutoUpdateEnabled.collectAsState(initial = false)
    val savedUrl by userPreferences.jsonUrl.collectAsState(initial = UserPreferences.DEFAULT_JSON_URL)
    var jsonUrlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    var showSaved by remember { mutableStateOf(false) }

    var isShizukuAvailable by remember { mutableStateOf(ShizukuInstaller.isShizukuAvailable()) }
    var hasShizukuPermission by remember { mutableStateOf(ShizukuInstaller.hasShizukuPermission()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isShizukuAvailable = ShizukuInstaller.isShizukuAvailable()
                hasShizukuPermission = ShizukuInstaller.hasShizukuPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        )

        // Settings Cards
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Account
            val githubUser by userPreferences.githubUserName.collectAsState(initial = "")
            val githubAvatar by userPreferences.githubUserAvatar.collectAsState(initial = null)
            
            SettingsGroup(title = "GitHub Account") {
                SettingsClickRow(
                    icon = Icons.Default.Code,
                    title = if (githubUser.isEmpty()) "Sign in to GitHub" else githubUser,
                    subtitle = if (githubUser.isEmpty()) "Increase API rate limits" else "Authorized (5,000 req/hr)",
                    onClick = onLoginClick
                )
            }

            // Section: Appearance
            SettingsGroup(title = "Appearance") {

                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(16.dp))
                
                // Accent Color Picker
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Personalize your app highlights",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val accentColors = listOf(
                        "#FF3B5C", // Crimson Red
                        "#00F0FF", // Neon Blue
                        "#00FF41", // Matrix Green
                        "#6C63FF", // Royal Purple
                        "#FFB300"  // Amber
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        accentColors.forEach { hex ->
                            val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { CrimsonRed }
                            val isSelected = hex.equals(accentColorHex, ignoreCase = true)
                            
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(color)
                                    .bounceOnClick { scope.launch { userPreferences.setCustomAccentColor(hex) } }
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, androidx.compose.foundation.shape.CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: Security
            SettingsGroup(title = "Security") {
                val isBiometricEnabled by userPreferences.isBiometricLockEnabled.collectAsState(initial = false)
                SettingsToggleRow(
                    icon = Icons.Default.Lock,
                    title = "Biometric Vault",
                    subtitle = "Require Fingerprint/Face ID to open app",
                    checked = isBiometricEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { userPreferences.setBiometricLockEnabled(enabled) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section: System
            SettingsGroup(title = "System") {
                SettingsToggleRow(
                    icon = Icons.Default.Refresh,
                    title = "Auto-Updates",
                    subtitle = "Check for newer versions automatically",
                    checked = isAutoUpdateEnabled,
                    onCheckedChange = { enabled ->
                        scope.launch { userPreferences.setAutoUpdateEnabled(enabled) }
                        if (enabled) {
                            val request = androidx.work.PeriodicWorkRequestBuilder<com.sorwe.store.worker.UpdateCheckWorker>(
                                24, java.util.concurrent.TimeUnit.HOURS
                            ).build()
                            androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                                "AutoUpdateCheck",
                                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                                request
                            )
                        } else {
                            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("AutoUpdateCheck")
                        }
                    }
                )
                
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsClickRow(
                    icon = Icons.Default.Link,
                    title = "Shizuku Integration",
                    subtitle = if (!isShizukuAvailable) "Shizuku is not running" else if (hasShizukuPermission) "Authorized (Silent Installs Available)" else "Running (Tap to Authorize)",
                    onClick = {
                        if (!isShizukuAvailable) {
                            android.widget.Toast.makeText(context, "Shizuku is not running. Please start Shizuku first.", android.widget.Toast.LENGTH_LONG).show()
                        } else if (!hasShizukuPermission) {
                            ShizukuInstaller.requestShizukuPermission()
                        } else {
                            android.widget.Toast.makeText(context, "Shizuku is already authorized!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                if (hasShizukuPermission) {
                    val isShizukuInstallerEnabled by userPreferences.isShizukuInstallerEnabled.collectAsState(initial = false)
                    SettingsToggleRow(
                        icon = Icons.Default.Save,
                        title = "Direct Installation",
                        subtitle = "Install apps silently without system prompts",
                        checked = isShizukuInstallerEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { userPreferences.setShizukuInstallerEnabled(enabled) }
                        }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsClickRow(
                    icon = Icons.Default.List,
                    title = "Custom Repositories",
                    subtitle = "Manage app metadata sources",
                    onClick = onNavigateToRepositories
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsClickRow(
                    icon = Icons.Default.SdCard,
                    title = "Local APK Explorer",
                    subtitle = "Install offline APKs from storage",
                    onClick = onLocalExplorerClick
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsClickRow(
                    icon = Icons.Default.Send,
                    title = "Request an App",
                    subtitle = "Ask for a new mod or app to be added",
                    onClick = onRequestAppClick
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SettingsClickRow(
                    icon = Icons.Default.BugReport,
                    title = "Report a Bug",
                    subtitle = "Found an issue? Let us know",
                    onClick = onReportBugClick
                )
            }

            // Section: Maintenance
            SettingsGroup(title = "Maintenance") {
                StorageDashboard()
            }

            // Section: About
            SettingsGroup(title = "Information") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .glassPill(cornerRadius = 12.dp)
                                .background(CrimsonRed.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, null, tint = CrimsonRed, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("MOD Store", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.app_version), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = PRIVACY_POLICY.take(200) + "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(140.dp))
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = CrimsonRed,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .glassCard(cornerRadius = 24.dp, shadowElevation = 2.dp)
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = CrimsonRed, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = CrimsonRed)
        )
    }
}

@Composable
private fun SettingsClickRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceOnClick(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = CrimsonRed, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

private const val PRIVACY_POLICY = """
Privacy Policy and Terms of Use

Welcome to MOD Store, an open-source application provided for educational and informational purposes only. By accessing, downloading, installing, or using MOD Store, you acknowledge that you have read, understood, and agree to be legally bound by this Privacy Policy and Terms of Use. If you do not agree to these terms, you must immediately discontinue use of the application.

MOD Store is an open-source software project. The source code may be publicly accessible for transparency, research, and educational development. The application does not host, distribute, promote, or facilitate access to cracked software, modded applications, pirated content, unauthorized premium services, or any material that infringes intellectual property rights. MOD Store does not provide paid applications, subscription services, or premium features of any third-party service free of charge. The app does not bypass, disable, or interfere with licensing systems, payment systems, or digital rights management protections.

Users are solely and fully responsible for their actions while using MOD Store. Any third-party applications, websites, files, or services accessed through references or links are used entirely at the user’s own discretion and risk. The developers of MOD Store do not control, monitor, endorse, or assume responsibility for third-party content or services.

MOD Store does not knowingly host or distribute illegal content. If any content is found to violate applicable copyright, trademark, or intellectual property laws, it will be reviewed and removed in accordance with applicable legal requirements upon receiving valid notice. All trademarks, service marks, logos, and brand names mentioned within the application remain the property of their respective owners. MOD Store is not affiliated with, endorsed by, sponsored by, or officially connected with Google or any other company unless explicitly stated.

The application may collect limited non-personal technical information, including but not limited to device type, operating system, usage data, performance logs, and crash reports for the purpose of maintaining functionality, improving user experience, and ensuring security. Any information voluntarily provided by users, such as contact details submitted through support communication, will be used solely for communication and support purposes. We do not sell, rent, or trade personal information. Where third-party analytics or advertising services are used, such services may collect data in accordance with their own privacy policies and applicable data protection laws.

To the maximum extent permitted by applicable law in any jurisdiction, MOD Store and its developers disclaim all warranties, express or implied, including but not limited to warranties of merchantability, fitness for a particular purpose, non-infringement, accuracy, reliability, or availability. The application is provided on an “as is” and “as available” basis without warranties of any kind.

Under no circumstances shall the developers, contributors, or affiliates of MOD Store be liable for any direct, indirect, incidental, consequential, special, exemplary, or punitive damages arising out of or related to the use of, or inability to use, the application, even if advised of the possibility of such damages. Users assume full responsibility and risk for their use of the application.

Users agree not to misuse the application, attempt unauthorized access, interfere with functionality, reverse engineer (except where permitted by open-source licensing), exploit vulnerabilities, or use the app for unlawful purposes. Any violation of these terms may result in termination of access and potential legal action under applicable laws.

Users are responsible for ensuring that their use of MOD Store complies with all local, national, and international laws and regulations applicable in their jurisdiction. If any provision of these Terms is found to be invalid or unenforceable under applicable law, the remaining provisions shall remain in full force and effect.

We reserve the right to modify, update, or replace this Privacy Policy and Terms of Use at any time without prior notice. Continued use of MOD Store after changes are published constitutes acceptance of those changes.
"""
