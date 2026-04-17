package com.sorwe.store.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import kotlinx.coroutines.delay
import com.sorwe.store.ui.screens.detail.DetailScreen
import com.sorwe.store.ui.screens.external.PlatformAppsScreen
import com.sorwe.store.ui.screens.favorites.FavoritesScreen
import com.sorwe.store.ui.screens.home.HomeScreen
import com.sorwe.store.ui.screens.myapps.MyAppsScreen
import com.sorwe.store.ui.screens.settings.SettingsScreen
import com.sorwe.store.ui.theme.CrimsonRed
import com.sorwe.store.ui.theme.DarkOnSurfaceVariant
import com.sorwe.store.ui.theme.DarkSurface
import com.sorwe.store.ui.theme.glassColors

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Favorites : Screen("favorites")
    data object Settings : Screen("settings")
    data object PcApps : Screen("pc_apps")
    data object TvApps : Screen("tv_apps")
    data object MyApps : Screen("my_apps")
    data object Updates : Screen("updates")
    data object Repositories : Screen("repositories")
    data object Downloads : Screen("downloads")
    data object RequestApp : Screen("request_app")
    data object LocalExplorer : Screen("local_explorer")
    data object Insights : Screen("insights")
    data object Login : Screen("login")
    data object BugReport : Screen("bug_report")
    data object Detail : Screen("detail/{appId}") {
        fun createRoute(appId: String) = "detail/$appId"
    }
}

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Apps", Icons.Filled.Android, Icons.Default.Android),
    BottomNavItem(Screen.PcApps, "Windows", Icons.Filled.Computer, Icons.Default.Computer),
    BottomNavItem(Screen.TvApps, "TV", Icons.Filled.Tv, Icons.Default.Tv),
    BottomNavItem(Screen.Insights, "Insights", Icons.Filled.BarChart, Icons.Default.BarChart),
    BottomNavItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
    BottomNavItem(Screen.Favorites, "Saved", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
)

