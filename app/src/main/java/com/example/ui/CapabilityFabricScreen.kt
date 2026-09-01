package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.capabilities.federated.environment.FabricNodeState
import com.example.ai.capabilities.federated.provider.CapabilityCatalogEntry
import com.example.ai.capabilities.federated.provider.CapabilityFabric
import kotlinx.coroutines.launch

/**
 * Capability Catalog — every federated provider and what a real probe of its
 * backend actually found.
 *
 * This screen never claims availability. Each row shows the result of an actual
 * network probe performed on this device, mapped onto the REALITY_CONTRACT.md
 * state vocabulary, plus the reason when a backend is missing. A provider whose
 * runtime is not running reads BLOCKED_BY_EXTERNAL_DEPENDENCY and says which
 * endpoint failed — that is the honest answer, and it doubles as the install
 * instruction.
 */
@Composable
fun CapabilityFabricScreen(fabric: CapabilityFabric) {
    val catalog by fabric.catalog.collectAsStateWithLifecycle()
    val probing by fabric.probing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Probe once on entry so the screen shows measured state, never a guess.
    LaunchedEffect(Unit) {
        if (catalog.isEmpty()) fabric.probeAll()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Capability Fabric", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Federated open-source backends, probed from this device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { scope.launch { fabric.probeAll() } },
                enabled = !probing,
                modifier = Modifier.padding(start = 8.dp),
            ) { Text(if (probing) "Probing" else "Probe") }
        }

        Spacer(Modifier.height(8.dp))

        val available = catalog.count { it.status == FabricNodeState.AVAILABLE }
        if (catalog.isNotEmpty()) {
            Text(
                "$available of ${catalog.size} providers reachable",
                style = MaterialTheme.typography.labelLarge,
                color = if (available > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            )
        }

        if (probing) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(12.dp))

        if (catalog.isEmpty() && !probing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No probe has run yet.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(catalog) { entry -> CapabilityRow(entry) }
            }
        }
    }
}

@Composable
private fun CapabilityRow(entry: CapabilityCatalogEntry) {
    val tone = when (entry.status) {
        FabricNodeState.AVAILABLE -> Color(0xFF2E7D32)
        FabricNodeState.PARTIALLY_VERIFIED,
        FabricNodeState.RELIABILITY_UNDER_OBSERVATION -> Color(0xFFF9A825)
        FabricNodeState.UNAVAILABLE -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.capabilityType.name.replace('_', ' '),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        entry.providerId,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    entry.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = tone,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(6.dp))

            // The contract's own vocabulary, so the UI and the policy agree.
            Text(
                entry.realityState,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = tone,
            )

            entry.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            if (entry.details.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                entry.details.forEach { (k, v) ->
                    Text(
                        "$k: $v",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                "probed in ${entry.probeDurationMs} ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
