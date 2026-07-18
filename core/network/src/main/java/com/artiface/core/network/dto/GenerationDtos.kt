package com.artiface.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerationJobDto(
    @SerialName("job_id") val jobId: String,
    @SerialName("status") val status: String,
    @SerialName("progress") val progress: Float = 0f,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("error_message") val errorMessage: String? = null,
    @SerialName("result") val result: GenerationResultDto? = null,
)

@Serializable
data class GenerationResultDto(
    @SerialName("result_id") val resultId: String,
    @SerialName("style_id") val styleId: String,
    @SerialName("title") val title: String,
    @SerialName("expression") val expression: String,
    @SerialName("time_of_day") val timeOfDay: String,
    @SerialName("broad_location_label") val broadLocationLabel: String? = null,
    @SerialName("result_image_url") val resultImageUrl: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ApiErrorDto(
    @SerialName("error_code") val errorCode: String? = null,
    @SerialName("message") val message: String? = null,
)
