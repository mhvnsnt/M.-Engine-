package com.example.ai.capabilities

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SecurityScannerRegressionTest {

    private lateinit var scanner: SecurityScanner

    @Before
    fun setUp() {
        scanner = SecurityScannerImpl()
    }

    @Test
    fun testSecurityScanner_BlocksAnthropicKeyLeak() = runBlocking {
        val patch = """
            + val apiKey = "sk-ant-api03-abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890"
        """.trimIndent()

        val result = scanner.scanPatch(patch)
        assertFalse("Patch with hardcoded Anthropic key must be blocked", result.passed)
        assertTrue(result.violations.any { it.reason.contains("Anthropic API Key") && it.severity == "CRITICAL" })
    }

    @Test
    fun testSecurityScanner_BlocksGoogleKeyLeak() = runBlocking {
        val patch = """
            + val mapsKey = "AIzaSyD-1234567890abcdefghijklmnopqrstuv"
        """.trimIndent()

        val result = scanner.scanPatch(patch)
        assertFalse("Patch with Google API key must be blocked", result.passed)
        assertTrue(result.violations.any { it.reason.contains("Google API Key") && it.severity == "CRITICAL" })
    }

    @Test
    fun testSecurityScanner_BlocksGitHubPersonalAccessToken() = runBlocking {
        val patch = """
            + val token = "ghp_abcdefghijklmnopqrstuvwxyz1234567890"
        """.trimIndent()

        val result = scanner.scanPatch(patch)
        assertFalse("Patch with GitHub PAT must be blocked", result.passed)
        assertTrue(result.violations.any { it.reason.contains("GitHub Personal Access Token") })
    }

    @Test
    fun testSecurityScanner_DetectsCommandInjectionVulnerability() = runBlocking {
        val vulnerableCode = """
            fun runUnsafe(input: String) {
                Runtime.getRuntime().exec("sh -c " + input)
            }
        """.trimIndent()

        val violations = scanner.checkFileContent("UnsafeRunner.kt", vulnerableCode)
        assertTrue("Must detect command injection heuristic", violations.any { it.reason.contains("Command Injection") })
    }

    @Test
    fun testSecurityScanner_PassesCleanCode() = runBlocking {
        val cleanCode = """
            package com.example.service
            
            class SafeService(private val apiKeyProvider: () -> String) {
                fun getStatus(): String = "OK"
            }
        """.trimIndent()

        val violations = scanner.checkFileContent("SafeService.kt", cleanCode)
        assertTrue("Clean code without credentials or injection must pass", violations.isEmpty())

        val patchResult = scanner.scanPatch(cleanCode)
        assertTrue(patchResult.passed)
        assertEquals(0, patchResult.violations.size)
    }
}
