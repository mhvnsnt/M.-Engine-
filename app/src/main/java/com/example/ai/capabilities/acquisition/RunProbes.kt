package com.example.ai.capabilities.acquisition

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val discovery = PhysicalRuntimeDiscovery()
    val observations = discovery.executeProbes()
    
    observations.forEach { it.printLedger() }
}
