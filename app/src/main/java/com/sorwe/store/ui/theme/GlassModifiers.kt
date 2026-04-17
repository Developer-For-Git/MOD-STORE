package com.sorwe.store.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable glass-effect Modifier extensions for liquid glass UI.
 * Optimized to remember shapes and brushes to avoid per-frame allocations.
 */

@Composable
fun glassColors(): GlassColorSet {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    return remember(isDark) {
        if (isDark) {
            GlassColorSet(
                glassBg = DarkGlassBackground,
                glassBorder = DarkGlassBorder,
                glassHighlight = DarkGlassHighlight,
                glassShimmer = DarkGlassShimmer
            )
        } else {
            GlassColorSet(
                glassBg = LightGlassBackground,
                glassBorder = LightGlassBorder,
                glassHighlight = LightGlassHighlight,
                glassShimmer = LightGlassShimmer
            )
        }
    }
}

data class GlassColorSet(
    val glassBg: Color,
    val glassBorder: Color,
    val glassHighlight: Color,
    val glassShimmer: Color
)

/**
 * Glass card effect: translucent background, luminous border.
 * Removed heavy shadow() in favor of lightweight drawBehind.
 */
@Composable
fun Modifier.glassCard(
    cornerRadius: Dp = 20.dp,
    shadowElevation: Dp = 8.dp
): Modifier {
    val colors = glassColors()
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    val bgBrush = remember(colors.glassBg, colors.glassHighlight, isDark) {
        if (isDark) {
            Brush.linearGradient(
                colors = listOf(colors.glassBg, colors.glassHighlight),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            )
        } else {
            // Stronger frost effect for light mode
            Brush.verticalGradient(
                colors = listOf(colors.glassBg.copy(alpha = 0.95f), colors.glassBg.copy(alpha = 0.85f))
            )
        }
    }

    val borderWidth = if (isDark) 1.dp else 1.2.dp
    val borderBrush = remember(colors.glassBorder, isDark) {
        Brush.linearGradient(
            colors = listOf(
                colors.glassBorder,
                colors.glassBorder.copy(alpha = if (isDark) 0.05f else 0.2f)
            ),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        )
    }

    return this
        .clip(shape)
        .background(brush = bgBrush)
        .border(width = borderWidth, brush = borderBrush, shape = shape)
}

/**
 * Frosted glass nav bar effect.
 */
@Composable
fun Modifier.glassNavBar(): Modifier {
    val colors = glassColors()
    val bgBrush = remember(colors.glassBg) {
        Brush.verticalGradient(
            colors = listOf(
                colors.glassBg.copy(alpha = 0.85f),
                colors.glassBg.copy(alpha = 0.95f)
            )
        )
    }
    val shape = remember { RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp) }
    return this
        .background(brush = bgBrush)
        .border(width = 1.dp, color = colors.glassBorder.copy(alpha = 0.5f), shape = shape)
}

/**
 * Glass pill style for search bars and chips.
 */
@Composable
fun Modifier.glassPill(
    cornerRadius: Dp = 24.dp
): Modifier {
    val colors = glassColors()
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    
    val bgBrush = if (isDark) Brush.linearGradient(listOf(colors.glassBg, colors.glassHighlight))
                  else Brush.verticalGradient(listOf(colors.glassBg, colors.glassBg.copy(alpha = 0.9f)))

    val borderBrush = remember(colors.glassBorder, isDark) {
        Brush.linearGradient(
            colors = listOf(
                colors.glassBorder,
                colors.glassBorder.copy(alpha = if (isDark) 0.03f else 0.25f)
            )
        )
    }
    return this
        .clip(shape)
        .background(brush = bgBrush)
        .border(
            width = if (isDark) 1.dp else 1.2.dp,
            brush = borderBrush,
            shape = shape
        )
}

/**
 * Premium bounce effect on click.
 * Combines scaling and haptic feedback.
 */
@Composable
fun Modifier.bounceOnClick(
    enabled: Boolean = true,
    scale: Float = 0.96f,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val animatedScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) scale else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    return this
        .scale(animatedScale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onClick()
            }
        )
}

/**
 * Accent gradient brush for buttons and highlights (Premium Crimson).
 */
fun accentGradient(): Brush = Brush.linearGradient(
    colors = listOf(CrimsonRedDark, CrimsonRed, CrimsonRedLight),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)

/**
 * Soft accent gradient for selected states.
 */
fun softAccentGradient(): Brush = Brush.linearGradient(
    colors = listOf(
        CrimsonRed.copy(alpha = 0.3f),
        CrimsonRedLight.copy(alpha = 0.2f)
    ),
    start = Offset(0f, 0f),
    end = Offset(Float.POSITIVE_INFINITY, 0f)
)
