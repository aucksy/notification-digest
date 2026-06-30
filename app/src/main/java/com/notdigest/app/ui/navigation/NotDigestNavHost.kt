package com.notdigest.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.notdigest.app.ui.apps.AppsScreen
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
    Tab(NavRoutes.APPS, "Apps", Icons.Filled.Apps, Icons.Outlined.Apps),
    Tab(NavRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun NotDigestNavHost(
    startDestination: String,
    deepLinkRoute: String?,
    onDeepLinkConsumed: () -> Unit,
    onInboxOpenedFromLink: () -> Unit = {},
) {
    val navController = rememberNavController()

    LaunchedEffect(deepLinkRoute) {
        val route = deepLinkRoute ?: return@LaunchedEffect
        // While onboarding is still on screen, ignore deep links — including the relaunch→Home jump.
        // Otherwise re-opening the app mid-onboarding would shove Home on top of the unfinished
        // onboarding flow, leaving both alive at once.
        if (navController.currentDestination?.route == NavRoutes.ONBOARDING) {
            onDeepLinkConsumed()
            return@LaunchedEffect
        }
        if (route in NavRoutes.BOTTOM_BAR) {
            navController.navigateTab(route)
            // Only now that we've genuinely landed on the Inbox: ask it to jump to the newest items.
            if (route == NavRoutes.INBOX) onInboxOpenedFromLink()
        } else {
            navController.navigate(route) { launchSingleTop = true }
        }
        onDeepLinkConsumed()
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in NavRoutes.BOTTOM_BAR

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomBar(navController = navController, backStackEntry = backStackEntry)
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
                    onOpenApps = { navController.navigateTab(NavRoutes.APPS) },
                )
            }
            composable(NavRoutes.INBOX) {
                InboxScreen(contentPadding = innerPadding)
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
        }
    }
}

@Composable
private fun BottomBar(
    navController: NavHostController,
    backStackEntry: androidx.navigation.NavBackStackEntry?,
) {
    val viewModel: BottomBarViewModel = hiltViewModel()
    val archivedCount by viewModel.pendingCount.collectAsStateWithLifecycle()

    NavigationBar {
        tabs.forEach { tab ->
            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateTab(tab.route) },
                icon = {
                    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
                    if (tab.route == NavRoutes.INBOX && archivedCount > 0) {
                        BadgedBox(badge = { Badge { Text(if (archivedCount > 99) "99+" else "$archivedCount") } }) {
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
