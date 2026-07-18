package com.artiface.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CaricatureResultEntity::class,
        GenerationJobEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class ArtifaceDatabase : RoomDatabase() {
    abstract fun caricatureResultDao(): CaricatureResultDao
    abstract fun generationJobDao(): GenerationJobDao
}
