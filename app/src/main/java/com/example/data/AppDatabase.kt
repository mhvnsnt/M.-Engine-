package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class, StyleProfileEntity::class, EndpointEntity::class, MemoryFragment::class, WorkspaceEntity::class, FileEntity::class, SessionEntity::class, UserConstraints::class, LocationSnapshot::class, RegionProfile::class, AstroProfile::class, GraphNode::class, CapabilityKnowledgeEntity::class, JobEntity::class, MissionEntity::class, CapabilityStateEntity::class], version = 10, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun sessionDao(): SessionDao
    abstract fun styleDao(): StyleDao
    abstract fun endpointDao(): EndpointDao
    abstract fun memoryFragmentDao(): MemoryFragmentDao
    abstract fun workspaceDao(): WorkspaceDao
    abstract fun locationDao(): LocationDao
    abstract fun astroDao(): AstroDao
    abstract fun graphNodeDao(): GraphNodeDao
    abstract fun capabilityKnowledgeDao(): CapabilityKnowledgeDao
    abstract fun jobDao(): JobDao
    abstract fun missionDao(): MissionDao
    abstract fun capabilityStateDao(): CapabilityStateDao
}
