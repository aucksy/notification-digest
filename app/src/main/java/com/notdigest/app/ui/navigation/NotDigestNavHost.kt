package com.notdigest.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notdigest.app.ui.apps.AppsScreen
import com.notdigest.app.ui.digest.DigestDetailScreen
import com.notdigest.app.ui.history.HistoryScreen
import com.notdigest.app.ui.home.HomeScreen
import com.notdigest.app.ui.inbox.InboxScreen
import com.notdigest.app.ui.onboarding.OnboardingScreen
import com.notdigest.app.ui.schedule.ScheduleScreen
import com.notdigest.app.ui.settings.SettingsScreen

private data class Tab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val tabs = listOf(
    Tab(NavRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    Tab(NavRoutes.INBOX, "Inbox", Icons.Filled.Inbox, Icons.Outlined.Inbox),
    Tab(NavRoutes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History),
    Tab(NavRoutes.APPS, "Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    Tab(NavRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun NotDigestNavHost(
    startDestination: String,
    deepLinkRoute: String?,
    onDeepLinkConsumed: () -> Unit,
) {
    val navController = rememberNavController()

    LaunchedEffect(deepLinkRoute) {
        val route = deepLinkRoute ?: return@LaunchedEffect
        navController.navigate(route) { launchSingleTop = true }
        onDeepLinkConsumed()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in NavRoutes.BOTTOM_BAR

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomBar(navController = navController, backStackEntry = backStackEntry)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier,
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 16 } },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { it / 16 } },
        ) {
            composable(NavRoutes.ONBOARDING) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(NavRoutes.HOME) {
                HomeScreen(
                    contentPadding = innerPadding,
                    onOpenInbox = { navController.navigateTab(NavRoutes.INBOX) },
                    onOpenHistory = { navController.navigateTab(NavRoutes.HISTORY) },
                    onOpenApps = { navController.navigateTab(NavRoutes.APPS) },
                    onOpenSchedule = { navController.navigate(NavRoutes.SCHEDULE) },
                    onOpenDigest = { id -> navController.navigate(NavRoutes.digestDetail(id)) },
                )
            }
            composable(NavRoutes.INBOX) {
                InboxScreen(contentPadding = innerPadding)
            }
            composable(NavRoutes.HISTORY) {
                HistoryScreen(
                    contentPadding = innerPadding,
                    onOpenDigest = { id -> navController.navigate(NavRoutes.digestDetail(id)) },
                )
            }
            composable(NavRoutes.APPS) {
                AppsScreen(contentPadding = innerPadding)
            }
            composable(NavRoutes.SETTINGS) {
                SettingsScreen(
                    contentPadding = innerPadding,
                    onOpenSchedule = { navController.navigate(NavRoutes.SCHEDULE) },
                )
            }
            composable(NavRoutes.SCHEDULE) {
                ScheduleScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = NavRoutes.DIGEST_DETAIL,
                arguments = listOf(navArgument(NavRoutes.ARG_DIGEST_ID) { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong(NavRoutes.ARG_DIGEST_ID) ?: 0L
                DigestDetailScreen(digestId = id, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry?,
) {
    val viewModel: BottomBarViewModel = hiltViewModel()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()

    NavigationBar {
        tabs.forEach { tab ->
            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateTab(tab.route) },
                icon = {
                    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
                    if (tab.route == NavRoutes.INBOX && pendingCount > 0) {
                        BadgedBox(badge = { Badge { Text(if (pendingCount > 99) "99+" else "$pendingCount") } }) {
                            Icon(icon, contentDescription = tab.label)
                        }
                    } else {
                        Icon(icon, contentDescription = tab.label)
                    }
                },
                label = { Text(tab.label) },
                alwaysShowLabel = false,
            )
        }
    }
}

/** Navigate to a top-level tab with standard single-top + state restoration semantics. */
private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
