package com.sorwe.store.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.glassColors
import com.sorwe.store.ui.theme.bounceOnClick

@Composable
fun CategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            GlassChip(
                label = "All",
                isSelected = selectedCategory == null,
                onClick = { onCategorySelected(null) }
            )
        }

        items(categories) { category ->
            val isSelected = selectedCategory == category
            GlassChip(
                label = category,
                isSelected = isSelected,
                onClick = { onCategorySelected(if (isSelected) null else category) }
            )
        }
    }
}

@Composable
private fun GlassChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = glassColors()
    val shape = RoundedCornerShape(20.dp)

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) CrimsonRed.copy(alpha = 0.2f) else colors.glassBg,
        animationSpec = tween(250),
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) CrimsonRed.copy(alpha = 0.4f) else colors.glassBorder,
        animationSpec = tween(250),
        label = "chipBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) CrimsonRed else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(250),
        label = "chipText"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .bounceOnClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}
