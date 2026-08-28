package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EvidenceScreen(viewModel: ChatViewModel) {
    val evidenceRecords = viewModel.evidenceEngine.getRecords()
    val failureClusters = viewModel.failureObservatory.getTopClusters()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Evidence Ledger & Failure Observatory", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (failureClusters.isNotEmpty()) {
                Text("Failure Observatory Clusters", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(failureClusters) { cluster ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("${cluster.category.name} (${cluster.occurrences}x)", style = MaterialTheme.typography.titleSmall)
                                    Text(cluster.primarySignature, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text("Evidence Ledger", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            if (evidenceRecords.isEmpty()) {
                Text("No evidence recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    items(evidenceRecords.reversed()) { ev ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                                val icon = when (ev.verdict.name) {
                                    "VERIFIED_PASS" -> Icons.Default.CheckCircle
                                    "VERIFIED_FAIL" -> Icons.Default.Warning
                                    else -> Icons.Default.Info
                                }
                                val tint = when (ev.verdict.name) {
                                    "VERIFIED_PASS" -> MaterialTheme.colorScheme.primary
                                    "VERIFIED_FAIL" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.secondary
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("${ev.type.name} - ${ev.verdict.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ev.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (ev.expectedResult.isNotBlank() || ev.observedResult.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Exp: ${ev.expectedResult}\nObs: ${ev.observedResult}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
