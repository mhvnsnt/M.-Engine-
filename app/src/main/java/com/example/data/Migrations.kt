package com.example.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Schema migrations.
 *
 * The application previously relied on `fallbackToDestructiveMigration(true)`,
 * which means every schema version bump DELETED the entire encrypted database —
 * all conversation history with it. That is incompatible with a Level 0 record
 * whose defining property is that it survives.
 *
 * Migrations are declared here so a version bump preserves owner data. The
 * destructive fallback is retained only as a last resort for a database whose
 * version predates any declared migration path.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Purely additive: creates the canonical event table and its indices.
        // No existing table is touched, so no existing row can be lost.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `conversation_events` (
                `eventId` TEXT NOT NULL,
                `timestamp` INTEGER NOT NULL,
                `actor` TEXT NOT NULL,
                `rawContent` TEXT NOT NULL,
                `sourcePlatform` TEXT NOT NULL,
                `conversationId` TEXT NOT NULL,
                `referencedArtifacts` TEXT NOT NULL,
                `supersededByEventId` TEXT,
                `migratedFrom` TEXT,
                PRIMARY KEY(`eventId`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_events_timestamp` ON `conversation_events` (`timestamp`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_events_conversationId` ON `conversation_events` (`conversationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_conversation_events_actor` ON `conversation_events` (`actor`)")
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Additive: owner context becomes data instead of Kotlin literals.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `owner_goals` (
                `id` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `timeHorizon` TEXT NOT NULL,
                `priority` INTEGER NOT NULL,
                `provenance` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `supersededBy` TEXT,
                PRIMARY KEY(`id`)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `terminology_preferences` (
                `rejectedTerm` TEXT NOT NULL,
                `preferredTerm` TEXT NOT NULL,
                `context` TEXT NOT NULL,
                `provenance` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `supersededBy` TEXT,
                PRIMARY KEY(`rejectedTerm`)
            )
            """.trimIndent(),
        )
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Additive: Project authority (Level 2), the Library artifact graph, and
        // worker job records. Nothing existing is touched.
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `projects` (
                `id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL,
                `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`))
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `project_associations` (
                `projectId` TEXT NOT NULL, `kind` TEXT NOT NULL, `refId` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`projectId`, `kind`, `refId`))
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_associations_projectId` ON `project_associations` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_associations_kind` ON `project_associations` (`kind`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_associations_refId` ON `project_associations` (`refId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `project_memory` (
                `id` TEXT NOT NULL, `projectId` TEXT NOT NULL, `kind` TEXT NOT NULL,
                `statement` TEXT NOT NULL, `provenance` TEXT NOT NULL,
                `sourceEventIds` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `supersededBy` TEXT, PRIMARY KEY(`id`))
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_memory_projectId` ON `project_memory` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_memory_kind` ON `project_memory` (`kind`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `artifacts` (
                `id` TEXT NOT NULL, `contentHash` TEXT NOT NULL, `kind` TEXT NOT NULL,
                `name` TEXT NOT NULL, `uri` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL,
                `projectId` TEXT, `conversationId` TEXT, `jobId` TEXT,
                `provenance` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                `supersededBy` TEXT, PRIMARY KEY(`id`))
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_artifacts_contentHash` ON `artifacts` (`contentHash`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_artifacts_projectId` ON `artifacts` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_artifacts_conversationId` ON `artifacts` (`conversationId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_artifacts_jobId` ON `artifacts` (`jobId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `worker_jobs` (
                `id` TEXT NOT NULL, `projectId` TEXT, `conversationId` TEXT,
                `capabilityType` TEXT NOT NULL, `providerId` TEXT NOT NULL,
                `objective` TEXT NOT NULL, `status` TEXT NOT NULL,
                `reportedResult` TEXT, `verifiedResult` TEXT,
                `startedAt` INTEGER NOT NULL, `finishedAt` INTEGER,
                PRIMARY KEY(`id`))
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_jobs_projectId` ON `worker_jobs` (`projectId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_jobs_status` ON `worker_jobs` (`status`)")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
