package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun TasksScreen(viewModel: ChatViewModel) {
    val jobs by viewModel.jobDao.getAllJobs().collectAsState(initial = emptyList())
    
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Active Tasks & Background Jobs", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (jobs.isEmpty()) {
                Text("No background tasks running.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(jobs) { job ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val icon = when (job.status) {
                                        "COMPLETED" -> Icons.Default.CheckCircle
                                        "FAILED" -> Icons.Default.Warning
                                        else -> Icons.Default.PlayArrow
                                    }
                                    val tint = when (job.status) {
                                        "COMPLETED" -> MaterialTheme.colorScheme.primary
                                        "FAILED" -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.secondary
                                    }
                                    Icon(imageVector = icon, contentDescription = null, tint = tint)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Job #${job.id}: ${job.status}", style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(job.description, style = MaterialTheme.typography.bodyMedium)
                                
                                if (job.logs.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = job.logs,
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                            modifier = Modifier.padding(8.dp).fillMaxWidth()
                                        )
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
