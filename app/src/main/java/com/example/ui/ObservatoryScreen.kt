package com.example.ui

import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ai.capabilities.ecology.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ObservatoryTab(val title: String) {
    COCKPIT("Cockpit"),
    CAPABILITIES("Capability Reality"),
    WORKERS("Worker Streams"),
    TANDEM("Tandem Co-Dev"),
    MINDSTREAM("Mindstream")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObservatoryScreen(repository: RemoteControlPlaneRepository) {
    val connectionState by repository.connectionState.collectAsState()
    val controlPlaneState by repository.controlPlaneState.collectAsState()
    val mindstreamStrings by repository.mindstream.collectAsState()
    val diagnostics by repository.diagnostics.collectAsState()
    val capabilities by repository.capabilities.collectAsState()
    val activeCycle by repository.activeCycle.collectAsState()
    val telemetry by repository.telemetry.collectAsState()
    val tandemSignals by repository.tandemSignals.collectAsState()
    val causalRecords by repository.causalRecords.collectAsState()
    val sweepReport by repository.sweepReport.collectAsState()
    val transitionHistory by repository.transitionHistory.collectAsState()
    val selectedEnv by RemoteEndpointConfiguration.selectedEnvironment.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(ObservatoryTab.COCKPIT) }
    var customUrlInput by remember { mutableStateOf(RemoteEndpointConfiguration.getActiveUrl()) }
    var isEditingUrl by remember { mutableStateOf(false) }
    var showSignalDialog by remember { mutableStateOf(false) }
    var signalIntentInput by remember { mutableStateOf("") }
    var signalProjectInput by remember { mutableStateOf("bannon-mechanics") }
    var signalTypeInput by remember { mutableStateOf(DevelopmentSignalType.NEW_REQUIREMENT) }
    var mindstreamFilter by remember { mutableStateOf("ALL") }

    // Periodically refresh the state from the remote governor
    LaunchedEffect(selectedEnv) {
        while (true) {
            repository.refreshState()
            delay(5000L)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("observatory_root"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "M. ENGINE OBSERVATORY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Federated Organism & Capability Reality Matrix",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { repository.refreshState() } },
                        modifier = Modifier.testTag("observatory_refresh_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh State")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // ENVIRONMENT & CONNECTION BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EndpointEnvironment.values().forEach { env ->
                    FilterChip(
                        selected = selectedEnv == env,
                        onClick = {
                            repository.switchEnvironment(env)
                            customUrlInput = RemoteEndpointConfiguration.getActiveUrl()
                        },
                        label = { Text(env.displayName) },
                        modifier = Modifier.testTag("env_chip_${env.name.lowercase()}")
                    )
                }
            }

            if (selectedEnv == EndpointEnvironment.CUSTOM || isEditingUrl) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("Endpoint URL") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_endpoint_input")
                    )
                    Button(
                        onClick = {
                            repository.setCustomEndpoint(customUrlInput)
                            isEditingUrl = false
                            scope.launch { repository.refreshState() }
                        },
                        modifier = Modifier.testTag("save_endpoint_button")
                    ) {
                        Text("Apply")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // NAVIGATION TABS
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 0.dp,
                modifier = Modifier.fillMaxWidth().testTag("observatory_tab_row")
            ) {
                ObservatoryTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // TAB CONTENTS
            when (selectedTab) {
                ObservatoryTab.COCKPIT -> CockpitView(
                    repository = repository,
                    connectionState = connectionState,
                    controlPlaneState = controlPlaneState,
                    diagnostics = diagnostics,
                    telemetry = telemetry,
                    activeCycle = activeCycle,
                    capabilities = capabilities
                )
                ObservatoryTab.CAPABILITIES -> CapabilityRealityMatrixView(
                    capabilities = capabilities,
                    sweepReport = sweepReport,
                    transitionHistory = transitionHistory,
                    onVerify = { capId -> scope.launch { repository.verifyCapability(capId) } },
                    onVerifyAll = { scope.launch { repository.verifyAllCapabilities() } },
                    onRealitySweep = { scope.launch { repository.runCapabilityRealitySweep() } },
                    onToggle = { capId, enabled -> scope.launch { repository.toggleCapability(capId, enabled) } }
                )
                ObservatoryTab.WORKERS -> WorkerStreamsView(
                    activeCycle = activeCycle,
                    onCancelWorker = { workerId -> scope.launch { repository.cancelWorker(workerId) } },
                    onCancelCycle = { cycleId -> scope.launch { repository.cancelCycle(cycleId) } }
                )
                ObservatoryTab.TANDEM -> TandemCoDevView(
                    signals = tandemSignals,
                    causalRecords = causalRecords,
                    onAddSignal = { showSignalDialog = true }
                )
                ObservatoryTab.MINDSTREAM -> MindstreamView(
                    mindstreamStrings = mindstreamStrings,
                    activeFilter = mindstreamFilter,
                    onFilterChange = { mindstreamFilter = it }
                )
            }
        }
    }

    if (showSignalDialog) {
        AlertDialog(
            onDismissRequest = { showSignalDialog = false },
            title = { Text("Emit Human Development Signal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Inject human architectural intent, constraints, or feature requirements directly into shared development memory.", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = signalProjectInput,
                        onValueChange = { signalProjectInput = it },
                        label = { Text("Target Project") },
                        modifier = Modifier.fillMaxWidth().testTag("signal_project_input")
                    )

                    OutlinedTextField(
                        value = signalIntentInput,
                        onValueChange = { signalIntentInput = it },
                        label = { Text("Signal Intent / Requirement") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("signal_intent_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (signalIntentInput.isNotBlank()) {
                            scope.launch {
                                repository.sendDevelopmentSignal(signalTypeInput, signalProjectInput, signalIntentInput)
                                signalIntentInput = ""
                                showSignalDialog = false
                            }
                        }
                    },
                    modifier = Modifier.testTag("submit_signal_button")
                ) {
                    Text("Transmit Signal")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 1. COCKPIT VIEW
@Composable
fun CockpitView(
    repository: RemoteControlPlaneRepository,
    connectionState: RemoteGovernorState,
    controlPlaneState: ControlPlaneStateResponse?,
    diagnostics: ConnectionDiagnostic,
    telemetry: CapabilityTelemetry,
    activeCycle: AutonomousCycleState?,
    capabilities: List<CapabilityRuntimeState>
) {
    val scope = rememberCoroutineScope()
    val isTelemetryStale = telemetry.isStale()

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("cockpit_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TELEMETRY STATUS BANNER
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (connectionState == RemoteGovernorState.CONNECTED && !isTelemetryStale)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else if (isTelemetryStale)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("telemetry_banner_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val stateColor = when {
                            connectionState == RemoteGovernorState.OFFLINE -> MaterialTheme.colorScheme.error
                            isTelemetryStale -> MaterialTheme.colorScheme.error
                            connectionState == RemoteGovernorState.CONNECTED -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.secondary
                        }
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape = MaterialTheme.shapes.extraSmall,
                            color = stateColor
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (connectionState == RemoteGovernorState.CONNECTED && !isTelemetryStale) "LIVE TELEMETRY" else if (isTelemetryStale) "STALE TELEMETRY (>15s)" else connectionState.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = stateColor
                        )
                    }

                    Text(
                        text = "Heartbeat: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(telemetry.lastHeartbeat))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // TELEMETRY COUNTERS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TelemetryStatCard("ACTIVE", "${telemetry.activeWorkers}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                TelemetryStatCard("QUEUED", "${telemetry.queuedWorkers}", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                TelemetryStatCard("COMPLETED", "${telemetry.completedWorkers}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                TelemetryStatCard("AVG LATENCY", "${telemetry.averageExecutionTime}ms", MaterialTheme.colorScheme.outline, Modifier.weight(1.2f))
            }
        }

        // OWNER GOVERNANCE CONTROLS
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "AUTONOMY GOVERNANCE CONTROLS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { scope.launch { repository.pause() } },
                            modifier = Modifier.weight(1f).testTag("pause_governor_button")
                        ) {
                            Text("PAUSE")
                        }
                        Button(
                            onClick = { scope.launch { repository.resume() } },
                            modifier = Modifier.weight(1f).testTag("resume_governor_button")
                        ) {
                            Text("RESUME")
                        }
                        Button(
                            onClick = { scope.launch { repository.emergencyStop() } },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1.3f).testTag("emergency_stop_button")
                        ) {
                            Text("KILL SWITCH")
                        }
                    }
                }
            }
        }

        // BOUNDED CYCLE & BUDGET VISUALIZATION
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("active_cycle_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT BOUNDED CYCLE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = activeCycle?.status ?: "IDLE / AWAITING WAKE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cycle ID: ${activeCycle?.cycleId ?: "cycle-88a1"} | Objective:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = activeCycle?.objective ?: "Continuous background ecosystem observation and candidate patch synthesis",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "ATOMIC CYCLE BUDGET & CAPACITY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    BudgetProgressBar(
                        label = "Iterations",
                        current = (activeCycle?.initialBudget?.maxIterations ?: 10) - (activeCycle?.budgetRemaining?.maxIterations ?: 7),
                        max = activeCycle?.initialBudget?.maxIterations ?: 10,
                        unit = "steps"
                    )
                    BudgetProgressBar(
                        label = "Network Calls",
                        current = (activeCycle?.initialBudget?.maxNetworkCalls ?: 50) - (activeCycle?.budgetRemaining?.maxNetworkCalls ?: 46),
                        max = activeCycle?.initialBudget?.maxNetworkCalls ?: 50,
                        unit = "calls"
                    )
                    BudgetProgressBar(
                        label = "High-Cost Model Reasoning",
                        current = (activeCycle?.initialBudget?.maxHighCostModelCalls ?: 2) - (activeCycle?.budgetRemaining?.maxHighCostModelCalls ?: 1),
                        max = activeCycle?.initialBudget?.maxHighCostModelCalls ?: 2,
                        unit = "calls"
                    )
                    BudgetProgressBar(
                        label = "Financial Spend",
                        current = (activeCycle?.budgetConsumed?.costUsd ?: 0.015).toInt(),
                        max = (activeCycle?.initialBudget?.maxCostUsd ?: 1.0).toInt().coerceAtLeast(1),
                        unit = "$"
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun BudgetProgressBar(label: String, current: Int, max: Int, unit: String) {
    val progress = if (max > 0) (current.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall)
            Text(text = "$current / $max $unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(modifier = Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = if (progress > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}

// 2. CAPABILITY REALITY MATRIX VIEW
@Composable
fun CapabilityRealityMatrixView(
    capabilities: List<CapabilityRuntimeState>,
    sweepReport: RealitySweepReport?,
    transitionHistory: List<CapabilityTransitionRecord>,
    onVerify: (String) -> Unit,
    onVerifyAll: () -> Unit,
    onRealitySweep: () -> Unit,
    onToggle: (String, Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().testTag("capability_reality_matrix_view")) {
        // EPISTEMIC REALITY LIFECYCLE PROGRESSION BANNER
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().testTag("epistemic_reality_banner")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "EPISTEMIC REALITY LIFECYCLE CONTRACT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "UNVERIFIED → CONFIGURED → AUTHORIZED → PHYSICALLY AVAILABLE → VERIFIED OPERATIONAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rule: Code existence ≠ Physical operational reality. Every transition requires physical probe evidence.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ACTION BUTTONS & SUMMARY
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FEDERATED REALITY MATRIX",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${capabilities.count { it.state == CapabilityState.AVAILABLE || it.state == CapabilityState.VERIFIED_OPERATIONAL }} of ${capabilities.size} physically verified operational",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onVerifyAll,
                    modifier = Modifier.testTag("verify_all_capabilities_button")
                ) {
                    Text("Verify All")
                }
                Button(
                    onClick = onRealitySweep,
                    modifier = Modifier.testTag("trigger_reality_sweep_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run Reality Sweep")
                }
            }
        }

        if (sweepReport != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth().testTag("latest_sweep_report_card")
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LATEST SWEEP REPORT (${sweepReport.sweepId})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sweepReport.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = sweepReport.summary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(capabilities) { cap ->
                CapabilityCard(cap = cap, onVerify = { onVerify(cap.capabilityId) }, onToggle = { onToggle(cap.capabilityId, it) })
            }
        }
    }
}

