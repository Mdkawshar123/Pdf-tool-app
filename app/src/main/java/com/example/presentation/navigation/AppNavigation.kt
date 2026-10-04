package com.example.presentation.navigation

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.R
import com.example.presentation.editor.PdfEditorScreen
import com.example.presentation.files.FilesScreen
import com.example.presentation.history.HistoryScreen
import com.example.presentation.home.HomeScreen
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.settings.SettingsScreen
import com.example.presentation.splash.SplashScreen
import com.example.presentation.tools.ToolRunnerScreen
import com.example.presentation.viewer.PdfViewerScreen

sealed class BottomNavTab(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavTab("tab_home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Files : BottomNavTab("tab_files", R.string.nav_files, Icons.Filled.Folder, Icons.Outlined.Folder)
    object History : BottomNavTab("tab_history", R.string.nav_history, Icons.Filled.History, Icons.Outlined.History)
    object Settings : BottomNavTab("tab_settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavTabs = listOf(
    BottomNavTab.Home,
    BottomNavTab.Files,
    BottomNavTab.History,
    BottomNavTab.Settings
)

@Composable
fun AppNavigation(
    currentThemeMode: String,
    onThemeModeChange: (String) -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isBottomBarVisible = currentRoute in bottomNavTabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    modifier = Modifier
                        .testTag("bottom_navigation_bar")
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    bottomNavTabs.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = stringResource(tab.titleRes)
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(tab.titleRes),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "splash_screen",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Splash Screen (Initial branded reveal)
            composable("splash_screen") {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate(BottomNavTab.Home.route) {
                            popUpTo("splash_screen") { inclusive = true }
                        }
                    }
                )
            }

            // Home Tab
            composable(BottomNavTab.Home.route) {
                HomeScreen(
                    onToolClick = { toolId ->
                        if (toolId == "edit_pdf") {
                            navController.navigate("editor_screen")
                        } else if (toolId == "viewer") {
                            navController.navigate("tool_runner/viewer")
                        } else {
                            navController.navigate("tool_runner/$toolId")
                        }
                    },
                    onOpenViewer = { path ->
                        val encoded = Uri.encode(path)
                        navController.navigate("viewer_screen/$encoded")
                    },
                    onNavigateSettings = {
                        navController.navigate(BottomNavTab.Settings.route)
                    }
                )
            }

            // Files Tab
            composable(BottomNavTab.Files.route) {
                FilesScreen(
                    onOpenViewer = { path ->
                        val encoded = Uri.encode(path)
                        navController.navigate("viewer_screen/$encoded")
                    }
                )
            }

            // History Tab
            composable(BottomNavTab.History.route) {
                HistoryScreen(
                    onOpenViewer = { path ->
                        val encoded = Uri.encode(path)
                        navController.navigate("viewer_screen/$encoded")
                    }
                )
            }

            // Settings Tab
            composable(BottomNavTab.Settings.route) {
                SettingsScreen(
                    currentThemeMode = currentThemeMode,
                    onThemeModeChange = onThemeModeChange,
                    onShowOnboarding = {
                        navController.navigate("onboarding_screen")
                    }
                )
            }

            // Tool Execution Screen
            composable(
                route = "tool_runner/{toolId}",
                arguments = listOf(navArgument("toolId") { type = NavType.StringType })
            ) { backStackEntry ->
                val toolId = backStackEntry.arguments?.getString("toolId") ?: "merge"
                ToolRunnerScreen(
                    toolId = toolId,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenViewer = { path ->
                        val encoded = Uri.encode(path)
                        navController.navigate("viewer_screen/$encoded")
                    }
                )
            }

            // PDF Viewer Screen
            composable(
                route = "viewer_screen/{filePath}",
                arguments = listOf(navArgument("filePath") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val path = Uri.decode(encodedPath)
                PdfViewerScreen(
                    filePath = path,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // PDF Editor Screen
            composable(route = "editor_screen") {
                PdfEditorScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { path ->
                        val encoded = Uri.encode(path)
                        navController.navigate("viewer_screen/$encoded") {
                            popUpTo("editor_screen") { inclusive = true }
                        }
                    }
                )
            }

            // Onboarding Screen
            composable(route = "onboarding_screen") {
                OnboardingScreen(
                    onFinish = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
