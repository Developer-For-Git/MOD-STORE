package com.sorwe.store.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sorwe.store.data.remote.ReleaseNotesDto
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.accentGradient
import com.sorwe.store.ui.theme.softAccentGradient
import com.sorwe.store.ui.theme.glassCard
import com.sorwe.store.ui.theme.glassPill
import com.sorwe.store.ui.theme.bounceOnClick
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import com.sorwe.store.util.DownloadInfo
import com.sorwe.store.util.DownloadStatus
import com.sorwe.store.util.DownloadUtil
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun UpdateDialog(
    releaseNotes: ReleaseNotesDto,
    onUpdate: () -> Unit,
    downloadInfo: DownloadInfo? = null,
    onDismissRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F0F0F),
                            Color(0xFF1A0A0A),
                            Color(0xFF0A0A0A)
                        )
                    )
                )
        ) {
            // Background Decorative Element
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 100.dp, y = (-50).dp)
                    .size(300.dp)
                    .background(CrimsonRed.copy(alpha = 0.08f), CircleShape)
                    .blur(100.dp)
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- Top Header ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NEW UPDATE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            color = CrimsonRed,
                            letterSpacing = 2.sp
                        )
                        
                        Surface(
                            color = Color.White.copy(alpha = 0.08f),
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = "v${releaseNotes.version}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // Glowy Icon Header
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .background(softAccentGradient(), RoundedCornerShape(40.dp))
                                .blur(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .glassPill(cornerRadius = 30.dp)
                                .background(accentGradient()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = releaseNotes.title,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 42.sp
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Change List Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 28.dp)
                            .background(Color.White.copy(alpha = 0.03f))
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "WHAT'S NEW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.5f),
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        releaseNotes.changes.forEachIndexed { index, change ->
                            val emoji = when {
                                change.contains("fix", ignoreCase = true) -> "🔧 "
                                change.contains("new", ignoreCase = true) || change.contains("add", ignoreCase = true) -> "✨ "
                                change.contains("speed", ignoreCase = true) || change.contains("performance", ignoreCase = true) -> "⚡ "
                                change.contains("ui", ignoreCase = true) || change.contains("design", ignoreCase = true) -> "🎨 "
                                else -> "🚀 "
                            }
                            
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = emoji,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = change,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    lineHeight = 24.sp
                                )
                            }
                            if (index < releaseNotes.changes.size - 1) {
                                Divider(
                                    color = Color.White.copy(alpha = 0.05f),
                                    modifier = Modifier.padding(start = 32.dp, top = 4.dp, bottom = 4.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
                }

                // --- Immersive Footer ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                            )
                        )
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val status = downloadInfo?.status ?: DownloadStatus.Idle
                        val isDownloading = status is DownloadStatus.Downloading
                        val isCompleted = status is DownloadStatus.Completed

                        if (isDownloading) {
                            val progressStatus = status as DownloadStatus.Downloading
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = if (progressStatus.progress >= 0) progressStatus.progress else 0f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(CircleShape),
                                    color = CrimsonRed,
                                    trackColor = CrimsonRed.copy(alpha = 0.1f)
                                )
                                Text(
                                    text = if (progressStatus.progress >= 0) 
                                        "DOWNLOADING ${ (progressStatus.progress * 100).toInt()}%" 
                                        else "PREPARING...",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CrimsonRed,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isCompleted) {
                                    val file = downloadInfo?.file ?: DownloadUtil.findDownloadedApk(context, "MOD Store")
                                    if (file != null) {
                                        coroutineScope.launch {
                                            DownloadUtil.installApk(context, file)
                                        }
                                    }
                                } else if (!isDownloading) {
                                    onUpdate()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .bounceOnClick { },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDownloading) Color.Transparent else Color.White,
                                contentColor = if (isDownloading) Color.White else Color.Black,
                                disabledContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            enabled = !isDownloading,
                            shape = RoundedCornerShape(22.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isDownloading) SolidColor(Color.Transparent) 
                                        else if (isCompleted) accentGradient() else SolidColor(Color.White)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isDownloading -> "PLEASE WAIT..."
                                        isCompleted -> "INSTALL UPDATE"
                                        else -> "UPDATE NOW"
                                    },
                                    color = if (isDownloading) Color.White.copy(alpha = 0.5f) 
                                            else if (isCompleted) Color.White else Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    letterSpacing = 1.2.sp
                                )
                            }
                        }
                        
                        
                        Text(
                            text = "Update required for security & stability",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