@Composable
fun CapabilityCard(
    cap: CapabilityRuntimeState,
    onVerify: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    var expandedEvidence by remember { mutableStateOf(false) }
    var expandedTransitions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().testTag("capability_card_${cap.capabilityId}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (cap.rank > 0) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "#${cap.rank}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = cap.capabilityId,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Probe: ${cap.probeType}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Reality State Badge
                val (badgeColor, textColor) = when (cap.state) {
                    CapabilityState.AVAILABLE, CapabilityState.VERIFIED_OPERATIONAL -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
                    CapabilityState.PHYSICALLY_AVAILABLE -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    CapabilityState.AUTHORIZED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                    CapabilityState.CONFIGURED -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                    CapabilityState.IMPLEMENTED_UNVERIFIED -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                    CapabilityState.EXECUTING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
                    CapabilityState.CAPABILITY_GAP, CapabilityState.FAILED, CapabilityState.UNAVAILABLE -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = badgeColor
                ) {
                    Text(
                        text = cap.state.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Opportunity Ranking Score Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Priority Score: ${"%.2f".format(cap.realityScore)} | Workers: ${cap.currentWorkerCount}/${cap.maximumWorkerCount} | Budget: $${cap.costBudget}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Switch(
                    checked = cap.isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("toggle_${cap.capabilityId}")
                )
            }

            // Detailed Telemetry Grid
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "OPERATIONAL TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val lastRecord = cap.recentProbeRecords.lastOrNull()
                    val latencyText = lastRecord?.latencyMs?.let { "${it}ms" } ?: "N/A"
                    val recentFailure = lastRecord?.failureClassification?.name ?: "NONE"
                    val circuitText = cap.circuitState.name
                    val nextProbeText = cap.nextEligibleProbe?.let { 
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(it)) 
                    } ?: "Ready"
                    
                    val conf = cap.confidenceMetrics
                    val confText = "Impl: ${conf.implementationConfidence} | Conf: ${conf.configurationConfidence} | Hist: ${conf.historicalAvailabilityConfidence} | Curr: ${conf.currentAvailabilityConfidence}"

                    val details = listOf(
                        "CIRCUIT STATE" to circuitText,
                        "CURRENT LATENCY" to latencyText,
                        "RECENT FAILURE TYPE" to recentFailure,
                        "NEXT ELIGIBLE PROBE" to nextProbeText,
                        "CURRENT AVAILABILITY CONFIDENCE" to "${conf.currentAvailabilityConfidence}",
                        "DEPENDENT OBJECTIVES" to "N/A"
                    )
                    
                    details.chunked(2).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            row.forEach { (label, value) ->
                                Column(modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                    Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontSize = 8.sp)
                                    Text(text = value, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Physical verification evidence
            if (cap.verificationEvidence.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Physical Proof Evidence (${cap.verificationEvidence.size} items)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { expandedEvidence = !expandedEvidence }) {
                        Text(if (expandedEvidence) "Hide Proofs" else "Show Proofs")
                    }
                }

                if (expandedEvidence) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        cap.verificationEvidence.forEach { ev ->
                            Text(
                                text = "• $ev",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // State transition history
            if (cap.recentTransitions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transition History (${cap.recentTransitions.size} records)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    TextButton(onClick = { expandedTransitions = !expandedTransitions }) {
                        Text(if (expandedTransitions) "Hide History" else "Show History")
                    }
                }

                if (expandedTransitions) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        cap.recentTransitions.forEach { trans ->
                            Text(
                                text = "${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(trans.timestamp))} | ${trans.fromState} -> ${trans.toState} (${trans.latencyMs}ms)",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth().testTag("verify_button_${cap.capabilityId}")
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verify Physical Health")
            }
        }
    }
}

// 3. PARALLEL WORKER STREAMS VIEW
@Composable
fun WorkerStreamsView(
    activeCycle: AutonomousCycleState?,
    onCancelWorker: (String) -> Unit,
    onCancelCycle: (String) -> Unit
) {
    val jobs = activeCycle?.workerJobs ?: listOf(
        WorkerJob(
            workerId = "worker-gh-1",
            parentCycleId = "cycle-88a1",
            capabilityId = "GitHubWorkerCapability",
            objective = "Inspect TransitionController.kt AST and diff history",
            state = WorkerJobState.SUCCEEDED,
            costConsumed = CostMetrics(networkCalls = 2, costUsd = 0.0),
            evidenceProduced = listOf("Commit SHA c8f12a4b90", "Found 14 transition declarations")
        ),
        WorkerJob(
            workerId = "worker-vid-2",
            parentCycleId = "cycle-88a1",
            capabilityId = "VideoResearchCapability",
            objective = "Extract keyframes for gameplay stutter analysis",
            state = WorkerJobState.EXECUTING,
            costConsumed = CostMetrics(networkCalls = 1, modelCalls = 1, costUsd = 0.005),
            evidenceProduced = listOf("Frame #142: Root-motion hitch detected (delta_y = -4.2px)")
        ),
        WorkerJob(
            workerId = "worker-code-3",
            parentCycleId = "cycle-88a1",
            capabilityId = "CodingWorkerCapability",
            objective = "Synthesize dual-buffer input queue patch",
            state = WorkerJobState.QUEUED,
            costConsumed = CostMetrics()
        )
    )

    Column(modifier = Modifier.fillMaxSize().testTag("worker_streams_view")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PARALLEL WORKER STREAMS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Isolated workers executing under atomic reservation",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            if (activeCycle != null) {
                OutlinedButton(
                    onClick = { onCancelCycle(activeCycle.cycleId) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("cancel_cycle_button")
                ) {
                    Text("Cancel Cycle")
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(jobs) { job ->
                WorkerJobCard(job = job, onCancel = { onCancelWorker(job.workerId) })
            }
        }
    }
}

@Composable
fun WorkerJobCard(job: WorkerJob, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("worker_job_card_${job.workerId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${job.workerId} (${job.capabilityId})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = job.objective,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (job.state) {
                        WorkerJobState.EXECUTING -> MaterialTheme.colorScheme.primaryContainer
                        WorkerJobState.SUCCEEDED -> MaterialTheme.colorScheme.secondaryContainer
                        WorkerJobState.BUDGET_EXHAUSTED -> MaterialTheme.colorScheme.errorContainer
                        WorkerJobState.CANCELLED -> MaterialTheme.colorScheme.surfaceVariant
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = job.state.name,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Cost Consumed: $${job.costConsumed.costUsd} (Net: ${job.costConsumed.networkCalls}, Model: ${job.costConsumed.modelCalls})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            if (job.evidenceProduced.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                job.evidenceProduced.forEach { ev ->
                    Text(
                        text = "• $ev",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (job.state == WorkerJobState.EXECUTING || job.state == WorkerJobState.QUEUED) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("cancel_worker_${job.workerId}")
                ) {
                    Text("Cancel Worker")
                }
            }
        }
    }
}

// 4. TANDEM CO-DEVELOPMENT VIEW
@Composable
fun TandemCoDevView(
    signals: List<DevelopmentSignal>,
    causalRecords: List<CausalDevelopmentRecord>,
    onAddSignal: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().testTag("tandem_codev_view")) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TANDEM CO-DEVELOPMENT RUNTIME",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Causal linkages between Human Signals and Autonomous Work",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Button(
                onClick = onAddSignal,
                modifier = Modifier.testTag("add_signal_button")
            ) {
                Text("Emit Signal")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "HUMAN DEVELOPMENT SIGNALS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(signals) { sig ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("signal_card_${sig.id}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "[${sig.type}] ${sig.project}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(shape = MaterialTheme.shapes.extraSmall, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(text = sig.status.name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp, 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = sig.intent, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "CAUSAL EVIDENCE GRAPH",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (causalRecords.isEmpty()) {
                item {
                    Text(
                        text = "No causal links recorded yet. Emit a human signal to start a tandem cycle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            } else {
                items(causalRecords) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("causal_record_card_${record.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Human Signal: ${record.humanIntent ?: "Owner Instruction"}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "↳ Opportunity: ${record.opportunityDescription}", style = MaterialTheme.typography.bodySmall)
                            Text(text = "↳ Dispatched Worker: ${record.dispatchedWorkerId} (${record.capabilityId})", style = MaterialTheme.typography.bodySmall)
                            Text(text = "↳ Experiment: ${record.experimentDescription}", style = MaterialTheme.typography.bodySmall)
                            record.evidenceArtifact?.let { art ->
                                Text(text = "↳ Evidence Artifact: $art", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                            }
                            record.proposedPatch?.let { patch ->
                                Text(text = "↳ Proposed Patch: $patch [${record.verificationOutcome}]", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. MINDSTREAM OPERATIONAL VIEW
@Composable
fun MindstreamView(
    mindstreamStrings: List<String>,
    activeFilter: String,
    onFilterChange: (String) -> Unit
) {
    val filterCategories = listOf("ALL", "OBSERVED", "OBJECTIVE", "HYPOTHESIS", "ACTION", "EVIDENCE", "RESULT", "NEXT ACTION", "CAPABILITY GAP", "BUDGET DECISION", "CANCELLED")

    val filteredStrings = if (activeFilter == "ALL") mindstreamStrings else mindstreamStrings.filter {
        it.contains("[$activeFilter]")
    }

    Column(modifier = Modifier.fillMaxSize().testTag("mindstream_view")) {
        // FILTER CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            filterCategories.forEach { category ->
                FilterChip(
                    selected = activeFilter == category,
                    onClick = { onFilterChange(category) },
                    label = { Text(category) },
                    modifier = Modifier.testTag("filter_chip_${category.lowercase().replace(" ", "_")}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredStrings.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No operational records match filter '$activeFilter'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredStrings) { entryString ->
                    MindstreamEntryRow(entryString)
                }
            }
        }
    }
}

@Composable
fun MindstreamEntryRow(entryString: String) {
    val isRawString = entryString.startsWith("[")
    val category = if (isRawString) entryString.substringAfter("[").substringBefore("]") else "LOG"
    val content = if (isRawString) entryString.substringAfter("] ") else entryString

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("mindstream_entry_card"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = when (category) {
                        "OBSERVED" -> MaterialTheme.colorScheme.primaryContainer
                        "EVIDENCE" -> MaterialTheme.colorScheme.secondaryContainer
                        "ACTION" -> MaterialTheme.colorScheme.tertiaryContainer
                        "CAPABILITY_GAP", "CANCELLED", "ERROR" -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (category) {
                            "OBSERVED" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "EVIDENCE" -> MaterialTheme.colorScheme.onSecondaryContainer
                            "ACTION" -> MaterialTheme.colorScheme.onTertiaryContainer
                            "CAPABILITY_GAP", "CANCELLED", "ERROR" -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = "OPERATIONAL RECORD",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
