package com.artiface.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CaricatureResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class ArtifaceDatabase : RoomDatabase() {
    abstract fun caricatureResultDao(): CaricatureResultDao
}
