package com.example.ai.capabilities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NonDestructiveEvolutionPolicyTest {
    @Test
    fun policy_allowsOnlyAdditiveEvolutionModes() {
        val policy = NonDestructiveEvolutionPolicy()

        assertTrue(policy.authorize(NonDestructiveMode.AUGMENT))
        assertTrue(policy.authorize(NonDestructiveMode.ADAPTER))
        assertTrue(policy.authorize(NonDestructiveMode.COMPOSE))
        assertFalse(policy.canReplaceNativeImplementation())
    }

    @Test
    fun catalog_containsEvidenceGatedCandidatesForCoreEvolutionAreas() {
        val candidates = OpenSourceEvolutionCatalog.candidates

        assertTrue(candidates.any { EvolutionFit.DURABILITY in it.fits })
        assertTrue(candidates.any { EvolutionFit.RESILIENCE in it.fits })
        assertTrue(candidates.any { EvolutionFit.OBSERVABILITY in it.fits })
        assertTrue(candidates.all { it.repositoryUrl.startsWith("https://github.com/") })
        assertTrue(candidates.all { it.license != "UNKNOWN" })
    }
}
