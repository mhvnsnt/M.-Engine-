package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class, StyleProfileEntity::class, EndpointEntity::class, MemoryFragment::class, WorkspaceEntity::class, FileEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun styleDao(): StyleDao
    abstract fun endpointDao(): EndpointDao
    abstract fun memoryFragmentDao(): MemoryFragmentDao
    abstract fun workspaceDao(): WorkspaceDao
}
