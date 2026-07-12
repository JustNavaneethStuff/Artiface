package com.artiface.core.model

import java.time.Instant

/**
 * A photo captured by the camera feature and stored in app-scoped storage.
 */
data class CapturedSelfie(
    val id: String,
    val localUri: String,
    val capturedAt: Instant,
    val width: Int,
    val height: Int,
    val orientation: Int,
)

data class CaricatureStyle(
    val id: StyleId,
    val name: String,
    val description: String,
    val previewResource: String?,
    val enabled: Boolean = true,
)

@JvmInline
value class StyleId(val value: String)

enum class ExpressionCategory {
    Joyful,
    Serious,
    Surprised,
    Tired,
    Mischievous,
    Neutral,
    Unknown,
}

enum class TimeOfDay {
    Morning,
    Afternoon,
    Evening,
    Night,
}

/**
 * Contextual signals that may influence generation.
 * Broad location is a user-friendly label only — never precise GPS.
 */
data class GenerationContext(
    val expression: ExpressionCategory,
    val timeOfDay: TimeOfDay,
    val broadLocationLabel: String?,
    val styleId: StyleId,
    val contextualPersonalizationEnabled: Boolean,
)

enum class GenerationStatus {
    PreparingImage,
    Uploading,
    WaitingForProcessing,
    DownloadingResult,
    Completed,
    Failed,
}

data class GenerationJob(
    val id: String,
    val selfieId: String,
    val status: GenerationStatus,
    val progress: Float,
    val createdAt: Instant,
    val updatedAt: Instant,
    val errorMessage: String? = null,
)

data class CaricatureResult(
    val id: String,
    val originalImageUri: String,
    val generatedImageUri: String,
    val styleId: StyleId,
    val title: String,
    val expression: ExpressionCategory,
    val timeOfDay: TimeOfDay,
    val broadLocationLabel: String?,
    val createdAt: Instant,
    val isFavourite: Boolean = false,
)

enum class AppThemeMode {
    System,
    Light,
    Dark,
}

/**
 * User-facing preferences persisted via DataStore.
 * Contextual personalization and location are opt-in.
 */
data class UserPreferences(
    val hasCompletedOnboarding: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.System,
    val contextualPersonalizationEnabled: Boolean = false,
    val locationContextEnabled: Boolean = false,
)
