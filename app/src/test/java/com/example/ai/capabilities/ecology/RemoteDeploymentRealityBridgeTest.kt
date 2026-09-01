package com.example.ai.capabilities.ecology

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RemoteDeploymentRealityBridgeTest {

    private lateinit var reconciliationEngine: EvidenceReconciliationEngine

    @Before
    fun setup() {
        reconciliationEngine = EvidenceReconciliationEngine()
        RemoteEndpointConfiguration.setEnvironment(EndpointEnvironment.LOCAL_EMULATOR)
    }

    @Test
    fun testEndpointConfigurationSwitching() {
        // Default environment is LOCAL_EMULATOR
        RemoteEndpointConfiguration.setEnvironment(EndpointEnvironment.LOCAL_EMULATOR)
        assertEquals(EndpointEnvironment.LOCAL_EMULATOR, RemoteEndpointConfiguration.selectedEnvironment.value)
        assertEquals("http://10.0.2.2:8080/", RemoteEndpointConfiguration.getActiveUrl())
        assertEquals(TransportSecurityState.PLAINTEXT_HTTP, RemoteEndpointConfiguration.getTransportSecurity(RemoteEndpointConfiguration.getActiveUrl()))

        // Switch to STAGING
        RemoteEndpointConfiguration.setEnvironment(EndpointEnvironment.STAGING)
        assertEquals(EndpointEnvironment.STAGING, RemoteEndpointConfiguration.selectedEnvironment.value)
        assertEquals("https://staging-control-plane.mengine.internal/", RemoteEndpointConfiguration.getActiveUrl())
        assertEquals(TransportSecurityState.TLS_SECURE, RemoteEndpointConfiguration.getTransportSecurity(RemoteEndpointConfiguration.getActiveUrl()))

        // Switch to PRODUCTION
        RemoteEndpointConfiguration.setEnvironment(EndpointEnvironment.PRODUCTION)
        assertEquals(EndpointEnvironment.PRODUCTION, RemoteEndpointConfiguration.selectedEnvironment.value)
        assertEquals("https://control-plane.mengine.internal/", RemoteEndpointConfiguration.getActiveUrl())
        assertEquals(TransportSecurityState.TLS_SECURE, RemoteEndpointConfiguration.getTransportSecurity(RemoteEndpointConfiguration.getActiveUrl()))

        // Custom URL configuration with automatic trailing slash formatting
        RemoteEndpointConfiguration.setCustomUrl("http://192.168.1.50:9090")
        assertEquals(EndpointEnvironment.CUSTOM, RemoteEndpointConfiguration.selectedEnvironment.value)
        assertEquals("http://192.168.1.50:9090/", RemoteEndpointConfiguration.getActiveUrl())
        assertEquals(TransportSecurityState.PLAINTEXT_HTTP, RemoteEndpointConfiguration.getTransportSecurity(RemoteEndpointConfiguration.getActiveUrl()))
    }

    @Test
    fun testFederatedEvidenceReconciliation() {
        // Test matching repository commit against remote entries
        val localCommit = EvidenceOfAction.RepositoryObserved(
            commitSha = "c8f12a4b90",
            filesInspected = listOf("build.gradle.kts", "schema.sql")
        )
        val remoteEntriesWithCommit = listOf(
            "[DISCOVER] Inspected commit c8f12a4b90",
            "[SYSTEM] Heartbeat OK"
        )
        val recordConfirmed = reconciliationEngine.reconcileFederatedEvidence(localCommit, remoteEntriesWithCommit)
        assertEquals(ReconciliationOutcome.CONFIRMED, recordConfirmed.outcome)
        assertEquals("REMOTE_c8f12a4b", recordConfirmed.remoteEvidenceId)

        // Test un-synced commit resulting in MERGED
        val remoteEntriesWithoutCommit = listOf("[SYSTEM] Heartbeat OK")
        val recordMerged = reconciliationEngine.reconcileFederatedEvidence(localCommit, remoteEntriesWithoutCommit)
        assertEquals(ReconciliationOutcome.MERGED, recordMerged.outcome)
        assertNull(recordMerged.remoteEvidenceId)
    }

    @Test
    fun testToolingAnomalyEvidenceLogging() {
        val anomaly = EvidenceOfAction.ToolingAnomalyObserved(
            event = "KSP_AWT_APPLICATION_MANAGER_NULL",
            affectedThread = "AWT-EventQueue-0",
            buildTask = ":app:assembleDebug",
            artifactOutcome = "BUILD_SUCCESSFUL",
            impactObserved = "NONE_IN_CURRENT_BUILD",
            epistemicStatus = "OBSERVED",
            confidence = 0.99,
            falsificationCondition = "Fails if APK generation is prevented"
        )

        val record = reconciliationEngine.reconcileFederatedEvidence(anomaly, emptyList())
        assertEquals(ReconciliationOutcome.CONFIRMED, record.outcome)
        assertEquals("TOOLING_OBSERVATORY", record.origin)
        assertEquals("AUTHORIZATION_SYSTEM", record.authorization)
    }

    @Test
    fun testCapabilityGapLogging() {
        val gap = EvidenceOfAction.CapabilityGapRecorded(
            capabilityId = "GAP_POSTGRES_LOCAL_DAEMON",
            requiredCapability = "PostgreSQL 15 physical database container",
            whyRequired = "Physical verification of PostgresLedgerRepository",
            strategiesAttempted = listOf("docker run -p 5432:5432 postgres"),
            authorizedAlternatives = listOf("Hosted PostgreSQL (Supabase/Neon)", "Remote CI runner with service containers"),
            estimatedCost = "Free tier / minimal",
            securityImplications = "Environment credential management",
            recommendedNextAcquisition = "Authorized Hosted PostgreSQL"
        )

        val record = reconciliationEngine.reconcileFederatedEvidence(gap, emptyList())
        assertEquals(ReconciliationOutcome.MERGED, record.outcome)
        assertEquals("CAPABILITY_HARVEST", record.origin)
        assertEquals("AUTHORIZATION_L3", record.authorization)
    }
}
