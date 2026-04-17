package com.sorwe.store.ui.screens.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sorwe.store.R
import com.sorwe.store.ui.components.DownloadButton
import com.sorwe.store.ui.components.ScreenshotCarousel
import com.sorwe.store.ui.components.shimmerEffect
import com.sorwe.store.ui.theme.Coral40
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.glassCard
import com.sorwe.store.ui.theme.glassPill
import com.sorwe.store.ui.theme.bounceOnClick
import com.sorwe.store.util.DownloadStatus
import com.sorwe.store.util.DownloadUtil
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sorwe.store.util.PackageUtil
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    // Architecture picker dialog
    if (uiState.showArchPicker) {
        val tagName = uiState.archVariants.firstOrNull()?.tagName ?: ""
        ArchitecturePickerDialog(
            tagName = tagName,
            variants = uiState.archVariants,
            isLoading = uiState.isLoadingVariants,
            errorMessage = uiState.archPickerError,
            repoUrl = uiState.app?.repoUrl,
            onVariantSelected = { variant ->
                viewModel.dismissArchitecturePicker()
                viewModel.startDownload(variant.downloadUrl)
            },
            onDismiss = { viewModel.dismissArchitecturePicker() }
        )
    }

    // ── Observe the application-scoped DownloadManager state ──
    // This survives navigation: if the user leaves and comes back,
    // the download state (progress / completed) is preserved.
    val downloadInfo by remember(uiState.app?.name) {
        viewModel.getDownloadState()
    }.collectAsStateWithLifecycle()

    val downloadStatus = downloadInfo.status
    val downloadedFile = downloadInfo.file

    val favoriteColor by animateColorAsState(
        targetValue = if (uiState.isFavorite) Coral40 else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "favColor"
    )

    if (uiState.isLoading) {
        AppDetailSkeleton(onNavigateBack = onNavigateBack)
        return
    }

    val app = uiState.app ?: return

    val lifecycle = LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkInstallation()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val scrollBehavior = androidx.compose.material3.TopAppBarDefaults.pinnedScrollBehavior()

    androidx.compose.material3.Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::toggleFavorite,
                        modifier = Modifier.bounceOnClick(onClick = viewModel::toggleFavorite)
                    ) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = favoriteColor
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
            // --- Premium Hero Section ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .graphicsLayer {
                            // Parallax: background moves slower than foreground
                            translationY = scrollState.value * 0.4f
                            alpha = (1f - (scrollState.value / 600f)).coerceIn(0f, 1f)
                        }
                ) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    CrimsonRed.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .graphicsLayer {
                                // Subtle scale down as we scroll
                                val scaleFactor = (1f - (scrollState.value / 1200f)).coerceAtLeast(0.85f)
                                scaleX = scaleFactor
                                scaleY = scaleFactor
                                translationY = -scrollState.value * 0.1f
                            }
                            .glassCard(cornerRadius = 28.dp, shadowElevation = 12.dp)
                            .padding(4.dp)
                    ) {
                        coil.compose.SubcomposeAsyncImage(
                            model = remember(app.icon) {
                                coil.request.ImageRequest.Builder(context)
                                    .data(app.icon.takeIf { it.isNotBlank() })
                                    .size(512)
                                    .crossfade(true)
                                    .build()
                            },
                            contentDescription = app.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop,
                            loading = { com.sorwe.store.ui.components.FallbackIcon(appName = app.name, modifier = Modifier.fillMaxSize()) },
                            error = { com.sorwe.store.ui.components.FallbackIcon(appName = app.name, modifier = Modifier.fillMaxSize()) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    if (app.developer.isNotBlank()) {
                        Text(
                            text = app.developer,
                            style = MaterialTheme.typography.labelLarge,
                            color = CrimsonRed.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        app.category.split(",").take(2).forEach { cat ->
                            val trimmedCat = cat.trim()
                            if (trimmedCat.isNotBlank()) {
                                Text(
                                    text = trimmedCat,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .glassPill(cornerRadius = 8.dp)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GlassInfoChip(icon = Icons.Default.Info, label = "Version", value = app.version)
                GlassInfoChip(icon = Icons.Default.Star, label = "Rating", value = if (app.rating > 0) String.format("%.1f", app.rating) else "N/A")
                GlassInfoChip(icon = Icons.Default.Check, label = "Size", value = app.size)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Consolidation: Single DownloadButton for all app states
            DownloadButton(
                repoUrl = app.repoUrl.takeIf { it.isNotBlank() },
                onOpenRepo = {
                    val repoIntent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(app.repoUrl)
                    )
                    context.startActivity(repoIntent)
                    android.widget.Toast.makeText(context, "Opening app repository…", android.widget.Toast.LENGTH_SHORT).show()
                },
                onClick = {
                    val platform = app.platform ?: ""
                    if (platform.equals("windows", ignoreCase = true) || 
                        platform.equals("pc", ignoreCase = true) || 
                        platform.equals("tv", ignoreCase = true)) {
                        
                        val repoUrlStr = app.repoUrl.takeIf { it.isNotBlank() } ?: "https://github.com/Developer-For-Git/MOD-STORE-DATA-"
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(repoUrlStr))
                        context.startActivity(intent)
                        return@DownloadButton
                    }

                    // ── Android apps: normal download / install flow ──
                    when (downloadStatus) {
                        is DownloadStatus.Pending -> { /* ignore taps while pending */ }
                        is DownloadStatus.Idle, is DownloadStatus.Failed -> {
                            // 1. Check install permission
                            if (!DownloadUtil.canInstallPackages(context)) {
                                DownloadUtil.requestInstallPermission(context)
                                return@DownloadButton
                            }
                            // 2. No stored download URL — try the architecture picker which
                            // fetches APK variants directly from GitHub API via the token.
                            val downloadUrl = app.downloadUrl
                            if (downloadUrl.isBlank() || downloadUrl == "#") {
                                if (uiState.isResolving) {
                                    android.widget.Toast.makeText(context, "Still fetching download link...", android.widget.Toast.LENGTH_SHORT).show()
                                    return@DownloadButton
                                }
                                viewModel.showArchitecturePicker()
                                return@DownloadButton
                            }
                            val scheme = android.net.Uri.parse(downloadUrl).scheme
                            if (scheme != "http" && scheme != "https") {
                                // Invalid scheme — try architecture picker as a resolver
                                viewModel.showArchitecturePicker()
                                return@DownloadButton
                            }
                            // 3. Show architecture picker to let user choose the right APK variant
                            viewModel.showArchitecturePicker()
                        }
                        is DownloadStatus.Completed -> {
                            try {
                                if (!DownloadUtil.canInstallPackages(context)) {
                                    DownloadUtil.requestInstallPermission(context)
                                } else {
                                    val file = downloadedFile ?: DownloadUtil.findDownloadedApk(context, app.name)
                                    if (file != null && file.exists()) {
                                        // Validate APK before installing
                                        val isValidApk = try {
                                            file.inputStream().use { stream ->
                                                val header = ByteArray(4)
                                                stream.read(header) == 4 &&
                                                    header[0] == 0x50.toByte() &&
                                                    header[1] == 0x4B.toByte() &&
                                                    header[2] == 0x03.toByte() &&
                                                    header[3] == 0x04.toByte()
                                            }
                                        } catch (e: Exception) { false }

                                        if (isValidApk) {
                                            coroutineScope.launch { DownloadUtil.installApk(context, file) }
                                        } else {
                                            // File is corrupted — delete and reset
                                            file.delete()
                                            viewModel.resetDownload()
                                            android.widget.Toast.makeText(context, "Downloaded file is corrupted, please download again", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        viewModel.resetDownload()
                                        android.widget.Toast.makeText(context, "File not found, please download again", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("DetailScreen", "Install error: ${e.message}", e)
                                // Delete bad file and reset to download state
                                val file = downloadedFile ?: DownloadUtil.findDownloadedApk(context, app.name)
                                file?.delete()
                                viewModel.resetDownload()
                                android.widget.Toast.makeText(context, "Install failed, please download again", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                        is DownloadStatus.Downloading -> { /* ignore taps while downloading */ }
                        else -> {}
                    }
                },
                onCancel = {
                    // Cancel via DownloadManager — cancels OkHttp call + coroutine + dismisses notification
                    viewModel.cancelDownload()
                },
                size = app.size,
                label = when {
                    uiState.isResolving -> "Fetching download link..."
                    app.platform.equals("PC", ignoreCase = true) ||
                    app.platform.equals("TV", ignoreCase = true) -> "Download / Get"
                    else -> null
                },
                downloadStatus = downloadStatus,
                isInstalled = uiState.isInstalled,
                isUpdateAvailable = uiState.isUpdateAvailable,
                onUninstall = { PackageUtil.uninstallApp(context, uiState.targetPackageName) },
                onOpen = { PackageUtil.launchApp(context, uiState.targetPackageName) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            if (uiState.apkExists) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { 
                        viewModel.deleteApk()
                        android.widget.Toast.makeText(context, "APK file deleted", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        contentColor = CrimsonRed
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete APK File",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Screenshots
            if (app.screenshots.isNotEmpty()) {
                SectionHeader(title = stringResource(R.string.screenshots_label))
                Spacer(modifier = Modifier.height(8.dp))
                ScreenshotCarousel(screenshots = app.screenshots)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Description
            SectionHeader(title = stringResource(R.string.description_label))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            // Changelog
            if (app.changelog.isNotBlank()) {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = stringResource(R.string.changelog_label))
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .glassCard(cornerRadius = 16.dp, shadowElevation = 4.dp)
                ) {
                    dev.jeziellago.compose.markdowntext.MarkdownText(
                        markdown = app.changelog,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun GlassInfoChip(icon: ImageVector, label: String, value: String) {
    Box(
        modifier = Modifier
            .glassCard(cornerRadius = 16.dp, shadowElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = CrimsonRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSkeleton(onNavigateBack: () -> Unit) {
    androidx.compose.material3.Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = onNavigateBack) {
                        androidx.compose.material3.Icon(imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                // Icon skeleton
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                        .shimmerEffect()
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(20.dp))
                androidx.compose.foundation.layout.Column {
                    // Title skeleton
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(28.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .shimmerEffect()
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    // Developer skeleton
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .shimmerEffect()
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            
            // Stats row skeleton
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                repeat(3) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(40.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                            .shimmerEffect()
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            
            // Install button skeleton
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                    .shimmerEffect()
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(32.dp))
            
            // Description skeleton
            repeat(4) { idx ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxWidth(if (idx == 3) 0.6f else 1f)
                        .height(14.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
