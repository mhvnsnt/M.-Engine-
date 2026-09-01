package com.example.ai.capabilities.federated.environment

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WorkerNodeRecord(
    val nodeId: String,
    val url: String,
    val secret: String,
    var status: FabricNodeState,
    val environmentName: String,
    val capabilities: EnvironmentCapabilities
)

class WorkerRegistry {
    private val _workers = MutableStateFlow<List<WorkerNodeRecord>>(emptyList())
    val workers: StateFlow<List<WorkerNodeRecord>> = _workers.asStateFlow()
    
    fun registerWorker(nodeId: String, url: String, secret: String, status: FabricNodeState, environmentName: String, capabilities: EnvironmentCapabilities) {
        val newRecord = WorkerNodeRecord(nodeId, url, secret, status, environmentName, capabilities)
        _workers.value = _workers.value.filter { it.nodeId != nodeId } + newRecord
    }
    
    fun updateWorkerStatus(nodeId: String, newStatus: FabricNodeState) {
        _workers.value = _workers.value.map { 
            if (it.nodeId == nodeId) it.copy(status = newStatus) else it 
        }
    }

    fun getWorker(nodeId: String): WorkerNodeRecord? {
        return _workers.value.find { it.nodeId == nodeId }
    }
    
    fun getVerifiedWorkers(): List<WorkerNodeRecord> {
        return _workers.value.filter { it.status == FabricNodeState.AVAILABLE || it.status == FabricNodeState.RELIABILITY_UNDER_OBSERVATION || it.status == FabricNodeState.PARTIALLY_VERIFIED }
    }
    
    fun clear() {
        _workers.value = emptyList()
    }
}

object GlobalWorkerRegistry {
    val instance = WorkerRegistry()
}
