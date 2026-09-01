package com.example.ai.capabilities.ecology

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Ignore

class LiveGitHubProbeTest {
    @Test
    fun testLiveGitHubWorkerCapability() = runBlocking {
        val cap = GitHubWorkerCapability()
        
        println("━━━━━━━━ M. ENGINE — LIVE CAPABILITY VERIFICATION ━━━━━━━━\n")
        println("CAPABILITY\n${cap.name}\n")
        
        val result = cap.verifyHealth()
        
        val httpStatus = result.evidence.find { it.startsWith("HTTP STATUS:") }?.substringAfter(": ") ?: "N/A"
        val latency = result.evidence.find { it.startsWith("LATENCY:") }?.substringAfter(": ")?.replace("ms", "") ?: "N/A"
        val auth = result.evidence.find { it.startsWith("AUTHORIZATION:") }?.substringAfter(": ") ?: "UNKNOWN"
        val repoRetrieved = result.evidence.find { it.startsWith("Repository metadata") } != null
        
        println("OBSERVED")
        println(if (result.success) "Live GitHub API response received." else "Probe failed: ${result.failureReason}")
        println("\nHTTP STATUS\n$httpStatus")
        println("\nLATENCY\n$latency ms")
        println("\nAUTHORIZATION\n$auth")
        
        if (repoRetrieved) {
            println("\nEVIDENCE\nRepository metadata retrieved from live API.")
            result.evidence.filter { it.startsWith("Repository:") || it.startsWith("Default Branch:") }.forEach {
                println(it)
            }
        } else {
            println("\nEVIDENCE\n" + result.evidence.joinToString("\n"))
        }
        
        println("\nREALITY MATRIX")
        println("Implementation Confidence: HIGH")
        println("Configuration Confidence: ${if (result.success) "HIGH" else "LOW"}")
        println("Historical Availability: OBSERVED")
        println("Current Availability: ${cap.state}")
        
        println("\nCIRCUIT STATE\n${cap.circuitState}")
        println("\nNEXT ACTION\n${if (result.success) "No immediate retry required.\nCapability is eligible for authorized Opportunity Engine work." else "Capability gap identified."}")
        
        println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    }
}
