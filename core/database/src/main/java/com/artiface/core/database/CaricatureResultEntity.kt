package com.artiface.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleId
import com.artiface.core.model.TimeOfDay
import java.time.Instant

@Entity(tableName = "caricature_results")
data class CaricatureResultEntity(
    @PrimaryKey val id: String,
    val selfieId: String,
    val originalImageUri: String,
    val generatedImageUri: String,
    val styleId: String,
    val title: String,
    val expression: String,
    val timeOfDay: String,
    val broadLocationLabel: String?,
    val createdAtEpochMs: Long,
    val isFavourite: Boolean,
    val status: String,
)

fun CaricatureResultEntity.toDomain(): CaricatureResult =
    CaricatureResult(
        id = id,
        originalImageUri = originalImageUri,
        generatedImageUri = generatedImageUri,
        styleId = StyleId(styleId),
        title = title,
        expression = ExpressionCategory.valueOf(expression),
        timeOfDay = TimeOfDay.valueOf(timeOfDay),
        broadLocationLabel = broadLocationLabel,
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        isFavourite = isFavourite,
    )

fun CaricatureResult.toEntity(
    selfieId: String,
    status: GenerationStatus = GenerationStatus.Completed,
): CaricatureResultEntity =
    CaricatureResultEntity(
        id = id,
        selfieId = selfieId,
        originalImageUri = originalImageUri,
        generatedImageUri = generatedImageUri,
        styleId = styleId.value,
        title = title,
        expression = expression.name,
        timeOfDay = timeOfDay.name,
        broadLocationLabel = broadLocationLabel,
        createdAtEpochMs = createdAt.toEpochMilli(),
        isFavourite = isFavourite,
        status = status.name,
    )

fun CaricatureResultEntity.toGalleryItem(): com.artiface.core.model.GalleryItem =
    com.artiface.core.model.GalleryItem(
        result = toDomain(),
        selfieId = selfieId,
        status = runCatching { GenerationStatus.valueOf(status) }
            .getOrDefault(GenerationStatus.Completed),
    )
