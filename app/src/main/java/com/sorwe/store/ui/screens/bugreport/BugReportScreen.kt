package com.sorwe.store.ui.screens.bugreport

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.accentGradient
import com.sorwe.store.ui.theme.bounceOnClick
import com.sorwe.store.ui.theme.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var bugSummary by remember { mutableStateOf("") }
    var bugDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Report a Bug",
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.onBackground
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            // 1. Tell about the bug (Summary)
            Text(
                text = "1. Tell us about the bug",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CrimsonRed,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = bugSummary,
                    onValueChange = { bugSummary = it },
                    placeholder = { Text("Short summary of the issue...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonRed,
                        focusedLabelColor = CrimsonRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(Modifier.height(24.dp))

            // 2. Video or Screenshot proof
            Text(
                text = "2. Please upload video or screenshot proof in email",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CrimsonRed,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "To help us fix the bug faster, please attach any relevant screenshots or videos directly in your email app before sending.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            // 3. Describe your bug
            Text(
                text = "3. Describe your bug",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CrimsonRed,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 24.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = bugDescription,
                    onValueChange = { bugDescription = it },
                    placeholder = { Text("Step by step details on how to reproduce...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonRed,
                        focusedLabelColor = CrimsonRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }

            Spacer(Modifier.height(32.dp))

            // Action Button
            Button(
                onClick = {
                    if (bugSummary.isBlank() || bugDescription.isBlank()) {
                        Toast.makeText(context, "Summary and Description are required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val body = """
                        Device: ${android.os.Build.MODEL}
                        Android Version: ${android.os.Build.VERSION.RELEASE}
                        App Version: ${com.sorwe.store.BuildConfig.VERSION_NAME}
                        
                        --- BUG SUMMARY ---
                        $bugSummary
                        
                        --- DESCRIPTION ---
                        $bugDescription
                        
                        ---
                        Sent from MOD Store Bug Reporter
                    """.trimIndent()

                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:modstoredeveloperteam@outlook.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Bug Report: $bugSummary")
                        putExtra(Intent.EXTRA_TEXT, body)
                    }

                    try {
                        context.startActivity(Intent.createChooser(intent, "Send Bug Report"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .bounceOnClick { }
                    .background(accentGradient(), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Send, null)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "SEND BUG REPORT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}
