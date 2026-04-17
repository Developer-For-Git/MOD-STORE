package com.sorwe.store.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import com.sorwe.store.ui.components.AppCardGrid
import com.sorwe.store.ui.components.LoadingAnimation
import com.sorwe.store.ui.components.CategoryChips
import com.sorwe.store.ui.components.ErrorState
import com.sorwe.store.ui.components.ErrorType
import com.sorwe.store.ui.components.FeaturedBanner
import com.sorwe.store.ui.components.SearchBar
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.accentGradient
import com.sorwe.store.ui.theme.glassColors
import com.sorwe.store.ui.theme.bounceOnClick
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAppClick: (String) -> Unit,
    onMyAppsClick: () -> Unit,
    onUpdatesClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    val updateDownloadInfo by viewModel.getUpdateDownloadState().collectAsStateWithLifecycle()

    // Handle Update Dialog
    (updateState as? com.sorwe.store.util.UpdateState.UpdateAvailable)?.let { update ->
        com.sorwe.store.ui.components.UpdateDialog(
            releaseNotes = update.releaseNotes,
            onUpdate = { viewModel.startUpdate(update.releaseNotes.downloadUrl) },
            downloadInfo = updateDownloadInfo
        )
    }

    // Notification Permission Request for Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { /* No-op */ }
        )
        
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    // Show FAB once the user scrolls past the very first item
    val showScrollToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

    val refreshState = rememberSwipeRefreshState(
        isRefreshing = uiState.isLoading && uiState.allApps.isNotEmpty()
    )

    Box(modifier = Modifier.fillMaxSize()) {
    SwipeRefresh(
        state = refreshState,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // --- Premium Header ---
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                val calendar = remember { java.util.Calendar.getInstance() }
                val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                val greeting = when (hour) {
                    in 0..11 -> "Good Morning"
                    in 12..16 -> "Good Afternoon"
                    else -> "Good Evening"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 16.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MOD Store",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )

                        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.IconButton(
                                onClick = { uriHandler.openUri("https://github.com/Developer-For-Git") },
                                modifier = Modifier.bounceOnClick { 
                                    uriHandler.openUri("https://github.com/Developer-For-Git") 
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = "Developer Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            androidx.compose.material3.IconButton(
                                onClick = onDownloadsClick,
                                modifier = Modifier.bounceOnClick(onClick = onDownloadsClick)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Downloads",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            androidx.compose.material3.IconButton(
                                onClick = onUpdatesClick,
                                modifier = Modifier.bounceOnClick(onClick = onUpdatesClick)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Updates",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            androidx.compose.material3.IconButton(
                                onClick = onMyAppsClick,
                                modifier = Modifier.bounceOnClick(onClick = onMyAppsClick)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "My Apps",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Search bar
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange
                )
            }

            // Spacer
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading state when list is empty
            if (uiState.isLoading && uiState.allApps.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingAnimation()
                    }
                }
            }
            // Error state when list is empty
            else if (uiState.error != null && uiState.allApps.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    ErrorState(
                        type = if (uiState.error?.contains("network", true) == true || uiState.error?.contains("internet", true) == true) ErrorType.NO_INTERNET else ErrorType.GENERIC_ERROR,
                        errorMessage = uiState.error,
                        onRetry = viewModel::refresh
                    )
                }
            }
            // Success state - render the actual feed content
            else {
                // --- Hero/Featured Section ---
                if (uiState.featuredApps.isNotEmpty() && uiState.searchQuery.isBlank() && uiState.selectedCategory == null) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Column {
                            FeaturedBanner(
                                featuredApps = uiState.featuredApps,
                                onAppClick = onAppClick
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }

                // Category chips
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    CategoryChips(
                        categories = uiState.categories,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = viewModel::onCategorySelected
                    )
                }

                // --- Main Feed Section Title ---
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = when {
                                    uiState.searchQuery.isNotBlank() -> "Search Results"
                                    uiState.selectedCategory != null -> uiState.selectedCategory!!
                                    else -> "All Applications"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.filteredApps.size} apps",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        SortDropdown(
                            currentSort = uiState.sortOption,
                            onSortSelected = viewModel::onSortOptionSelected
                        )
                    }
                }

                // App Cards
                if (uiState.filteredApps.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        ErrorState(type = ErrorType.SEARCH_EMPTY)
                    }
                } else {
                    items(
                        items = uiState.filteredApps,
                        key = { it.id }
                    ) { app ->
                    AppCardGrid(
                        app = app,
                        isFavorite = app.id in uiState.favoriteIds,
                        onClick = { onAppClick(app.id) },
                        onFavoriteClick = { viewModel.toggleFavorite(app.id) }
                    )
                    }
                }
            }
        }
    }

    // ── Scroll-to-Top FAB ──
    Box(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 20.dp, bottom = 100.dp)
    ) {
        AnimatedVisibility(
            visible = showScrollToTop,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = CrimsonRed.copy(alpha = 0.4f),
                        spotColor = CrimsonRed.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(brush = accentGradient())
                    .bounceOnClick {
                        coroutineScope.launch {
                            gridState.animateScrollToItem(0)
                        }
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardDoubleArrowUp,
                    contentDescription = "Scroll to top",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    } // end FAB wrapper Box
    } // end fillMaxSize Box
} // end HomeScreen


@Composable
private fun SortDropdown(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = glassColors()

    Box {
        // Sort button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = if (expanded) accentGradient()
                    else androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(colors.glassBg, colors.glassBg)
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (expanded) Color.Transparent else colors.glassBorder,
                    shape = RoundedCornerShape(14.dp)
                )
                .bounceOnClick { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = "Sort",
                tint = if (expanded) Color.White else CrimsonRed,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentSort.label.split(" ").first(), // Short label like "Recently", "Name", "Size"
                style = MaterialTheme.typography.labelLarge,
                color = if (expanded) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (expanded) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        // Dropdown menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
        ) {
            SortOption.entries.forEach { option ->
                val isSelected = option == currentSort
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) CrimsonRed else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSortSelected(option)
                        expanded = false
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CrimsonRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

