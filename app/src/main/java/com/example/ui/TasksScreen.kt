package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun TasksScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Universal Development Loop", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Simulated Universal Development Loop state
            val loopSteps = listOf(
                "UNDERSTAND" to true,
                "RETRIEVE MEMORY + CONTEXT" to true,
                "RESEARCH" to true,
                "PLAN" to true,
                "SELECT MODEL / WORKERS" to true,
                "IMPLEMENT" to false,
                "BUILD" to false,
                "RUN" to false,
                "OBSERVE REALITY" to false,
                "COMPARE EXPECTED vs ACTUAL" to false,
                "DIAGNOSE & FIX" to false,
                "REGRESSION TEST" to false,
                "SECURITY CHECK" to false,
                "EVIDENCE REVIEW" to false,
                "PR & RELEASE" to false
            )
            
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(loopSteps.size) { index ->
                    val step = loopSteps[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (step.second) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (step.second) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (step.second) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = step.first,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (step.second) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
