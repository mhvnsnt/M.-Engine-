package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
    object Workspace : Screen("workspace", "Projects", Icons.Default.Folder)
    object Tasks : Screen("tasks", "Tasks", Icons.Default.List)
    object Evidence : Screen("evidence", "Evidence", Icons.Default.CheckCircle)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Privacy : Screen("privacy", "Privacy", Icons.Default.Settings)
}

@Composable
fun MainScreen(viewModel: ChatViewModel, workspaceViewModel: com.example.ui.WorkspaceViewModel) {
    val isOnboardingComplete by viewModel.settingsRepository.isOnboardingCompleteFlow.collectAsStateWithLifecycle(initialValue = false)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    
    if (!isOnboardingComplete) {
        OnboardingScreen(onComplete = {
            coroutineScope.launch {
                viewModel.settingsRepository.completeOnboarding()
            }
        })
        return
    }

    val selectedFile by workspaceViewModel.selectedFile.collectAsStateWithLifecycle()
    LaunchedEffect(selectedFile) {
        viewModel.workspaceContext.value = selectedFile?.content
    }
    val navController = rememberNavController()
    val items = listOf(
        Screen.Chat,
        Screen.Workspace,
        Screen.Tasks,
        Screen.Evidence,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Chat.route) {
                ChatScreen(viewModel = viewModel, workspaceViewModel = workspaceViewModel, onSettingsClick = { navController.navigate(Screen.Settings.route) })
            }
            composable(Screen.Workspace.route) {
                WorkspaceScreen(viewModel = workspaceViewModel)
            }
            composable(Screen.Tasks.route) {
                TasksScreen()
            }
            composable(Screen.Evidence.route) {
                EvidenceScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) })
            }
            composable(Screen.Privacy.route) {
                PrivacyScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
