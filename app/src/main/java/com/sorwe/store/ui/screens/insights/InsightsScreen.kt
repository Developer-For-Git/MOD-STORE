package com.sorwe.store.ui.screens.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0x0D0D0D),
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Column(modifier = Modifier.fillMaxSize()) {
                    InsightsHeader(onBack = onBack)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CrimsonRed)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        InsightsHeader(
                            onBack = onBack,
                            modifier = Modifier.padding(horizontal = 0.dp)
                        )
                    }

                    item {
                        StoreStatsOverviewCard(
                            totalMods = uiState.totalModsCount,
                            savedMB = uiState.totalStorageSavedMB
                        )
                    }
                    
                    if (uiState.mostUpdatedApps.isNotEmpty()) {
                        item {
                            SectionHeader("Update Hall of Fame", Icons.Default.AutoAwesome)
                        }

                        items(uiState.mostUpdatedApps) { app ->
                            StoreInsightItem(
                                app = app,
                                valueLabel = "Updates",
                                valueText = "${app.updateCount}"
                            )
                        }
                    }

                    if (uiState.newestApps.isNotEmpty()) {
                        item {
                            SectionHeader("Fresh Arrivals", Icons.Default.CloudDownload)
                        }

                        items(uiState.newestApps) { app ->
                            StoreInsightItem(
                                app = app,
                                valueLabel = "Added",
                                valueText = formatDate(app.addedAt)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InsightsHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text("Store Insights", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        modifier = modifier.statusBarsPadding()
    )
}

@Composable
fun StoreStatsOverviewCard(totalMods: Int, savedMB: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 24.dp)
            .padding(24.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, null, tint = CrimsonRed, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Storage Impact", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val displaySaved = if (savedMB > 1024) String.format("%.1f GB", savedMB / 1024f) else String.format("%.0f MB", savedMB)
            Text(
                text = "${displaySaved} saved",
                style = MaterialTheme.typography.displaySmall,
                color = CrimsonRed,
                fontWeight = FontWeight.Black
            )
            Text(
                "Optimization across $totalMods apps in your store",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, null, tint = CrimsonRed, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun StoreInsightItem(app: AppItem, valueLabel: String, valueText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, fontWeight = FontWeight.Bold, color = Color.White)
                Text(app.developer, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    valueText,
                    fontWeight = FontWeight.Black,
                    color = CrimsonRed,
                    fontSize = 18.sp
                )
                Text(
                    valueLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Long ago"
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> "$days days ago"
        days < 30L -> "${days / 7} weeks ago"
        else -> "${days / 30} months ago"
    }
}
