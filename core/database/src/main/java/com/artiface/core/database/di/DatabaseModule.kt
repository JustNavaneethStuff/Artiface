package com.artiface.core.database.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Database wiring shell for Phase 1.
 * Room database, DAOs, and migrations arrive in Phase 5.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
