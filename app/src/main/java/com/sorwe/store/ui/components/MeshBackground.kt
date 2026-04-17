package com.sorwe.store.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.sorwe.store.ui.theme.*

/**
 * A static mesh background featuring "blobs" of color.
 * Designed to sit behind glassmorphic surfaces to create depth and life without causing scroll jank.
 */
@Composable
fun MeshBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Clear background
        drawRect(DarkBackground)

        // Blob 1: Crimson Aura (Primary)
        val x1 = width * 0.5f 
        val y1 = height * 0.5f + height * 0.2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CrimsonRed.copy(alpha = 0.4f), Color.Transparent),
                center = Offset(x1, y1),
                radius = width * 0.8f
            ),
            center = Offset(x1, y1),
            radius = width * 0.8f
        )

        // Blob 2: Aqua Glow (Secondary)
        val x2 = width * 0.3f + width * 0.2f
        val y2 = height * 0.7f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SecondaryAccent.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(x2, y2),
                radius = width * 0.7f
            ),
            center = Offset(x2, y2),
            radius = width * 0.7f
        )

        // Blob 3: Violet Deep (Tertiary)
        val x3 = width * 0.7f - width * 0.2f
        val y3 = height * 0.2f + height * 0.2f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(SecondaryAccent.copy(alpha = 0.25f), Color.Transparent),
                center = Offset(x3, y3),
                radius = width * 0.6f
            ),
            center = Offset(x3, y3),
            radius = width * 0.6f
        )
    }
}
