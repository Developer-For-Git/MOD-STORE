package com.sorwe.store.ui.screens.external

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sorwe.store.ui.components.AppCardGrid
import com.sorwe.store.ui.components.CategoryChips
import com.sorwe.store.ui.components.ErrorState
import com.sorwe.store.ui.components.ErrorType
import com.sorwe.store.ui.components.LoadingAnimation
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.accentGradient
import com.sorwe.store.ui.theme.bounceOnClick
import com.sorwe.store.ui.components.SearchBar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.foundation.lazy.grid.GridItemSpan
import kotlinx.coroutines.launch

/**
 * Screen that shows PC or TV apps using the same AppCardGrid as Home.
 * @param platform "pc" or "tv"
 */
@Composable
fun PlatformAppsScreen(
    platform: String,
    onAppClick: (String) -> Unit,
    viewModel: PlatformAppsViewModel = hiltViewModel()
) {
    // Tell the ViewModel which platform to filter for
    viewModel.init(platform)

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val title = if (platform == "pc") "PC Applications" else "TV Applications"
    val icon = if (platform == "pc") Icons.Default.Computer else Icons.Default.Tv

    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }

    // Content with pull-to-refresh
    val refreshState = rememberSwipeRefreshState(
        isRefreshing = uiState.isLoading && uiState.apps.isNotEmpty()
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 140.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header item
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 18.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = CrimsonRed,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box {
                            androidx.compose.material3.IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Sort,
                                    contentDescription = "Sort Apps",
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            androidx.compose.material3.DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                com.sorwe.store.ui.screens.home.SortOption.entries.forEach { option ->
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = option.label,
                                                fontWeight = if (uiState.sortOption == option) FontWeight.Bold else FontWeight.Normal,
                                                color = if (uiState.sortOption == option) CrimsonRed else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            viewModel.onSortOptionSelected(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Search bar item
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = viewModel::onSearchQueryChange
                    )
                }

                // Spacer
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Loading state
                if (uiState.isLoading && uiState.apps.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(400.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingAnimation(
                                circleSize = 16.dp,
                                spaceBetween = 8.dp,
                                travelDistance = 20.dp
                            )
                        }
                    }
                }
                // Error state
                else if (uiState.error != null && uiState.apps.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val errorType = if (uiState.error?.contains("Unable to resolve host", ignoreCase = true) == true ||
                            uiState.error?.contains("network", ignoreCase = true) == true
                        ) {
                            ErrorType.NO_INTERNET
                        } else {
                            ErrorType.GENERIC_ERROR
                        }
                        ErrorState(
                            type = errorType,
                            onRetry = viewModel::refresh
                        )
                    }
                }
                // Success content
                else {
                    // Category chips
                    if (uiState.categories.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            CategoryChips(
                                categories = uiState.categories,
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = viewModel::onCategorySelected
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    // Section label
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.filteredApps.size} apps",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (uiState.selectedCategory != null) uiState.selectedCategory!! else "All $title",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // App grid cards
                    if (uiState.filteredApps.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
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
} // end PlatformAppsScreen
