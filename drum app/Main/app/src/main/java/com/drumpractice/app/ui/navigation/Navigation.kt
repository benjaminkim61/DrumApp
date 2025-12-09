package com.drumpractice.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.drumpractice.app.ui.screens.home.HomeScreen
import com.drumpractice.app.ui.screens.library.LibraryScreen
import com.drumpractice.app.ui.screens.player.PlayerScreen
import com.drumpractice.app.ui.screens.recording.RecordingScreen
import com.drumpractice.app.ui.screens.recording.PostRecordingScreen
import com.drumpractice.app.ui.screens.settings.SettingsScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )
    
    data object Library : Screen(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )
    
    data object Record : Screen(
        route = "record",
        title = "Record",
        selectedIcon = Icons.Filled.FiberManualRecord,
        unselectedIcon = Icons.Outlined.FiberManualRecord
    )
    
    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
    
    data object Player : Screen(
        route = "player/{songId}",
        title = "Player",
        selectedIcon = Icons.Filled.PlayArrow,
        unselectedIcon = Icons.Outlined.PlayArrow
    ) {
        fun createRoute(songId: Long) = "player/$songId"
    }
    
    data object PostRecording : Screen(
        route = "post_recording/{recordingId}",
        title = "Edit Recording",
        selectedIcon = Icons.Filled.Edit,
        unselectedIcon = Icons.Outlined.Edit
    ) {
        fun createRoute(recordingId: Long) = "post_recording/$recordingId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Library,
    Screen.Record,
    Screen.Settings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrumPracticeNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Determine if bottom bar should be shown
    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { 
                            it.route == screen.route 
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { 300 },
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { -300 },
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(300)) + slideInHorizontally(
                    initialOffsetX = { -300 },
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(300)) + slideOutHorizontally(
                    targetOffsetX = { 300 },
                    animationSpec = tween(300)
                )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToPlayer = { songId ->
                        navController.navigate(Screen.Player.createRoute(songId))
                    },
                    onNavigateToRecording = {
                        navController.navigate(Screen.Record.route)
                    }
                )
            }

            composable(Screen.Library.route) {
                LibraryScreen(
                    onNavigateToPlayer = { songId ->
                        navController.navigate(Screen.Player.createRoute(songId))
                    },
                    onNavigateToPostRecording = { recordingId ->
                        navController.navigate(Screen.PostRecording.createRoute(recordingId))
                    }
                )
            }

            composable(Screen.Record.route) {
                RecordingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onRecordingComplete = { recordingId ->
                        navController.navigate(Screen.PostRecording.createRoute(recordingId)) {
                            popUpTo(Screen.Record.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(
                route = Screen.Player.route,
                arguments = listOf(navArgument("songId") { type = NavType.LongType })
            ) { backStackEntry ->
                val songId = backStackEntry.arguments?.getLong("songId") ?: 0L
                PlayerScreen(
                    songId = songId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToRecording = {
                        navController.navigate(Screen.Record.route)
                    }
                )
            }

            composable(
                route = Screen.PostRecording.route,
                arguments = listOf(navArgument("recordingId") { type = NavType.LongType })
            ) { backStackEntry ->
                val recordingId = backStackEntry.arguments?.getLong("recordingId") ?: 0L
                PostRecordingScreen(
                    recordingId = recordingId,
                    onNavigateBack = { navController.popBackStack() },
                    onSaveComplete = {
                        navController.navigate(Screen.Library.route) {
                            popUpTo(Screen.Home.route)
                        }
                    }
                )
            }
        }
    }
}