@Composable
fun SorweNavGraph(intent: android.content.Intent? = null) {
    val navController = rememberNavController()
    
    // Handle deep links when a new intent is received (e.g. from onNewIntent)
    androidx.compose.runtime.LaunchedEffect(intent) {
        intent?.let {
            navController.handleDeepLink(it)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Favorites.route,
        Screen.Settings.route,
        Screen.PcApps.route,
        Screen.TvApps.route,
        Screen.Insights.route
    )

    var isScrolling by remember { mutableStateOf(false) }
    val lastScrollTime = remember { mutableLongStateOf(0L) }
    
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Any movement triggers "scrolling" state
                if (available.y != 0f) {
                    lastScrollTime.longValue = System.currentTimeMillis()
                }
                return Offset.Zero
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(lastScrollTime.longValue) {
        if (lastScrollTime.longValue > 0) {
            isScrolling = true
            delay(500) // Wait for user to stop interaction
            isScrolling = false
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // Root Animated Aura
            com.sorwe.store.ui.components.MeshBackground()
            
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.4f)),
                enterTransition = {
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeIn(tween(400))
                },
                exitTransition = {
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { -it / 3 },
                        animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeOut(tween(400))
                },
                popEnterTransition = {
                    androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeIn(tween(400))
                },
                popExitTransition = {
                    androidx.compose.animation.slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                    ) + fadeOut(tween(400))
                }
            ) {
                composable(
                    route = Screen.Home.route,
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = { androidx.compose.animation.ExitTransition.None }
                ) {
                    HomeScreen(
                        onAppClick = { appId ->
                            navController.navigate(Screen.Detail.createRoute(appId))
                        },
                        onMyAppsClick = {
                            navController.navigate(Screen.MyApps.route)
                        },
                        onUpdatesClick = {
                            navController.navigate(Screen.Updates.route)
                        },
                        onDownloadsClick = {
                            navController.navigate(Screen.Downloads.route)
                        }
                    )
                }

                composable(
                    route = Screen.PcApps.route,
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = { androidx.compose.animation.ExitTransition.None }
                ) {
                    PlatformAppsScreen(
                        platform = "pc",
                        onAppClick = { appId ->
                            navController.navigate(Screen.Detail.createRoute(appId))
                        }
                    )
                }

                composable(
                    route = Screen.TvApps.route,
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = { androidx.compose.animation.ExitTransition.None }
                ) {
                    PlatformAppsScreen(
                        platform = "tv",
                        onAppClick = { appId ->
                            navController.navigate(Screen.Detail.createRoute(appId))
                        }
                    )
                }

                composable(
                    route = Screen.Favorites.route,
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = { androidx.compose.animation.ExitTransition.None }
                ) {
                    FavoritesScreen(
                        onAppClick = { appId ->
                            navController.navigate(Screen.Detail.createRoute(appId))
                        }
                    )
                }

                composable(
                    route = Screen.Settings.route,
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = { androidx.compose.animation.ExitTransition.None }
                ) {
                    SettingsScreen(
                        onNavigateToRepositories = { navController.navigate(Screen.Repositories.route) },
                        onRequestAppClick = { navController.navigate(Screen.RequestApp.route) },
                        onLocalExplorerClick = { navController.navigate(Screen.LocalExplorer.route) },
                        onLoginClick = { navController.navigate(Screen.Login.route) },
                        onReportBugClick = { navController.navigate(Screen.BugReport.route) }
                    )
                }

                composable(Screen.MyApps.route) {
                    MyAppsScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onAppClick = { appId ->
                            navController.navigate(Screen.Detail.createRoute(appId))
                        }
                    )
                }

                composable(Screen.Updates.route) {
                    com.sorwe.store.ui.screens.updates.UpdatesScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onAppClick = { appId ->
                            navController.navigate(Screen.Detail.createRoute(appId))
                        }
                    )
                }

                composable(Screen.Repositories.route) {
                    com.sorwe.store.ui.screens.repositories.RepositoriesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Downloads.route) {
                    com.sorwe.store.ui.screens.downloads.DownloadsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.LocalExplorer.route) {
                    com.sorwe.store.ui.screens.explorer.LocalExplorerScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.Insights.route) {
                    com.sorwe.store.ui.screens.insights.InsightsScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.RequestApp.route) {
                    com.sorwe.store.ui.screens.request.RequestAppScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.BugReport.route) {
                    com.sorwe.store.ui.screens.bugreport.BugReportScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Login.route,
                    deepLinks = listOf(navDeepLink { uriPattern = "sorwestore://oauth?code={code}" })
                ) { backStackEntry ->
                    val code = backStackEntry.arguments?.getString("code")
                    com.sorwe.store.ui.screens.auth.LoginScreen(
                        onNavigateBack = { navController.popBackStack() },
                        code = code
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(
                        navArgument("appId") { type = NavType.StringType }
                    ),
                    enterTransition = { androidx.compose.animation.EnterTransition.None },
                    exitTransition = { androidx.compose.animation.ExitTransition.None },
                    popEnterTransition = { androidx.compose.animation.EnterTransition.None },
                    popExitTransition = { androidx.compose.animation.ExitTransition.None }
                ) {
                    DetailScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
            
            // --- Floating Premium Dock ---
            if (showBottomBar) {
                val navBarShape = RoundedCornerShape(32.dp)
                val selectedIndex = bottomNavItems.indexOfFirst { item ->
                    currentDestination?.hierarchy?.any { it.route == item.screen.route } == true
                }.coerceAtLeast(0)

                val dockAlpha by animateFloatAsState(
                    targetValue = if (isScrolling) 0f else 1f,
                    animationSpec = tween(400),
                    label = "dockAlpha"
                )
                val dockScale by animateFloatAsState(
                    targetValue = if (isScrolling) 0.8f else 1f,
                    animationSpec = tween(400),
                    label = "dockScale"
                )
                val dockTranslation by animateFloatAsState(
                    targetValue = if (isScrolling) 40f else 0f,
                    animationSpec = tween(400),
                    label = "dockTranslation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp)
                        .navigationBarsPadding()
                        .graphicsLayer {
                            alpha = dockAlpha
                            scaleX = dockScale
                            scaleY = dockScale
                            translationY = dockTranslation
                        },
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .height(68.dp)
                            .clip(navBarShape)
                            .background(DarkSurface.copy(alpha = 0.95f))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                    ) {
                        bottomNavItems.forEachIndexed { index, item ->
                            val selected = index == selectedIndex
                            
                            Box(
                                modifier = Modifier
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(
                                        if (selected) CrimsonRed.copy(alpha = 0.12f)
                                        else Color.Transparent
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (!selected) {
                                            navController.navigate(item.screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .padding(horizontal = if (selected) 20.dp else 12.dp),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.foundation.layout.Row(
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label,
                                        tint = if (selected) CrimsonRed else DarkOnSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    
                                    if (selected) {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = CrimsonRed,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
