package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(viewModel: ChatViewModel, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Data Control") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Manage what M. Engine has learned about you and your locations. Deleting this data is permanent and will reset the model's contextual awareness.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Memory
            Text("Memory & Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Button(
                onClick = { viewModel.clearCoreMemory() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear CORE Memory")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear CORE Memories")
            }

            Button(
                onClick = { viewModel.clearEpisodicMemory() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Episodic Memory")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Episodic Memories")
            }

            // Location
            Spacer(modifier = Modifier.height(16.dp))
            Text("Location & Region Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            Button(
                onClick = { viewModel.clearAllRegionProfiles() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Regions")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Forget All Visited Regions")
            }

            Button(
                onClick = { viewModel.clearLocationSnapshots() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear Snapshots")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear Location History")
            }

            // Nuke all
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { 
                    viewModel.clearMemory()
                    viewModel.clearAllRegionProfiles()
                    viewModel.clearLocationSnapshots()
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Nuke All")
                Spacer(modifier = Modifier.width(8.dp))
                Text("NUKE ALL DATA (Hard Reset)")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
