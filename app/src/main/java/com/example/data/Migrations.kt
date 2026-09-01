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

val ALL_MIGRATIONS = arrayOf(MIGRATION_10_11, MIGRATION_11_12)
