package com.artiface.core.network.mapper

import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleId
import com.artiface.core.model.TimeOfDay
import com.artiface.core.network.dto.GenerationJobDto
import com.artiface.core.network.dto.GenerationResultDto
import java.time.Instant

fun mapApiStatus(status: String): GenerationStatus = when (status.lowercase()) {
    "preparing_image" -> GenerationStatus.PreparingImage
    "uploading" -> GenerationStatus.Uploading
    "waiting_for_processing" -> GenerationStatus.WaitingForProcessing
    "downloading_result" -> GenerationStatus.DownloadingResult
    "completed" -> GenerationStatus.Completed
    "failed" -> GenerationStatus.Failed
    else -> GenerationStatus.WaitingForProcessing
}

fun GenerationStatus.toApiStatus(): String = when (this) {
    GenerationStatus.PreparingImage -> "preparing_image"
    GenerationStatus.Uploading -> "uploading"
    GenerationStatus.WaitingForProcessing -> "waiting_for_processing"
    GenerationStatus.DownloadingResult -> "downloading_result"
    GenerationStatus.Completed -> "completed"
    GenerationStatus.Failed -> "failed"
}

fun GenerationJobDto.toDomainJob(selfieId: String): GenerationJob =
    GenerationJob(
        id = jobId,
        selfieId = selfieId,
        status = mapApiStatus(status),
        progress = progress.coerceIn(0f, 1f),
        createdAt = parseInstant(createdAt),
        updatedAt = parseInstant(updatedAt),
        errorMessage = errorMessage,
    )

fun GenerationResultDto.toDomainResult(
    originalImageUri: String,
    localGeneratedImageUri: String,
): CaricatureResult =
    CaricatureResult(
        id = resultId,
        originalImageUri = originalImageUri,
        generatedImageUri = localGeneratedImageUri,
        styleId = StyleId(styleId),
        title = title,
        expression = runCatching { ExpressionCategory.valueOf(expression) }
            .getOrDefault(ExpressionCategory.Unknown),
        timeOfDay = runCatching { TimeOfDay.valueOf(timeOfDay) }
            .getOrDefault(TimeOfDay.Afternoon),
        broadLocationLabel = broadLocationLabel,
        createdAt = parseInstant(createdAt),
        isFavourite = false,
    )

internal fun parseInstant(value: String): Instant =
    runCatching { Instant.parse(value) }.getOrElse { Instant.now() }
