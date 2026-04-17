package com.sorwe.store.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil.request.ImageRequest
import coil.transform.RoundedCornersTransformation
import com.sorwe.store.data.model.AppItem
import com.sorwe.store.ui.theme.Coral40
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.glassCard
import com.sorwe.store.ui.theme.bounceOnClick
import com.sorwe.store.R

// ─── Grid Card ─────────────────────────────────────────────────────────
@Composable
fun AppCardGrid(
    app: AppItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) Coral40 else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "favoriteColor"
    )
    val iconShape = remember { RoundedCornerShape(18.dp) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.6f else 0f,
        animationSpec = tween(300),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .bounceOnClick { onClick() }
            .then(
                if (isPressed) Modifier.background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(CrimsonRed.copy(alpha = glowAlpha), Color.Transparent),
                        radius = 400f
                    )
                ) else Modifier
            )
    ) {
        // Favorite heart — top right
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
                .padding(4.dp)
        ) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = favoriteColor,
                modifier = Modifier.size(16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon
            val density = LocalDensity.current
            val context = LocalContext.current
            val cornerRadiusPx = remember(density) { with(density) { 18.dp.toPx() } }

            coil.compose.SubcomposeAsyncImage(
                model = remember(app.icon) {
                    ImageRequest.Builder(context)
                        .data(app.icon.takeIf { it.isNotBlank() })
                        .size(256)
                        .crossfade(true)
                        .transformations(RoundedCornersTransformation(cornerRadiusPx))
                        .build()
                },
                contentDescription = app.name,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    FallbackIcon(appName = app.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)))
                },
                error = {
                    FallbackIcon(appName = app.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Name
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Size
            Text(
                text = app.size,
                style = MaterialTheme.typography.labelSmall,
                color = CrimsonRed,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}


// ─── List Card ─────────────────────────────────────────────────────────
@Composable
fun AppCardList(
    app: AppItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteColor by animateColorAsState(
        targetValue = if (isFavorite) Coral40 else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "favoriteColor"
    )
    val iconShape = remember { RoundedCornerShape(16.dp) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val glowAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 0f,
        animationSpec = tween(300),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 20.dp, shadowElevation = 8.dp)
            .bounceOnClick { onClick() }
            .then(
                if (isPressed) Modifier.background(
                    brush = androidx.compose.ui.graphics.Brush.radialGradient(
                        colors = listOf(CrimsonRed.copy(alpha = glowAlpha), Color.Transparent),
                        radius = 600f
                    )
                ) else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            val density = LocalDensity.current
            val context = LocalContext.current
            val cornerRadiusPx = remember(density) { with(density) { 16.dp.toPx() } }

            coil.compose.SubcomposeAsyncImage(
                model = remember(app.icon) {
                    ImageRequest.Builder(context)
                        .data(app.icon.takeIf { it.isNotBlank() })
                        .size(192)
                        .crossfade(true)
                        .transformations(RoundedCornersTransformation(cornerRadiusPx))
                        .build()
                },
                contentDescription = app.name,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    FallbackIcon(appName = app.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                },
                error = {
                    FallbackIcon(appName = app.name, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)))
                }
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Name + Size
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.size,
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonRed,
                    fontSize = 11.sp
                )
            }

            // Favorite
            IconButton(
                onClick = onFavoriteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = favoriteColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AppCardSkeleton(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .glassCard(cornerRadius = 22.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            // Arrangement.Center is imported now
            verticalArrangement = Arrangement.Center
        ) {
            // Icon Placeholder
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Name Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(18.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Size Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .shimmerEffect()
            )
        }
    }
}
