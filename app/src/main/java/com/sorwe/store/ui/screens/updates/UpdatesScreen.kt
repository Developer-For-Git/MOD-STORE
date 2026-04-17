package com.sorwe.store.ui.screens.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sorwe.store.ui.components.DownloadButton
import com.sorwe.store.util.DownloadStatus
import com.sorwe.store.util.DownloadUtil
import com.sorwe.store.R
import com.sorwe.store.ui.theme.CrimsonRed

@Composable
fun UpdatesScreen(
    onNavigateBack: () -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: UpdatesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Refresh updates list when coming back to the app (e.g. after install)
                viewModel.checkForUpdates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(com.sorwe.store.ui.theme.DarkBackground.copy(alpha = 0.5f))
                    .padding(top = topPadding, bottom = 8.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Text(
                    text = "App Updates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                if (uiState.availableUpdates.isNotEmpty() && !uiState.isLoading) {
                    Button(
                        onClick = { viewModel.updateAll() },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("Update All")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CrimsonRed
                    )
                }
                
                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.checkForUpdates() }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }

                uiState.availableUpdates.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All apps are up to date",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.availableUpdates) { updateInfo ->
                            UpdateAppCard(
                                updateInfo = updateInfo,
                                viewModel = viewModel,
                                onClick = { onAppClick(updateInfo.app.id) }
                            )
                        }
                    }
                }
            }
            
            // ── Architecture Picker Dialog ──
            val currentApp = uiState.currentPickingApp
            if (uiState.showArchPicker && currentApp != null) {
                val tagName = uiState.archVariants.firstOrNull()?.tagName ?: currentApp.version
                com.sorwe.store.ui.screens.detail.ArchitecturePickerDialog(
                    tagName = tagName,
                    variants = uiState.archVariants.map { v ->
                        com.sorwe.store.data.remote.ApkAssetVariant(
                            label = v.label,
                            fileName = v.fileName,
                            downloadUrl = v.downloadUrl,
                            sizeBytes = v.sizeBytes,
                            tagName = v.tagName
                        )
                    },
                    isLoading = uiState.isLoadingVariants,
                    errorMessage = uiState.archPickerError,
                    repoUrl = currentApp.repoUrl,
                    onDismiss = { viewModel.dismissArchitecturePicker() },
                    onVariantSelected = { variant: com.sorwe.store.data.remote.ApkAssetVariant ->
                        viewModel.startDownload(currentApp.name, variant.downloadUrl)
                        viewModel.dismissArchitecturePicker()
                    }
                )
            }
        }
    }
}

@Composable
fun UpdateAppCard(
    updateInfo: AppUpdateInfo,
    viewModel: UpdatesViewModel,
    onClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val downloadInfo by viewModel.getDownloadState(updateInfo.app.name).collectAsState()
    val downloadStatus = downloadInfo.status
    val downloadedFile = downloadInfo.file
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(downloadStatus) {
        if (downloadStatus is DownloadStatus.Completed && com.sorwe.store.util.ShizukuInstaller.hasShizukuPermission()) {
            val file = downloadedFile ?: com.sorwe.store.util.DownloadUtil.findDownloadedApk(context, updateInfo.app.name)
            if (file != null && file.exists()) {
                try {
                    val pInfo = context.packageManager.getPackageInfo(updateInfo.app.id, 0)
                    viewModel.markPreInstallTime(updateInfo.app.id, pInfo.lastUpdateTime)
                } catch (e: Exception) {}
                
                com.sorwe.store.util.DownloadUtil.installApk(context, file) { success ->
                    if (success) {
                        viewModel.checkForUpdates()
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
            coil.compose.SubcomposeAsyncImage(
                model = updateInfo.app.icon.takeIf { it.isNotBlank() },
                contentDescription = updateInfo.app.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface),
                loading = { com.sorwe.store.ui.components.FallbackIcon(appName = updateInfo.app.name, modifier = Modifier.fillMaxSize()) },
                error = { com.sorwe.store.ui.components.FallbackIcon(appName = updateInfo.app.name, modifier = Modifier.fillMaxSize()) }
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = updateInfo.app.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = updateInfo.localVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "to",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = updateInfo.remoteVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            IconButton(
                onClick = { viewModel.ignoreUpdate(updateInfo.app.id, updateInfo.remoteVersion) }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Ignore Update",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        DownloadButton(
                repoUrl = updateInfo.app.repoUrl.takeIf { it.isNotBlank() },
                onOpenRepo = {
                    val repoIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(updateInfo.app.repoUrl)
                    )
                    context.startActivity(repoIntent)
                    android.widget.Toast.makeText(context, "Opening app repository…", android.widget.Toast.LENGTH_SHORT).show()
                },
                onClick = {
                    val platform = updateInfo.app.platform ?: ""
                    if (platform.equals("windows", ignoreCase = true) || 
                        platform.equals("pc", ignoreCase = true) || 
                        platform.equals("tv", ignoreCase = true)) {
                        
                        val repoUrlStr = updateInfo.app.repoUrl.takeIf { it.isNotBlank() } ?: "https://github.com/Developer-For-Git/MOD-STORE-DATA-"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(repoUrlStr))
                        context.startActivity(intent)
                        return@DownloadButton
                    }

                    if (downloadStatus is DownloadStatus.Idle || downloadStatus is DownloadStatus.Failed) {
                        if (!DownloadUtil.canInstallPackages(context)) {
                            DownloadUtil.requestInstallPermission(context)
                            return@DownloadButton
                        }
                        val downloadUrl = updateInfo.app.downloadUrl
                        if (downloadUrl.isBlank() || downloadUrl == "#") {
                            // Instead of redirecting to browser, use architecture picker
                            viewModel.showArchitecturePicker(updateInfo.app)
                            return@DownloadButton
                        }
                        viewModel.startDownload(updateInfo.app.name, downloadUrl)
                    } else if (downloadStatus is DownloadStatus.Completed) {
                        try {
                            val file = downloadedFile ?: DownloadUtil.findDownloadedApk(context, updateInfo.app.name)
                            if (file != null && file.exists()) {
                                try {
                                    val pInfo = context.packageManager.getPackageInfo(updateInfo.app.id, 0)
                                    viewModel.markPreInstallTime(updateInfo.app.id, pInfo.lastUpdateTime)
                                } catch (e: Exception) {
                                    // Ignore if not found
                                }
                                coroutineScope.launch {
                                    DownloadUtil.installApk(context, file) { success ->
                                        if (success) viewModel.checkForUpdates()
                                    }
                                }
                                // The ON_RESUME lifecycle event will automatically refresh the list upon return (if system installer is used)
                            } else {
                                viewModel.resetDownload(updateInfo.app.name)
                                android.widget.Toast.makeText(context, "File not found, please download again", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("UpdatesScreen", "Install error: ${e.message}", e)
                            viewModel.resetDownload(updateInfo.app.name)
                            android.widget.Toast.makeText(context, "Install failed", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onCancel = { viewModel.cancelDownload(updateInfo.app.name) },
                size = updateInfo.app.size,
                label = "Update",
                downloadStatus = downloadStatus,
                isInstalled = true, // We know it's installed if it's in the updates list
                isUpdateAvailable = true, // We know there's an update
                onUninstall = { com.sorwe.store.util.PackageUtil.uninstallApp(context, updateInfo.app.id) },
                onOpen = { com.sorwe.store.util.PackageUtil.launchApp(context, updateInfo.app.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            )
        }
    }
}
