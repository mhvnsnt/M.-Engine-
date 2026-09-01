package com.example.ai.capabilities.federated

import com.example.ai.capabilities.federated.provider.OpenHandsClient

/**
 * OpenHands external adapter.
 *
 * HISTORY, kept deliberately: every method of this class used to be a `delay()`
 * followed by a hardcoded return. `retrieveTestOutput` returned the literal
 * string "BUILD SUCCESSFUL in 2s\n1 test completed, 0 failed" — a fabricated CI
 * result, which REALITY_CONTRACT.md names explicitly as forbidden. It also
 * returned a fake git diff for a file called DummyTest.kt, and dispatched
 * against endpoints (/sandbox/provision, /sandbox/{id}/execute) that do not
 * exist in OpenHands at all.
 *
 * Nothing referenced this class, so the fabrication never surfaced. That is
 * precisely why it was dangerous: the first caller to arrive would have received
 * a green test result for work that never ran.
 *
 * The simulation is now gone. Every method delegates to the real HTTP client and
 * fails loudly when the runtime is absent. There is no path through this class
 * that invents a result.
 */
class OpenHandsWorkerAdapter(
    private val client: OpenHandsClient = OpenHandsClient(),
) {

    /**
     * Thrown when the OpenHands runtime is not reachable.
     *
     * REALITY_CONTRACT.md §2-3: implement the real integration boundary, then
     * stop at the actual missing dependency rather than substituting a
     * simulation. This exception IS that stop.
     */
    class CapabilityGapException(message: String) : Exception(message)

    private suspend fun requireRuntime() {
        if (!client.checkHealth()) {
            throw CapabilityGapException(
                "BLOCKED_BY_EXTERNAL_DEPENDENCY: no OpenHands runtime is reachable. " +
                    "Start OpenHands and point the Capability Fabric at it.",
            )
        }
    }

    /** Starts a real OpenHands conversation. Returns its conversation id. */
    suspend fun startConversation(
        authorization: CodingTrialAuthorization,
        instruction: String,
    ): String {
        requireRuntime()
        return client.startConversation(
            instruction = instruction,
            repository = authorization.repositoryId,
        )
    }

    /** Reads the real execution status of a conversation. */
    suspend fun conversationStatus(conversationId: String): String {
        requireRuntime()
        return client.conversationStatus(conversationId)
    }

    /**
     * Test output is EVIDENCE. There is no local source for it — it comes from
     * the sandbox that actually ran the tests, or it does not exist.
     *
     * This method previously returned a hardcoded pass. It now returns whatever
     * the runtime reports, and throws when there is no runtime.
     */
    suspend fun retrieveConversationEvents(conversationId: String): String {
        requireRuntime()
        return client.conversationEvents(conversationId)
    }
}
