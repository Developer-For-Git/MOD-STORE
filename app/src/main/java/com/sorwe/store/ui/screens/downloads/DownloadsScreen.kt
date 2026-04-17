package com.sorwe.store.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sorwe.store.util.DownloadInfo
import com.sorwe.store.util.DownloadStatus
import com.sorwe.store.ui.theme.glassCard
import com.sorwe.store.ui.theme.bounceOnClick
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    "Downloads",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack, modifier = Modifier.bounceOnClick(onClick = onNavigateBack)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        if (downloads.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No active or completed downloads",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(downloads, key = { it.appName }) { info ->
                    DownloadCard(
                        info = info,
                        onCancel = { viewModel.cancelDownload(info.appName) },
                        onRemove = { viewModel.removeDownloadRecord(info.appName) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadCard(
    info: DownloadInfo,
    onCancel: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().glassCard(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.appName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                when (val status = info.status) {
                    is DownloadStatus.Pending -> Text("Pending...", style = MaterialTheme.typography.bodySmall)
                    is DownloadStatus.Downloading -> {
                        val progressPct = if (status.progress >= 0) (status.progress * 100).toInt() else 0
                        Text("Downloading: $progressPct%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (status.progress >= 0f) status.progress else 0f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    is DownloadStatus.Completed -> {
                        Text("Download Completed", style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color(0xFF10B981))
                    }
                    is DownloadStatus.Failed -> {
                        Text("Download Failed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    else -> {}
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            when (info.status) {
                is DownloadStatus.Downloading, is DownloadStatus.Pending -> {
                    IconButton(onClick = onCancel, modifier = Modifier.bounceOnClick(onClick = onCancel)) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                    }
                }
                is DownloadStatus.Completed, is DownloadStatus.Failed -> {
                    IconButton(onClick = onRemove, modifier = Modifier.bounceOnClick(onClick = onRemove)) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Record", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {}
            }
        }
    }
}
