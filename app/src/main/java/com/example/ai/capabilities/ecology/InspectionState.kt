package com.example.ai.capabilities.ecology

enum class InspectionLevel {
    LEVEL_0_REGISTRY,
    LEVEL_1_STRUCTURAL,
    LEVEL_2_SEMANTIC,
    LEVEL_3_EXECUTION,
    LEVEL_4_RUNTIME,
    LEVEL_5_CROSS_PROJECT
}

enum class ExecutionInspectionStatus { NOT_ATTEMPTED, IN_PROGRESS, COMPLETE, BLOCKED, UNKNOWN }

data class InspectionState(
    var level0Registry: ExecutionInspectionStatus = ExecutionInspectionStatus.NOT_ATTEMPTED,
    var level1Structural: ExecutionInspectionStatus = ExecutionInspectionStatus.NOT_ATTEMPTED,
    var level2Semantic: ExecutionInspectionStatus = ExecutionInspectionStatus.NOT_ATTEMPTED,
    var level3Execution: ExecutionInspectionStatus = ExecutionInspectionStatus.NOT_ATTEMPTED,
    var level4Runtime: ExecutionInspectionStatus = ExecutionInspectionStatus.NOT_ATTEMPTED,
    var level5CrossProject: ExecutionInspectionStatus = ExecutionInspectionStatus.NOT_ATTEMPTED
)
