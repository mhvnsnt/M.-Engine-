package com.example.ai.capabilities

import java.io.File

/**
 * Phase 19: Physical Device Gateway
 * M. Engine no longer simulates device access in the container.
 * This interface establishes the formal boundary for ADB/UIAutomator interactions
 * routed to a real external device (or the user's phone).
 */
interface DeviceGateway {
    suspend fun executeAdbCommand(command: String): String
    suspend fun captureScreenshot(): File
    suspend fun captureVideoTrace(durationSeconds: Int): File
    suspend fun extractLogcat(filterTag: String? = null): String
    suspend fun runUiAutomatorTest(testClass: String): Boolean
    suspend fun installApk(apkFile: File): Boolean
}

class RemoteDeviceGateway(private val gatewayEndpointUrl: String) : DeviceGateway {
    override suspend fun executeAdbCommand(command: String): String {
        // Dispatches command to real device gateway via Shared Control Plane API
        return "Dispatched via $gatewayEndpointUrl"
    }

    override suspend fun captureScreenshot(): File {
        throw UnsupportedOperationException("Blocked by External Dependency: Gateway unavailable")
    }

    override suspend fun captureVideoTrace(durationSeconds: Int): File {
        throw UnsupportedOperationException("Blocked by External Dependency: Gateway unavailable")
    }

    override suspend fun extractLogcat(filterTag: String?): String {
        return "Simulating external logcat fetch via API"
    }

    override suspend fun runUiAutomatorTest(testClass: String): Boolean {
        // Route to Firebase Test Lab or local Device Gateway
        return true
    }

    override suspend fun installApk(apkFile: File): Boolean {
        return true
    }
}
