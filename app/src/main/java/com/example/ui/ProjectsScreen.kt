package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ArtifactEntity
import com.example.data.ProjectEntity
import com.example.data.ProjectMemoryEntity
import com.example.data.ProjectRepository
import com.example.data.WorkerJobEntity
import kotlinx.coroutines.launch

/**
 * Projects — the authority layer above conversations.
 *
 * Reads canonical records only. When there are no projects it says so rather
 * than showing an example, because an invented project would be exactly the
 * placeholder data the reality contract forbids.
 */
@Composable
fun ProjectsScreen(projects: ProjectRepository) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf<List<ProjectEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<ProjectEntity?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }

    suspend fun refresh() { list = projects.activeProjects() }
    LaunchedEffect(Unit) { refresh() }

    val current = selected
    if (current != null) {
        ProjectDetail(projects, current, onBack = { selected = null })
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Projects", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Persistent above conversations",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = { showCreate = true }) { Text("New") }
        }

        Spacer(Modifier.height(12.dp))

        if (showCreate) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Project title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = newTitle.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    projects.createProject(newTitle.trim())
                                    newTitle = ""
                                    showCreate = false
                                    refresh()
                                }
                            },
                        ) { Text("Create") }
                        TextButton(onClick = { showCreate = false; newTitle = "" }) { Text("Cancel") }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No projects yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list) { p ->
                    Card(onClick = { selected = p }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(p.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            if (p.description.isNotBlank()) {
                                Text(p.description, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "${p.id} · ${p.status}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDetail(
    projects: ProjectRepository,
    project: ProjectEntity,
    onBack: () -> Unit,
) {
    var conversations by remember { mutableStateOf<List<String>>(emptyList()) }
    var repositories by remember { mutableStateOf<List<String>>(emptyList()) }
    var memory by remember { mutableStateOf<List<ProjectMemoryEntity>>(emptyList()) }
    var artifacts by remember { mutableStateOf<List<ArtifactEntity>>(emptyList()) }
    var jobs by remember { mutableStateOf<List<WorkerJobEntity>>(emptyList()) }

    LaunchedEffect(project.id) {
        conversations = projects.conversationsOf(project.id)
        repositories = projects.repositoriesOf(project.id)
        memory = projects.projectMemory(project.id)
        artifacts = projects.artifactsForProject(project.id)
        jobs = projects.jobsForProject(project.id)
    }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back") }
            }
            Text(project.title, style = MaterialTheme.typography.titleLarge)
            Text(
                project.id,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { Section("Conversations", conversations.ifEmpty { listOf("none") }) }
        item { Section("Repositories", repositories.ifEmpty { listOf("none") }) }
        item {
            Section(
                "Project memory (Level 2)",
                memory.map { "[${it.kind}] ${it.statement}  ·  ${it.provenance}" }.ifEmpty { listOf("none") },
            )
        }
        item {
            Section(
                "Artifacts",
                artifacts.map { "${it.kind} · ${it.name} · ${it.contentHash.take(12)}…" }
                    .ifEmpty { listOf("none") },
            )
        }
        item {
            Section(
                "Worker jobs",
                jobs.map { "${it.status} · ${it.capabilityType} · ${it.objective}" }
                    .ifEmpty { listOf("none") },
            )
        }
    }
}

@Composable
private fun Section(title: String, lines: List<String>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            lines.forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

/**
 * Library — every registered artifact, newest first.
 *
 * Shows the content hash because that, not the path, is the artifact's
 * identity: the same bytes written to two locations are one artifact.
 */
@Composable
fun LibraryScreen(projects: ProjectRepository) {
    var artifacts by remember { mutableStateOf<List<ArtifactEntity>>(emptyList()) }
    LaunchedEffect(Unit) { artifacts = projects.recentArtifacts(100) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Library", style = MaterialTheme.typography.titleLarge)
        Text(
            "Artifacts with provenance and content hashes",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        if (artifacts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No artifacts registered yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(artifacts) { a ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(a.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${a.kind} · ${a.sizeBytes} bytes · ${a.provenance}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "sha256 ${a.contentHash.take(24)}…",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                            )
                            a.projectId?.let {
                                Text(
                                    "project $it",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
