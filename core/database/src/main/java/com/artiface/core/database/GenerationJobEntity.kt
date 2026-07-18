package com.artiface.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleId
import java.time.Instant

@Entity(tableName = "generation_jobs")
data class GenerationJobEntity(
    @PrimaryKey val id: String,
    val selfieId: String,
    val styleId: String,
    val status: String,
    val progress: Float,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val errorMessage: String?,
    val resultId: String?,
)

fun GenerationJobEntity.toDomain(): GenerationJob =
    GenerationJob(
        id = id,
        selfieId = selfieId,
        status = runCatching { GenerationStatus.valueOf(status) }
            .getOrDefault(GenerationStatus.Failed),
        progress = progress,
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMs),
        errorMessage = errorMessage,
    )

fun GenerationJob.toEntity(
    styleId: StyleId,
    resultId: String? = null,
): GenerationJobEntity =
    GenerationJobEntity(
        id = id,
        selfieId = selfieId,
        styleId = styleId.value,
        status = status.name,
        progress = progress,
        createdAtEpochMs = createdAt.toEpochMilli(),
        updatedAtEpochMs = updatedAt.toEpochMilli(),
        errorMessage = errorMessage,
        resultId = resultId,
    )

fun GenerationJobEntity.styleIdValue(): StyleId = StyleId(styleId)
