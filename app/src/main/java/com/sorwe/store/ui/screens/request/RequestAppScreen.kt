package com.sorwe.store.ui.screens.request

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.accentGradient
import com.sorwe.store.ui.theme.bounceOnClick
import com.sorwe.store.ui.theme.glassCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Arrangement

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestAppScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var appName by remember { mutableStateOf("") }
    var appLink by remember { mutableStateOf("") }
    var appReason by remember { mutableStateOf("") }

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
                    text = "Request an App",
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
            
            // Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 28.dp, shadowElevation = 4.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .padding(24.dp)
            ) {
                Column {
                    Surface(
                        color = CrimsonRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "NEW REQUEST",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = CrimsonRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Looking for an app or mod we don't have? Provide the details below and we'll send it directly to our development team!",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Form Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassCard(cornerRadius = 28.dp)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = appName,
                    onValueChange = { appName = it },
                    label = { Text("App Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonRed,
                        focusedLabelColor = CrimsonRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = appLink,
                    onValueChange = { appLink = it },
                    label = { Text("Play Store/Source Link (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CrimsonRed,
                        focusedLabelColor = CrimsonRed,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                OutlinedTextField(
                    value = appReason,
                    onValueChange = { appReason = it },
                    label = { Text("Specific features or mods? (Optional)") },
                    modifier = Modifier.fillMaxWidth().height(140.dp),
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
                    if (appName.isBlank()) {
                        Toast.makeText(context, "App name is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val subject = "App Request: $appName"
                    val body = """
                        App Name: $appName
                        
                        Source/Link: ${appLink.ifBlank { "Not provided" }}
                        
                        Details/Mods: ${appReason.ifBlank { "No specific details provided." }}
                        
                        ---
                        Sent from MOD Store (v${com.sorwe.store.BuildConfig.VERSION_NAME})
                    """.trimIndent()
                    
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:modstoredeveloperteam@outlook.com")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf("modstoredeveloperteam@outlook.com"))
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                    }

                    try {
                        context.startActivity(Intent.createChooser(intent, "Send Email Request..."))
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show()
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
                Icon(Icons.Default.Send, null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "SEND EMAIL REQUEST", 
                    fontSize = 15.sp, 
                    fontWeight = FontWeight.Black, 
                    letterSpacing = 1.sp
                )
            }
            
            Spacer(Modifier.height(40.dp))
        }
    }
}
