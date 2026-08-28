package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ai.capabilities.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilityAuditScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCompetition: () -> Unit = {}
) {
    var auditResults by remember { mutableStateOf<List<CapabilityInventoryItem>>(emptyList()) }
    var isAuditing by remember { mutableStateOf(false) }
    
    val reposToAudit = listOf(
        "M.-Engine-",
        "God-Mode-OS-D3MN-V2",
        "God-Mode-OS-D3MN",
        "Bannon",
        "bolt.diy-M",
        "Dream-Infinite-World",
        "Wrestli6game-3",
        "M-Hero-Simulator-"
    )
    
    LaunchedEffect(Unit) {
        if (auditResults.isEmpty()) {
            isAuditing = true
            val results = viewModel.runRecursiveAudit(reposToAudit)
            auditResults = results
            isAuditing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recursive Capability Audit") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text(
                "M. Engine Research Loop - Capability Harvest Matrix",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Auditing ${reposToAudit.size} repositories to determine what capabilities should be harvested based on the Evidence Ledger.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isAuditing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Analyzing repositories (this may take a moment)...")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(auditResults) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(item.name, style = MaterialTheme.typography.titleMedium)
                                Text("Repo: ${item.implementationRef ?: "Unknown"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.description, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val stateColor = when(item.state) {
                                    InventoryState.ALREADY_EXISTS -> MaterialTheme.colorScheme.primary
                                    InventoryState.PARTIALLY_EXISTS -> MaterialTheme.colorScheme.secondary
                                    InventoryState.EXPERIMENTAL -> MaterialTheme.colorScheme.tertiary
                                    InventoryState.BROKEN -> MaterialTheme.colorScheme.error
                                    InventoryState.MISSING -> MaterialTheme.colorScheme.error
                                }
                                Text("Status: ${item.state.name}", color = stateColor, style = MaterialTheme.typography.labelMedium)
                                
                                if (item.state == InventoryState.MISSING || item.state == InventoryState.EXPERIMENTAL) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { onNavigateToCompetition() }) {
                                        Text("Initiate Research & Acquisition")
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
