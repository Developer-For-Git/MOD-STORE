package com.sorwe.store.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import com.sorwe.store.R
import com.sorwe.store.ui.theme.ErrorRed
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.GlassTeal
import com.sorwe.store.ui.theme.accentGradient
import com.sorwe.store.ui.theme.glassColors
import com.sorwe.store.ui.theme.bounceOnClick
import com.sorwe.store.util.DownloadStatus

@Composable
fun DownloadButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: String = "",
    label: String? = null,
    downloadStatus: DownloadStatus = DownloadStatus.Idle,
    onCancel: (() -> Unit)? = null,
    repoUrl: String? = null,
    onOpenRepo: (() -> Unit)? = null,
    isInstalled: Boolean = false,
    isUpdateAvailable: Boolean = false,
    onUninstall: (() -> Unit)? = null,
    onOpen: (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "downloadScale"
    )

    val colors = glassColors()
    val isDownloading = downloadStatus is DownloadStatus.Downloading
    val shape = RoundedCornerShape(16.dp)

    val containerColor by animateColorAsState(
        targetValue = when (downloadStatus) {
            is DownloadStatus.Completed -> GlassTeal
            is DownloadStatus.Failed -> ErrorRed
            is DownloadStatus.Downloading -> Color.Transparent
            is DownloadStatus.Pending -> Color.Transparent
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "buttonColor"
    )

    val useGradient = downloadStatus is DownloadStatus.Idle

    when {
        // ── Downloading state: unified progress bar with cancel ──
        isDownloading -> {
            val status = downloadStatus as DownloadStatus.Downloading
            val progress = status.progress
            val totalBytes = status.totalBytes

            // Animated progress 0f..1f
            val animatedProgress by animateFloatAsState(
                targetValue = if (progress in 0f..1f) progress else 0f,
                animationSpec = tween(300),
                label = "progress"
            )

            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(shape)
                    .background(colors.glassBg.copy(alpha = 0.5f))
                    .border(
                        width = 1.dp,
                        color = colors.glassBorder.copy(alpha = 0.3f),
                        shape = shape
                    )
            ) {
                // 1) The Liquid Progress Fill
                LiquidProgressBar(
                    progress = progress,
                    modifier = Modifier.fillMaxSize()
                )

                // 2) The Text Label (Centered)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val percent = (animatedProgress * 100).toInt().coerceIn(0, 100)
                    Text(
                        text = if (totalBytes > 0) "$percent%" else stringResource(R.string.downloading),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.shadow(2.dp, CircleShape)
                    )
                }

                // 3) Cancel Button (End-Aligned)
                if (onCancel != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(com.sorwe.store.ui.theme.DarkBackground.copy(alpha = 0.3f))
                            .clickable { onCancel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        // ── Pending state: queue status with cancel ──
        downloadStatus is DownloadStatus.Pending -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .scale(scale)
                    .clip(shape)
                    .background(Color.Gray.copy(alpha = 0.2f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = shape
                    )
            ) {
                // Centered text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Waiting in queue...",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                // Cancel Button (End-Aligned)
                if (onCancel != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(com.sorwe.store.ui.theme.DarkBackground.copy(alpha = 0.3f))
                            .clickable { onCancel() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // ── Installed state (No active download): split buttons for Open/Uninstall or Update/Uninstall ──
        isInstalled && (downloadStatus is DownloadStatus.Idle || downloadStatus is DownloadStatus.Failed || (downloadStatus is DownloadStatus.Completed && !isUpdateAvailable)) -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1) Secondary Action: Uninstall (Crimson Red)
                if (onUninstall != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(CrimsonRed.copy(alpha = 0.9f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
                            .clickable { onUninstall() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Uninstall",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2) Primary Action: Open (Green) OR Update (Teal/Accent)
                val primaryColor = if (isUpdateAvailable) GlassTeal else Color(0xFF00C853)
                val primaryLabel = if (isUpdateAvailable) "Update" else "Open"
                val primaryIcon = if (isUpdateAvailable) Icons.Default.Download else androidx.compose.material.icons.Icons.Default.PlayArrow

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(primaryColor)
                        .clickable { if (isUpdateAvailable) onClick() else onOpen?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = primaryIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = primaryLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Failed state (Not installed): split buttons ──
        downloadStatus is DownloadStatus.Failed -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(52.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1) Open Repo Button (Secondary)
                if (onOpenRepo != null && !repoUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(shape)
                            .background(com.sorwe.store.ui.theme.DarkBackground.copy(alpha = 0.6f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
                            .clickable { onOpenRepo() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Open Repo",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 2) Retry Button (Primary)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(ErrorRed)
                        .clickable { onClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.retry),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Other states (Idle, Completed) ──
        else -> {
            Button(
                onClick = onClick,
                modifier = modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .bounceOnClick(onClick = onClick)
                    .then(
                        if (useGradient) {
                            Modifier
                                .clip(shape)
                                .background(accentGradient())
                        } else {
                            Modifier
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.2f),
                                Color.White.copy(alpha = 0.05f)
                            )
                        ),
                        shape = shape
                    ),
                shape = shape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (useGradient) Color.Transparent else containerColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 24.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                when (downloadStatus) {
                    is DownloadStatus.Completed -> {
                        Icon(
                            imageVector = Icons.Default.InstallMobile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.install),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    is DownloadStatus.Idle -> {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label ?: if (size.isNotBlank()) "Download ($size)" else "Download",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB", mb)
}
