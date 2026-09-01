package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.ai.capabilities.ecology.RemoteControlPlaneRepository
import com.example.ai.capabilities.federated.provider.CapabilityFabric as CapabilityFabricRuntime
import kotlinx.coroutines.launch

sealed class ShellRoute(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : ShellRoute("home", "Home", Icons.Default.Home)
    object Conversations : ShellRoute("chat", "Conversations", Icons.AutoMirrored.Filled.Chat)
    object Projects : ShellRoute("projects", "Projects", Icons.Default.Folder)
    object Apps : ShellRoute("apps", "Apps", Icons.Default.PhoneAndroid)
    object Games : ShellRoute("games", "Games", Icons.Default.VideogameAsset)
    object Workspaces : ShellRoute("workspaces", "Workspaces", Icons.Default.Workspaces)
    object Agents : ShellRoute("agents", "Agents", Icons.Default.SmartToy)
    object ExecutionFabric : ShellRoute("observatory", "Execution Fabric", Icons.Default.Visibility)
    object CapabilityFabric : ShellRoute("capability_fabric", "Capability Fabric", Icons.Default.Hub)
    object Memory : ShellRoute("evidence", "Memory / Library", Icons.Default.CheckCircle)
    object Settings : ShellRoute("settings", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppShell(viewModel: ChatViewModel, workspaceViewModel: WorkspaceViewModel) {
    val isOnboardingComplete by viewModel.settingsRepository.isOnboardingCompleteFlow.collectAsStateWithLifecycle(initialValue = false)
    val coroutineScope = rememberCoroutineScope()
    val controlPlaneRepository = remember { RemoteControlPlaneRepository() }

    // Constructs the federated provider layer. Until this existed, the whole
    // federated/provider package was unreachable from any entry point.
    val capabilityFabric = remember { CapabilityFabricRuntime() }

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val drawerItems = listOf(
        ShellRoute.Home,
        ShellRoute.Conversations,
        ShellRoute.Projects,
        ShellRoute.Apps,
        ShellRoute.Games,
        ShellRoute.Workspaces,
        ShellRoute.Agents,
        ShellRoute.ExecutionFabric,
        ShellRoute.CapabilityFabric,
        ShellRoute.Memory,
        ShellRoute.Settings
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "M. Engine",
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                Divider(modifier = Modifier.padding(horizontal = 28.dp))
                Spacer(modifier = Modifier.height(8.dp))
                
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            coroutineScope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                // Optionally add a top bar to open the drawer
                TopAppBar(
                    title = { Text("M. Engine") },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ShellRoute.Conversations.route, // Default start is Conversations
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(ShellRoute.Home.route) {
                    // Placeholder for Home
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Home Dashboard (WIP)")
                    }
                }
                composable(ShellRoute.Conversations.route) {
                    ChatScreen(viewModel = viewModel, workspaceViewModel = workspaceViewModel, onSettingsClick = { navController.navigate(ShellRoute.Settings.route) })
                }
                composable(ShellRoute.Projects.route) {
                    WorkspaceScreen(viewModel = workspaceViewModel)
                }
                composable(ShellRoute.Apps.route) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Apps OS (WIP)") }
                }
                composable(ShellRoute.Games.route) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Games OS (WIP)") }
                }
                composable(ShellRoute.Workspaces.route) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Workspaces OS (WIP)") }
                }
                composable(ShellRoute.Agents.route) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { Text("Agents OS (WIP)") }
                }
                composable(ShellRoute.ExecutionFabric.route) {
                    ObservatoryScreen(repository = controlPlaneRepository)
                }
                composable(ShellRoute.CapabilityFabric.route) {
                    CapabilityFabricScreen(fabric = capabilityFabric)
                }
                composable(ShellRoute.Memory.route) {
                    EvidenceScreen(viewModel = viewModel)
                }
                composable("privacy") { PrivacyScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                composable("tasks") { TasksScreen(viewModel = viewModel) }
                composable("connections") { ConnectionsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToAudit = { navController.navigate("capability_audit") }) }
                composable("capability_audit") { CapabilityAuditScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToCompetition = { navController.navigate("capability_competition") }) }
                composable("capability_competition") { CapabilityCompetitionScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }) }
                composable(ShellRoute.Settings.route) {
                    SettingsScreen(viewModel = viewModel, onNavigateBack = { navController.popBackStack() }, onNavigateToPrivacy = { navController.navigate("privacy") })
                }
            }
        }
    }
}
// This file needs all the screens
