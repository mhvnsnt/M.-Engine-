package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.ai.capabilities.federated.environment.FabricNodeState
import com.example.ai.capabilities.federated.environment.GlobalWorkerRegistry
import com.example.ai.capabilities.federated.environment.RemoteFabricWorkerEnvironment
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FabricScreen(
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val workers by GlobalWorkerRegistry.instance.workers.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var enrollmentSecret by remember { mutableStateOf("") }
    
    var workerUrlInput by remember { mutableStateOf("") }
    var isProbing by remember { mutableStateOf(false) }
    var probeError by remember { mutableStateOf<String?>(null) }
    
    // Auto-health poll
    LaunchedEffect(workers) {
        while (true) {
            delay(10000)
            workers.forEach { worker ->
                val env = RemoteFabricWorkerEnvironment(worker.url, worker.secret)
                val health = env.checkHealth()
                GlobalWorkerRegistry.instance.updateWorkerStatus(
                    worker.nodeId, 
                    if (health) FabricNodeState.AVAILABLE else FabricNodeState.UNAVAILABLE
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EXECUTION FABRIC") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                enrollmentSecret = UUID.randomUUID().toString().replace("-", "").take(16)
                workerUrlInput = ""
                probeError = null
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Secure Worker Enrollment")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("Federated Worker Nodes", style = MaterialTheme.typography.titleLarge)
            Text(
                "Securely execute autonomous tasks on remote machines, isolated sandboxes, or your personal laptop via mutual authentication.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            
            if (workers.isEmpty()) {
                Text("No workers currently registered in the fabric.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(workers) { worker ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(worker.environmentName, fontWeight = FontWeight.Bold)
                                    Text(worker.url, style = MaterialTheme.typography.bodySmall)
                                    val statusColor = when (worker.status) {
                                        FabricNodeState.AVAILABLE -> MaterialTheme.colorScheme.primary
                                        FabricNodeState.PROBING -> MaterialTheme.colorScheme.secondary
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                    Text(worker.status.name, color = statusColor, style = MaterialTheme.typography.bodySmall)
                                }
                                
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        Toast.makeText(context, "Running Reality Trial...", Toast.LENGTH_SHORT).show()
                                        try {
                                            val env = RemoteFabricWorkerEnvironment(worker.url, worker.secret)
                                            val trialResult = env.executeCommand("echo 'CROSS_MACHINE_TRIAL_SUCCESS'")
                                            if (trialResult.exitCode == 0 && trialResult.stdout.contains("CROSS_MACHINE_TRIAL_SUCCESS")) {
                                                Toast.makeText(context, "Trial Success: Evidence verified across machine boundary.", Toast.LENGTH_LONG).show()
                                                GlobalWorkerRegistry.instance.updateWorkerStatus(worker.nodeId, FabricNodeState.PARTIALLY_VERIFIED)
                                            } else {
                                                Toast.makeText(context, "Trial Failed: Exit ${trialResult.exitCode}", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Trial Failed: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Reality Trial")
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Secure One-Tap Enrollment") },
                text = {
                    Column {
                        Text("1. Run the worker on your remote machine:")
                        val command = "python3 federated_worker.py --secret $enrollmentSecret"
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(command, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = { 
                                    clipboardManager.setText(AnnotatedString(command))
                                    Toast.makeText(context, "Command copied!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                }
                            }
                        }
                        
                        Text("2. Expose it (e.g., ngrok http 9092) and enter the public URL:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = workerUrlInput,
                            onValueChange = { workerUrlInput = it },
                            label = { Text("Worker URL (https://...)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (probeError != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(probeError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isProbing = true
                                probeError = null
                                try {
                                    val env = RemoteFabricWorkerEnvironment(workerUrlInput, enrollmentSecret)
                                    val health = env.checkHealth()
                                    if (!health) throw Exception("Health check failed on $workerUrlInput")
                                    val caps = env.probeCapabilities()
                                    
                                    GlobalWorkerRegistry.instance.registerWorker(
                                        nodeId = "worker_${UUID.randomUUID().toString().take(8)}",
                                        url = workerUrlInput,
                                        secret = enrollmentSecret,
                                        status = FabricNodeState.AVAILABLE,
                                        environmentName = env.environmentName,
                                        capabilities = caps
                                    )
                                    showAddDialog = false
                                } catch (e: Exception) {
                                    probeError = "Connection failed: ${e.message}"
                                } finally {
                                    isProbing = false
                                }
                            }
                        },
                        enabled = workerUrlInput.isNotBlank() && !isProbing
                    ) {
                        if (isProbing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Text("Pair Worker")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
