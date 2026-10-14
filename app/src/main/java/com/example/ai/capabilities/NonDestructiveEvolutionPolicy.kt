package com.example.ai.capabilities

/**
 * Governs how externally discovered capabilities may evolve M. Engine.
 *
 * The default posture is additive: preserve the native implementation and
 * introduce external capabilities behind an adapter, composition boundary, or
 * optional augmentation. Destructive replacement is not an autonomous option.
 */
class NonDestructiveEvolutionPolicy(
    private val allowDestructiveReplacement: Boolean = false
) {
    fun authorize(mode: NonDestructiveMode): Boolean = when (mode) {
        NonDestructiveMode.AUGMENT,
        NonDestructiveMode.ADAPTER,
        NonDestructiveMode.COMPOSE -> true
    }

    fun canReplaceNativeImplementation(): Boolean = allowDestructiveReplacement

    fun selectMode(candidate: OpenSourceCapabilityCandidate): NonDestructiveMode =
        candidate.preferredMode
}
