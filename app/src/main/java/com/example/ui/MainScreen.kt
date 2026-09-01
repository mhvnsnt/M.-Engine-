package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.capabilities.ecology.RemoteControlPlaneRepository

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
    object Observatory : Screen("observatory", "Observatory", Icons.Default.Visibility)
    object Workspace : Screen("workspace", "Projects", Icons.Default.Folder)
    object Tasks : Screen("tasks", "Tasks", Icons.AutoMirrored.Filled.List)
    object Evidence : Screen("evidence", "Evidence", Icons.Default.CheckCircle)
    object Connections : Screen("connections", "Connections", Icons.Default.Settings)
    object CapabilityAudit : Screen("capability_audit", "Audit", Icons.Default.Settings)
    object CapabilityCompetition : Screen("capability_competition", "Competition", Icons.Default.Settings)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Privacy : Screen("privacy", "Privacy", Icons.Default.Settings)
}

@Composable
fun MainScreen(viewModel: ChatViewModel, workspaceViewModel: com.example.ui.WorkspaceViewModel) {
    val isOnboardingComplete by viewModel.settingsRepository.isOnboardingCompleteFlow.collectAsStateWithLifecycle(initialValue = false)
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val controlPlaneRepository = remember { RemoteControlPlaneRepository() }
    
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
        Screen.Observatory,
        Screen.Chat,
        Screen.Workspace,
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
            startDestination = Screen.Observatory.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Observatory.route) {
                ObservatoryScreen(repository = controlPlaneRepository)
            }
            composable(Screen.Chat.route) {
                ChatScreen(viewModel = viewModel, workspaceViewModel = workspaceViewModel, onSettingsClick = { navController.navigate(Screen.Settings.route) })
            }
            composable(Screen.Workspace.route) {
                WorkspaceScreen(viewModel = workspaceViewModel)
            }
            composable(Screen.Tasks.route) {
                TasksScreen(viewModel = viewModel)
            }
            composable(Screen.Evidence.route) {
                EvidenceScreen(viewModel = viewModel)
            }
            composable(Screen.Connections.route) {
                ConnectionsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToAudit = { navController.navigate(Screen.CapabilityAudit.route) })
            }
            composable(Screen.CapabilityAudit.route) {
                CapabilityAuditScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToCompetition = { navController.navigate(Screen.CapabilityCompetition.route) })
            }
            composable(Screen.CapabilityCompetition.route) {
                CapabilityCompetitionScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() })
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
