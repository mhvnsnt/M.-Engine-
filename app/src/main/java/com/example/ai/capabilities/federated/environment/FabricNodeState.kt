package com.example.ai.capabilities.federated.environment

enum class FabricNodeState {
    DISCOVERED,
    PROBING,
    PARTIALLY_VERIFIED,
    AVAILABLE,
    RELIABILITY_UNDER_OBSERVATION,
    UNAVAILABLE
}
