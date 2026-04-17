package com.sorwe.store.ui.screens.settings

import android.content.Context
import android.os.Environment
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorwe.store.ui.theme.CrimsonRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class StorageStats(
    val cacheSize: Long = 0,
    val downloadsSize: Long = 0,
    val backupsSize: Long = 0,
    val totalSize: Long = 0
)

@Composable
fun StorageDashboard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var stats by remember { mutableStateOf<StorageStats?>(null) }
    var isClearing by remember { mutableStateOf(false) }

    // Load stats
    LaunchedEffect(isClearing) {
        if (!isClearing) {
            stats = calculateStorage(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val currentStats = stats
        if (currentStats != null) {
            StoragePieChart(stats = currentStats)
            Spacer(modifier = Modifier.height(24.dp))
            StorageLegend(stats = currentStats)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Clear Button
            Button(
                onClick = {
                    scope.launch {
                        isClearing = true
                        clearStorage(context)
                        isClearing = false
                        android.widget.Toast.makeText(context, "Storage optimized!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CrimsonRed.copy(alpha = 0.15f),
                    contentColor = CrimsonRed
                ),
                enabled = !isClearing && currentStats.totalSize > 0
            ) {
                if (isClearing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = CrimsonRed, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Optimize Storage", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            CircularProgressIndicator(color = CrimsonRed)
        }
    }
}

@Composable
private fun StoragePieChart(stats: StorageStats) {
    val cacheColor = Color(0xFF00F0FF) // Neon Blue
    val downloadsColor = CrimsonRed // Crimson Red
    val backupsColor = Color(0xFF00FF41) // Matrix Green
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    val total = if (stats.totalSize == 0L) 1f else stats.totalSize.toFloat()
    val cacheAngle = (stats.cacheSize / total) * 360f
    val downloadsAngle = (stats.downloadsSize / total) * 360f
    val backupsAngle = (stats.backupsSize / total) * 360f

    // Entrance animation
    val animSweep = remember { Animatable(0f) }
    LaunchedEffect(stats) {
        animSweep.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 18.dp.toPx()
            val size = Size(size.width, size.height)
            
            if (stats.totalSize == 0L) {
                drawArc(
                    color = emptyColor,
                    startAngle = 0f,
                    sweepAngle = 360f * animSweep.value,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                return@Canvas
            }

            var currentStartAngle = -90f
            
            // Draw Downloads
            if (downloadsAngle > 0) {
                drawArc(
                    color = downloadsColor,
                    startAngle = currentStartAngle,
                    sweepAngle = downloadsAngle * animSweep.value,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentStartAngle += downloadsAngle
            }

            // Draw Backups
            if (backupsAngle > 0) {
                drawArc(
                    color = backupsColor,
                    startAngle = currentStartAngle,
                    sweepAngle = backupsAngle * animSweep.value,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentStartAngle += backupsAngle
            }

            // Draw Cache
            if (cacheAngle > 0) {
                drawArc(
                    color = cacheColor,
                    startAngle = currentStartAngle,
                    sweepAngle = cacheAngle * animSweep.value,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatSize(stats.totalSize),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageLegend(stats: StorageStats) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LegendRow("Pending Downloads", formatSize(stats.downloadsSize), CrimsonRed)
        LegendRow("APK Backups", formatSize(stats.backupsSize), Color(0xFF00FF41))
        LegendRow("Image Cache", formatSize(stats.cacheSize), Color(0xFF00F0FF))
    }
}

@Composable
private fun LegendRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ─── UTILITIES ─────────────────────────────────────────────

private suspend fun calculateStorage(context: Context): StorageStats = withContext(Dispatchers.IO) {
    // 1. Cache (Coil etc)
    val cacheSize = context.cacheDir.calculateSizeRecursively()

    // 2. Pending Downloads (.apk in Android/data/.../files/Downloads)
    val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    val downloadsSize = appDownloadsDir?.calculateSizeRecursively() ?: 0L

    // 3. Backups (Downloads/MODstore_Backups)
    val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val backupsDir = File(publicDownloadsDir, "MODstore_Backups")
    val backupsSize = backupsDir.calculateSizeRecursively()

    StorageStats(
        cacheSize = cacheSize,
        downloadsSize = downloadsSize,
        backupsSize = backupsSize,
        totalSize = cacheSize + downloadsSize + backupsSize
    )
}

private suspend fun clearStorage(context: Context) = withContext(Dispatchers.IO) {
    // Clear only Cache and Pending Downloads. Backups should remain.
    context.cacheDir.deleteRecursively()
    
    val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    appDownloadsDir?.listFiles { file -> file.isFile && file.name.endsWith(".apk") }?.forEach { it.delete() }
}

private fun File.calculateSizeRecursively(): Long {
    if (!exists()) return 0
    if (isFile) return length()
    var size = 0L
    listFiles()?.forEach { child ->
        size += child.calculateSizeRecursively()
    }
    return size
}

private fun formatSize(bytes: Long): String {
    if (bytes == 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}
