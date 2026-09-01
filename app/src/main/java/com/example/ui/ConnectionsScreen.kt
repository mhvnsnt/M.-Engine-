package com.example.ui

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ai.capabilities.connections.AuthorizationResult
import com.example.ai.capabilities.connections.ConnectionProvider
import com.example.ai.capabilities.connections.ConnectionStatus
import com.example.ai.capabilities.connections.CapabilityType
import com.example.ai.capabilities.connections.RealityClassification
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionsScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAudit: () -> Unit = {},
    onNavigateToFabric: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val connectorManager = viewModel.connectorManager
    
    val connectionStates by connectorManager.connectionStates.collectAsState()
    val providers = remember { connectorManager.getAllProviders() }

    LaunchedEffect(Unit) {
        connectorManager.checkAllHealth()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CONNECTIONS") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(providers) { provider ->
                val status = connectionStates[provider.id] ?: ConnectionStatus.UNCONFIGURED
                var caps by remember { mutableStateOf<Set<CapabilityType>>(emptySet()) }
                LaunchedEffect(provider) {
                    caps = provider.discoverCapabilities()
                }
                ConnectionRow(
                    provider = provider,
                    capabilities = caps,
                    status = status,
                    onConnect = {
                        coroutineScope.launch {
                            val result = connectorManager.connectProvider(provider.id, context as? Activity)
                            when (result) {
                                is AuthorizationResult.Success -> {
                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                }
                                is AuthorizationResult.Error -> {
                                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                }
                                is AuthorizationResult.PendingUserAction -> {
                                    Toast.makeText(context, "Please complete authorization.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onRevoke = {
                        coroutineScope.launch {
                            connectorManager.disconnectProvider(provider.id)
                        }
                    }
                )
            }
                        
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onNavigateToAudit() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Manage Capabilities")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onNavigateToFabric() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Manage Execution Fabric")
                }
            }
        }
    }
}

@Composable
fun ConnectionRow(
    provider: ConnectionProvider,
    capabilities: Set<CapabilityType>,
    status: ConnectionStatus,
    onConnect: () -> Unit,
    onRevoke: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Capabilities: ${capabilities.joinToString { it.name }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            when (status) {
                ConnectionStatus.CONNECTED -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connected", color = Color(0xFF4CAF50), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = onRevoke, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Disconnect")
                        }
                    }
                }
                ConnectionStatus.ERROR -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Error", color = Color(0xFFF44336), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = onConnect, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Text("Retry")
                        }
                    }
                }
                ConnectionStatus.UNCONFIGURED -> {
                    Button(onClick = onConnect) {
                        Text("Connect")
                    }
                }
                ConnectionStatus.DISCONNECTED -> {
                    Button(onClick = onConnect) {
                        Text("Connect")
                    }
                }
                ConnectionStatus.PENDING_AUTHORIZATION -> {
                    Button(onClick = onConnect) {
                        Text("Resume Auth")
                    }
                }
            }
        }
    }
}
