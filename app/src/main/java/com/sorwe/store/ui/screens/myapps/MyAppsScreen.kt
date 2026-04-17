package com.sorwe.store.ui.screens.myapps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.sorwe.store.R
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.glassCard
import com.sorwe.store.util.DownloadUtil
import com.sorwe.store.util.PackageUtil
import kotlinx.coroutines.launch

private enum class MyAppsPage { MENU, INSTALLED, DOWNLOADED, FILES }

@Composable
fun MyAppsScreen(
    onNavigateBack: () -> Unit,
    onAppClick: (String) -> Unit,
    viewModel: MyAppsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentPage by remember { mutableStateOf(MyAppsPage.MENU) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val exportLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportApps(context, uri)
            android.widget.Toast.makeText(context, "Apps exported successfully", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importApps(context, uri) { importedApps ->
                android.widget.Toast.makeText(context, "Imported ${importedApps.size} apps. Go to Home to download them.", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (currentPage == MyAppsPage.MENU) onNavigateBack()
                else currentPage = MyAppsPage.MENU
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = when (currentPage) {
                    MyAppsPage.MENU -> "My Apps"
                    MyAppsPage.INSTALLED -> "Installed Apps"
                    MyAppsPage.DOWNLOADED -> "Downloaded"
                    MyAppsPage.FILES -> "APK Files"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            
            if (currentPage == MyAppsPage.MENU) {
                IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                    Icon(Icons.Default.Add, "Import Backup", tint = CrimsonRed)
                }
                IconButton(onClick = { exportLauncher.launch("sorwe_apps_backup.json") }) {
                    Icon(Icons.Default.Share, "Export Backup", tint = CrimsonRed)
                }
            }
            
            if (currentPage != MyAppsPage.MENU) {
                IconButton(onClick = viewModel::load) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = CrimsonRed)
                }
            }
            if (currentPage == MyAppsPage.FILES && uiState.leftoverApkFiles.isNotEmpty()) {
                IconButton(onClick = { showDeleteAllDialog = true }) {
                    Icon(Icons.Default.Delete, "Delete All", tint = CrimsonRed)
                }
            }
        }

        // ── Animated Content ─────────────────────────────────────────────
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == MyAppsPage.MENU) {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                            (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it } + fadeOut())
                }
            },
            label = "pageTransition"
        ) { page ->
            when (page) {
                MyAppsPage.MENU -> MenuPage(uiState,
                    onInstalledClick = { currentPage = MyAppsPage.INSTALLED },
                    onDownloadedClick = { currentPage = MyAppsPage.DOWNLOADED },
                    onFilesClick = { currentPage = MyAppsPage.FILES })
                MyAppsPage.INSTALLED -> InstalledGrid(uiState.installedApps, uiState.updatesAvailable.size, onAppClick, { viewModel.updateAll(context) }, uiState.isLoading)
                MyAppsPage.DOWNLOADED -> DownloadedGrid(uiState.downloadedNotInstalled, onAppClick, uiState.isLoading)
                MyAppsPage.FILES -> ApkFilesGrid(uiState.leftoverApkFiles, viewModel::deleteApkFile, uiState.isLoading)
            }
        }
    }

    // ── Delete All dialog ────────────────────────────────────────────────
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Delete All APK Files?") },
            text = { Text("This will delete all ${uiState.leftoverApkFiles.size} APK files from Downloads.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllApkFiles(); showDeleteAllDialog = false }) {
                    Text("Delete All", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteAllDialog = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  MENU — Settings-style cards
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MenuPage(
    uiState: MyAppsUiState,
    onInstalledClick: () -> Unit,
    onDownloadedClick: () -> Unit,
    onFilesClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MenuCard(Icons.Default.CheckCircle, Color(0xFF00C853), "Installed",
            "${uiState.installedApps.size} apps installed via Store", onInstalledClick)
        MenuCard(Icons.Default.Download, Color(0xFF2979FF), "Downloaded",
            "${uiState.downloadedNotInstalled.size} APKs not yet installed", onDownloadedClick)
        MenuCard(Icons.Default.Storage, Color(0xFFFF6D00), "APK Files",
            "${uiState.leftoverApkFiles.size} files in Downloads", onFilesClick)
    }
}

@Composable
private fun MenuCard(
    icon: ImageVector, iconBgColor: Color, title: String, subtitle: String, onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(20.dp), tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(iconBgColor, iconBgColor.copy(alpha = 0.7f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground, fontSize = 17.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  GRID PAGES — Same card style as Home screen
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun InstalledGrid(apps: List<AppItem>, updatesCount: Int, onAppClick: (String) -> Unit, onUpdateAll: () -> Unit, isLoading: Boolean) {
    val context = LocalContext.current
    if (isLoading) { LoadingBox(); return }
    if (apps.isEmpty()) { EmptyState(Icons.Default.PhoneAndroid, "No apps installed via Store yet"); return }

    Column(modifier = Modifier.fillMaxSize()) {
        if (updatesCount > 0) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onUpdateAll),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Updates Available", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("$updatesCount apps can be updated", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Text("Update All", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        items(apps, key = { it.id }) { app ->
            MyAppCard(
                app = app,
                onClick = { onAppClick(app.id) },
                actionLabel = "Open",
                actionColor = Color(0xFF00C853),
                onAction = {
                    val pkg = PackageUtil.findPackageNameByLabel(context, app.name) ?: app.id
                    PackageUtil.launchApp(context, pkg)
                },
                secondaryActionLabel = "Uninstall",
                secondaryActionColor = MaterialTheme.colorScheme.primary,
                onSecondaryAction = {
                    val pkg = PackageUtil.findPackageNameByLabel(context, app.name) ?: app.id
                    PackageUtil.uninstallApp(context, pkg)
                },
                onBackup = {
                    val pkg = PackageUtil.findPackageNameByLabel(context, app.name) ?: app.id
                    (context as? androidx.activity.ComponentActivity)?.let {
                        androidx.lifecycle.ViewModelProvider(it)[MyAppsViewModel::class.java].backupApk(context, pkg, app.name)
                    }
                }
            )
        }
    }
}
}

@Composable
private fun DownloadedGrid(apps: List<AppItem>, onAppClick: (String) -> Unit, isLoading: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    if (isLoading) { LoadingBox(); return }
    if (apps.isEmpty()) { EmptyState(Icons.Default.Download, "No downloaded APKs waiting to install"); return }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(apps, key = { it.id }) { app ->
            MyAppCard(
                app = app,
                onClick = { onAppClick(app.id) },
                actionLabel = "Install",
                actionColor = CrimsonRed,
                onAction = {
                    val file = DownloadUtil.findDownloadedApk(context, app.name)
                    if (file != null) coroutineScope.launch { DownloadUtil.installApk(context, file) }
                }
            )
        }
    }
}

@Composable
private fun ApkFilesGrid(
    files: List<ApkFileInfo>, onDelete: (ApkFileInfo) -> Unit, isLoading: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    if (isLoading) { LoadingBox(); return }
    if (files.isEmpty()) { EmptyState(Icons.Default.Storage, "No APK files in Downloads"); return }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(files, key = { it.file.absolutePath }) { info ->
            val sizeKb = info.file.length() / 1024
            val sizeText = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB"

            ApkFileCard(
                info = info,
                sizeText = sizeText,
                onInstall = { coroutineScope.launch { DownloadUtil.installApk(context, info.file) } },
                onDelete = { onDelete(info) }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  CARD COMPOSABLES
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MyAppCard(
    app: AppItem,
    onClick: () -> Unit,
    actionLabel: String,
    actionColor: Color,
    onAction: () -> Unit,
    secondaryActionLabel: String? = null,
    secondaryActionColor: Color? = null,
    onSecondaryAction: (() -> Unit)? = null,
    onBackup: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val cornerRadiusPx = remember { 18f * context.resources.displayMetrics.density }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .clickable(onClick = onClick)
    ) {
        if (onBackup != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .clickable(onClick = onBackup),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Save, "Backup", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            coil.compose.SubcomposeAsyncImage(
                model = remember(app.icon) {
                    ImageRequest.Builder(context)
                        .data(app.icon.takeIf { it.isNotBlank() })
                        .size(256)
                        .crossfade(true)
                        .transformations(RoundedCornersTransformation(cornerRadiusPx))
                        .build()
                },
                contentDescription = app.name,
                modifier = Modifier.size(72.dp),
                contentScale = ContentScale.Crop,
                loading = { com.sorwe.store.ui.components.FallbackIcon(appName = app.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) },
                error = { com.sorwe.store.ui.components.FallbackIcon(appName = app.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) }
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = app.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = app.size,
                style = MaterialTheme.typography.labelSmall,
                color = CrimsonRed,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Action button
            if (secondaryActionLabel != null && secondaryActionColor != null && onSecondaryAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onAction),
                        color = actionColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = actionLabel,
                            color = actionColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onSecondaryAction),
                        color = secondaryActionColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = secondaryActionLabel,
                            color = secondaryActionColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onAction),
                    color = actionColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = actionLabel,
                        color = actionColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ApkFileCard(
    info: ApkFileInfo,
    sizeText: String,
    onInstall: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val cornerRadiusPx = remember { 18f * context.resources.displayMetrics.density }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
    ) {
        // Delete icon top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CrimsonRed.copy(alpha = 0.15f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Delete, "Delete", tint = CrimsonRed, modifier = Modifier.size(18.dp))
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            if (info.matchedApp != null && info.matchedApp.icon.isNotBlank()) {
                coil.compose.SubcomposeAsyncImage(
                    model = remember(info.matchedApp.icon) {
                        ImageRequest.Builder(context)
                            .data(info.matchedApp.icon.takeIf { it.isNotBlank() })
                            .size(256)
                            .crossfade(true)
                            .transformations(RoundedCornersTransformation(cornerRadiusPx))
                            .build()
                    },
                    contentDescription = info.appName,
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Crop,
                    loading = { com.sorwe.store.ui.components.FallbackIcon(appName = info.appName, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) },
                    error = { com.sorwe.store.ui.components.FallbackIcon(appName = info.appName, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))) }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(CrimsonRed.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Storage, null, tint = CrimsonRed, modifier = Modifier.size(32.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = info.appName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = sizeText,
                style = MaterialTheme.typography.labelSmall,
                color = CrimsonRed, fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            // Install button
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onInstall),
                color = Color(0xFF00C853).copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Install", color = Color(0xFF00C853), fontWeight = FontWeight.Bold,
                    fontSize = 13.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  SHARED
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EmptyState(icon: ImageVector, message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f))
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = CrimsonRed,
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}
