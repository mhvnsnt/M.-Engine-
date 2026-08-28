package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.capabilities.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CapabilityCompetitionScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var competitionResult by remember { mutableStateOf<AcquisitionResult?>(null) }
    
    val capabilityToTest = "Repository Auditing"

    LaunchedEffect(Unit) {
        if (competitionResult == null) {
            isRunning = true
            competitionResult = viewModel.runCapabilityCompetition(capabilityToTest)
            isRunning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Evidence-Gated Capability Competition") },
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
                "M. Engine Research Loop - Phase 15C",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Target: $capabilityToTest",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isRunning) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Executing Evidence-Gated Integration Pipeline...\nDiscover → Retrieve → Inspect → License → Security → Build → Benchmark → Compare → Decide")
            } else {
                competitionResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.decision == CompetitionDecision.REPLACE) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Winner: ${result.candidate.name}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val decisionText = when (result.decision) {
                                CompetitionDecision.REPLACE -> "I replaced the native implementation with ${result.candidate.name} because it demonstrated superior performance under benchmark."
                                CompetitionDecision.KEEP -> "I kept the native implementation because external candidates failed to demonstrate superior performance under benchmark."
                                CompetitionDecision.REJECT -> "Rejected external candidates due to security or build failures."
                                else -> result.message
                            }
                            
                            Text(decisionText, style = MaterialTheme.typography.bodyLarge)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Evidence & Provenance:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            result.benchmarkMetrics?.let { metrics ->
                                Text("Delta Score: +${metrics.deltaScore}")
                                Text("Build Success: ${metrics.candidateMetrics.buildSuccess}")
                                Text("Speed/Latency: ${metrics.candidateMetrics.latencyMs}ms")
                            }
                            
                            Text("Status: ${result.status}")
                            if (result.prUrl != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("PR Created for Human Approval:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(result.prUrl)
                            }
                        }
                    }
                }
            }
        }
    }
}
