package com.sorwe.store.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue

@Composable
fun FallbackIcon(
    appName: String,
    modifier: Modifier = Modifier
) {
    // Determine the initial character, fallback to 'A' if empty/blank
    val cleanName = appName.trim()
    val initial = if (cleanName.isNotBlank()) cleanName.first().uppercaseChar() else 'A'

    // Generate a consistent, predictable gradient background using the name's hashcode.
    // Using simple hash algorithm to pick two colors from a pre-defined material-like palette.
    val (color1, color2) = remember(cleanName) {
        val hash = cleanName.hashCode().absoluteValue
        val palette = listOf(
            Color(0xFFE57373) to Color(0xFFEF5350), // Red
            Color(0xFFF06292) to Color(0xFFEC407A), // Pink
            Color(0xFFBA68C8) to Color(0xFFAB47BC), // Purple
            Color(0xFF9575CD) to Color(0xFF7E57C2), // Deep Purple
            Color(0xFF7986CB) to Color(0xFF5C6BC0), // Indigo
            Color(0xFF64B5F6) to Color(0xFF42A5F5), // Blue
            Color(0xFF4FC3F7) to Color(0xFF29B6F6), // Light Blue
            Color(0xFF4DD0E1) to Color(0xFF26C6DA), // Cyan
            Color(0xFF4DB6AC) to Color(0xFF26A69A), // Teal
            Color(0xFF81C784) to Color(0xFF66BB6A), // Green
            Color(0xFFAED581) to Color(0xFF9CCC65), // Light Green
            Color(0xFFFFB74D) to Color(0xFFFFA726), // Orange
            Color(0xFFFF8A65) to Color(0xFFFF7043), // Deep Orange
            Color(0xFFA1887F) to Color(0xFF8D6E63)  // Brown
        )
        palette[hash % palette.size]
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(color1, color2)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // We use a responsive text size matching roughly half the container size automatically
        // because setting a fixed sp can be disproportionate to the varying sizes. 
        // Here, we provide enough scaling implicitly if the parent clamps it via layout size.
        // For AsyncImage placeholders, the Text uses fillMaxSize + wrapContent in Box.
        androidx.compose.foundation.layout.BoxWithConstraints {
            val fontSize = (maxHeight.value * 0.45f).coerceAtLeast(14f)
            Text(
                text = initial.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
