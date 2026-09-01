package com.example.ai.capabilities.acquisition

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhysicalRuntimeDiscovery {

    suspend fun executeProbes(): List<PhysicalProbeObservation> {
        return listOf(
            probeDockerCli(),
            probeDockerDaemon(),
            probePodmanCli(),
            probeGit(),
            probeJava(),
            probeNode(),
            probePython(),
            probeOllamaCli(),
            probeOllamaServer(),
            probeSQLite()
        )
    }

    private suspend fun runCommand(vararg command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(false)
                .start()
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText().trim()
            val exitCode = process.waitFor()
            CommandResult(exitCode, stdout, stderr)
        } catch (e: Exception) {
            CommandResult(-1, "", e.message ?: "Exception executing command")
        }
    }

    private suspend fun probeDockerCli(): PhysicalProbeObservation {
        val result = runCommand("docker", "--version")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.DISCOVERED else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "Docker CLI",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Docker executable found" else "Executable not found or failed",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }

    private suspend fun probeDockerDaemon(): PhysicalProbeObservation {
        val result = runCommand("docker", "info")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.REACHABLE else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "Docker Daemon",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Docker daemon reachable" else "Docker daemon unreachable or unavailable",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }

    private suspend fun probePodmanCli(): PhysicalProbeObservation {
        val result = runCommand("podman", "--version")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.DISCOVERED else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "Podman CLI",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Podman executable found" else "Executable not found or failed",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }
    
    private suspend fun probeGit(): PhysicalProbeObservation {
        val result = runCommand("git", "--version")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.DISCOVERED else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "Git",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Git executable found" else "Executable not found or failed",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }

    private suspend fun probeJava(): PhysicalProbeObservation {
        val result = runCommand("java", "-version")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.DISCOVERED else CapabilityLifecycleState.NOT_DISCOVERED
        // Java prints version to stderr
        return PhysicalProbeObservation(
            toolName = "Java (JVM)",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Java executable found" else "Executable not found or failed",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }

    private suspend fun probeNode(): PhysicalProbeObservation {
        val result = runCommand("node", "--version")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.DISCOVERED else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "Node.js",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Node executable found" else "Executable not found or failed",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }
    
    private suspend fun probePython(): PhysicalProbeObservation {
        val result = runCommand("python3", "--version")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.DISCOVERED else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "Python",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Python executable found" else "Executable not found or failed",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }

    private suspend fun probeOllamaCli(): PhysicalProbeObservation {
        val result = runCommand("ollama", "-v")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.DISCOVERED else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "Ollama CLI",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Ollama executable found" else "Executable not found or failed",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }

    private suspend fun probeOllamaServer(): PhysicalProbeObservation {
        val result = runCommand("curl", "-s", "http://localhost:11434/api/tags")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.REACHABLE else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "Ollama Server",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "Ollama server reachable at localhost" else "Server unreachable",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }

    private suspend fun probeSQLite(): PhysicalProbeObservation {
        val result = runCommand("sqlite3", "--version")
        val state = if (result.exitCode == 0) CapabilityLifecycleState.DISCOVERED else CapabilityLifecycleState.NOT_DISCOVERED
        return PhysicalProbeObservation(
            toolName = "SQLite",
            lifecycleState = state,
            evidence = if (result.exitCode == 0) "SQLite executable found" else "Executable not found or failed",
            stdout = result.stdout,
            stderr = result.stderr,
            exitCode = result.exitCode
        )
    }
}

data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)
