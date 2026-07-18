package com.artiface.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GenerationJobDao {

    @Query("SELECT * FROM generation_jobs WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<GenerationJobEntity?>

    @Query("SELECT * FROM generation_jobs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): GenerationJobEntity?

    @Query(
        """
        SELECT * FROM generation_jobs
        WHERE status NOT IN ('Completed', 'Failed')
        ORDER BY updatedAtEpochMs DESC
        """,
    )
    suspend fun getActive(): List<GenerationJobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: GenerationJobEntity)

    @Query(
        """
        UPDATE generation_jobs
        SET status = :status,
            progress = :progress,
            updatedAtEpochMs = :updatedAtEpochMs,
            errorMessage = :errorMessage
        WHERE id = :id
        """,
    )
    suspend fun updateProgress(
        id: String,
        status: String,
        progress: Float,
        updatedAtEpochMs: Long,
        errorMessage: String? = null,
    )

    @Query(
        """
        UPDATE generation_jobs
        SET status = :status,
            progress = :progress,
            updatedAtEpochMs = :updatedAtEpochMs,
            errorMessage = :errorMessage,
            resultId = :resultId
        WHERE id = :id
        """,
    )
    suspend fun markTerminal(
        id: String,
        status: String,
        progress: Float,
        updatedAtEpochMs: Long,
        errorMessage: String?,
        resultId: String?,
    )

    @Query("DELETE FROM generation_jobs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM generation_jobs")
    suspend fun deleteAll()
}
