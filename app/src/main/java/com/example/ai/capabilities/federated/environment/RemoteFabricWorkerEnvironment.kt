package com.example.ai.capabilities.federated.environment

import org.json.JSONObject


import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CommandResult(val exitCode: Int, val stdout: String, val stderr: String)

class RemoteFabricWorkerEnvironment(
    private val workerUrl: String,
    private val secret: String,
    override val environmentId: String = "env_remote_worker_${workerUrl.hashCode()}",
    initialCapabilities: EnvironmentCapabilities? = null,
    initialEnvironmentName: String? = null
) : ExecutionEnvironment {
    
    override var environmentName: String = initialEnvironmentName ?: "Unknown Remote Worker"
        private set
        
    override var capabilities = initialCapabilities ?: EnvironmentCapabilities(
        shellExecution = CapabilityLevel.UNAVAILABLE,
        filesystemRead = CapabilityLevel.UNAVAILABLE,
        filesystemWrite = CapabilityLevel.UNAVAILABLE,
        processSpawning = CapabilityLevel.UNAVAILABLE,
        persistentProcessSupport = CapabilityLevel.UNAVAILABLE,
        networkEgress = CapabilityLevel.UNAVAILABLE,
        inboundNetworkSupport = CapabilityLevel.UNAVAILABLE,
        dockerCli = CapabilityLevel.UNAVAILABLE,
        dockerDaemon = CapabilityLevel.UNAVAILABLE,
        podman = CapabilityLevel.UNAVAILABLE,
        browserAutomation = CapabilityLevel.UNAVAILABLE,
        gpuAvailability = CapabilityLevel.UNAVAILABLE,
        localModelRuntime = CapabilityLevel.UNAVAILABLE,
        databaseAccess = CapabilityLevel.UNAVAILABLE,
        secretAccess = CapabilityLevel.UNAVAILABLE,
        maximumExecutionDurationMs = null,
        persistenceAcrossProcessDeath = CapabilityLevel.UNAVAILABLE
    )
    
    var nodeState: FabricNodeState = FabricNodeState.UNAVAILABLE
        private set

    override suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$workerUrl/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $secret")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            val success = connection.responseCode == 200
            if (success && nodeState != FabricNodeState.AVAILABLE) {
                nodeState = FabricNodeState.AVAILABLE
            }
            return@withContext success
        } catch (e: Exception) {
            nodeState = FabricNodeState.UNAVAILABLE
            return@withContext false
        }
    }

    override suspend fun probeCapabilities(): EnvironmentCapabilities = withContext(Dispatchers.IO) {
        nodeState = FabricNodeState.PROBING
        try {
            val url = URL("$workerUrl/probe")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $secret")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseStr)
                
                // Parse environmentName from probe response
                if (json.has("environmentName")) {
                    environmentName = json.getString("environmentName")
                }
                
                val parsedCapabilities = EnvironmentCapabilities(
                    shellExecution = parseLevel(json.optString("shellExecution", null)),
                    filesystemRead = parseLevel(json.optString("filesystemRead", null)),
                    filesystemWrite = parseLevel(json.optString("filesystemWrite", null)),
                    dockerCli = parseLevel(json.optString("dockerCli", null)),
                    browserAutomation = parseLevel(json.optString("browserAutomation", null)),
                    // Keep the rest unavailable for now in remote probe
                    processSpawning = CapabilityLevel.UNAVAILABLE,
                    persistentProcessSupport = CapabilityLevel.UNAVAILABLE,
                    networkEgress = CapabilityLevel.UNAVAILABLE,
                    inboundNetworkSupport = CapabilityLevel.UNAVAILABLE,
                    dockerDaemon = CapabilityLevel.UNAVAILABLE,
                    podman = CapabilityLevel.UNAVAILABLE,
                    gpuAvailability = CapabilityLevel.UNAVAILABLE,
                    localModelRuntime = CapabilityLevel.UNAVAILABLE,
                    databaseAccess = CapabilityLevel.UNAVAILABLE,
                    secretAccess = CapabilityLevel.UNAVAILABLE,
                    maximumExecutionDurationMs = null,
                    persistenceAcrossProcessDeath = CapabilityLevel.UNAVAILABLE
                )
                
                this@RemoteFabricWorkerEnvironment.capabilities = parsedCapabilities
                nodeState = FabricNodeState.AVAILABLE
                return@withContext parsedCapabilities
            } else {
                nodeState = FabricNodeState.UNAVAILABLE
                throw Exception("Probe failed with status code ${connection.responseCode}")
            }
        } catch (e: Exception) {
            nodeState = FabricNodeState.UNAVAILABLE
            throw e
        }
    }
    
    suspend fun executeCommand(command: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("$workerUrl/jobs")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $secret")
            connection.doOutput = true
            
            val escapedCommand = command.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
            val jsonPayload = """{
                "jobId": "job_${System.currentTimeMillis()}",
                "capability": "NATIVE_EXECUTION",
                "workspace": { "mode": "EPHEMERAL" },
                "task": {
                    "type": "NATIVE_COMMAND",
                    "command": "$escapedCommand"
                },
                "authorization": {},
                "budget": {}
            }"""
            
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonPayload)
                writer.flush()
            }
            
            if (connection.responseCode == 200) {
                val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseStr)
                
                val jobId = json.getString("jobId")
                
                // Poll for completion
                while (true) {
                    delay(500)
                    val pollUrl = URL("$workerUrl/jobs/$jobId")
                    val pollConn = pollUrl.openConnection() as HttpURLConnection
                    pollConn.requestMethod = "GET"
                    pollConn.setRequestProperty("Authorization", "Bearer $secret")
                    if (pollConn.responseCode == 200) {
                        val pollStr = pollConn.inputStream.bufferedReader().use { it.readText() }
                        val pollJson = JSONObject(pollStr)
                        val status = pollJson.getString("status")
                        if (status == "AWAITING_VERIFICATION" || status == "VERIFIED" || status == "FAILED") {
                            val resultObj = pollJson.optJSONObject("result")
                            return@withContext CommandResult(
                                exitCode = resultObj?.optInt("exitCode", -1) ?: -1,
                                stdout = resultObj?.optString("stdout", "") ?: "",
                                stderr = resultObj?.optString("stderr", "") ?: ""
                            )
                        }
                    }
                }
                return@withContext CommandResult(-1, "", "Timeout or unknown error")
            } else {
                return@withContext CommandResult(-1, "", "Failed with status code ${connection.responseCode}")
            }
        } catch (e: Exception) {
            return@withContext CommandResult(-1, "", "Execution Exception: ${e.message}")
        }
    }

    private fun parseLevel(levelString: String?): CapabilityLevel {
        return try {
            if (levelString != null) CapabilityLevel.valueOf(levelString) else CapabilityLevel.UNAVAILABLE
        } catch (e: Exception) {
            CapabilityLevel.UNAVAILABLE
        }
    }
}
