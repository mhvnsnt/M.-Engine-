package com.example.ai.cloud

import kotlinx.coroutines.runBlocking
import java.util.UUID

fun main(args: Array<String>) {
    val isTestMode = args.contains("--test-recovery")
    
    // Phase 2: Environment Configuration
    val dbType = System.getenv("AGENCY_DB_TYPE") ?: "sqlite"
    val dbUrl = System.getenv("AGENCY_DB_URL") ?: "jdbc:sqlite:agency_ledger.db"
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    
    val ledger = if (dbType == "postgres") {
        println("INITIALIZING POSTGRESQL LEDGER REPOSITORY: $dbUrl")
        PostgresLedgerRepository(dbUrl)
    } else {
        println("INITIALIZING SQLITE LEDGER REPOSITORY: $dbUrl")
        SQLiteLedgerRepository(dbUrl)
    }
    
    try {
        ledger.initDatabase()
    } catch (e: Exception) {
        println("Warning: Database initialization failed. If using Postgres, ensure schema is applied. Error: ${e.message}")
    }
    
    val executor = CycleExecutor(ledger)
    
    if (isTestMode) {
        runVerificationTest(ledger, executor)
        return
    }

    // Phase 7: Remote API Scaffold
    println("STARTING OBSERVATORY API ON PORT $port...")
    startKtorServer(port, ledger)

    // Phase 3: Scheduler Lifecycle
    val scheduler = AutonomousMetabolismScheduler(executor)
    runBlocking {
        scheduler.runDaemon()
    }
}

fun runVerificationTest(ledger: AgencyLedgerRepository, executor: CycleExecutor) {
    println("RUNNING PHYSICAL VERIFICATION TEST...")
    
    val crashedCycleId = UUID.randomUUID().toString()
    val runId = UUID.randomUUID().toString()
    ledger.startCycle(crashedCycleId, runId)
    
    println("TEST: Recovering crashed cycle $crashedCycleId...")
    executor.executeCycle(runId, crashedCycleId)
    
    println("TEST: Attempting duplicate execution of $crashedCycleId...")
    executor.executeCycle(runId, crashedCycleId)
    
    println("TEST: Triggering Kill Switch...")
    ledger.setEmergencyStop(true)
    val blockedCycleId = UUID.randomUUID().toString()
    executor.executeCycle(runId, blockedCycleId)
    
    println("\n=== PERSISTENT MINDSTREAM (THE LEDGER) ===")
    ledger.getMindstream().forEach { println(it) }
    
    ledger.setEmergencyStop(false)
}
