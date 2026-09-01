package com.example.ai.capabilities.ecology

import java.util.UUID

enum class WakeScheduleStatus {
    ON_SCHEDULE, DELAYED, SIGNIFICANTLY_DELAYED, UNKNOWN
}

enum class WakeResult {
    SUCCESS, RETRY, CANCELLED, FAILED, BLOCKED, OFFLINE_PROCESSED
}

data class MetabolismWakeRecord(
    val runId: String = UUID.randomUUID().toString(),
    val scheduledTimestamp: Long?,
    val actualStartTimestamp: Long = System.currentTimeMillis(),
    var actualCompletionTimestamp: Long? = null,
    var durationMs: Long? = null,
    var result: WakeResult? = null,
    val networkAvailable: Boolean,
    var reasonForEarlyExit: String? = null,
    var nextIntendedWorkCategory: String? = null,
    var schedulingJitterMs: Long? = null,
    var scheduleStatus: WakeScheduleStatus = WakeScheduleStatus.UNKNOWN
) {
    fun complete(finalResult: WakeResult, exitReason: String? = null, nextWork: String? = null) {
        this.actualCompletionTimestamp = System.currentTimeMillis()
        this.durationMs = this.actualCompletionTimestamp!! - this.actualStartTimestamp
        this.result = finalResult
        this.reasonForEarlyExit = exitReason
        this.nextIntendedWorkCategory = nextWork
    }
}
