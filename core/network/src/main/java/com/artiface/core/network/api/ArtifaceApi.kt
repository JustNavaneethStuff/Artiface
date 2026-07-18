package com.artiface.core.network.api

import com.artiface.core.network.dto.GenerationJobDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/**
 * Retrofit contract matching [docs/BACKEND_API.md].
 */
interface ArtifaceApi {

    @Multipart
    @POST("api/v1/generations")
    suspend fun createGeneration(
        @Part image: MultipartBody.Part,
        @Part("style_id") styleId: RequestBody,
        @Part("expression") expression: RequestBody,
        @Part("time_of_day") timeOfDay: RequestBody,
        @Part("broad_location_label") broadLocationLabel: RequestBody?,
        @Part("client_job_id") clientJobId: RequestBody?,
    ): GenerationJobDto

    @GET("api/v1/generations/{jobId}")
    suspend fun getGeneration(
        @Path("jobId") jobId: String,
    ): GenerationJobDto

    @POST("api/v1/generations/{jobId}/retry")
    suspend fun retryGeneration(
        @Path("jobId") jobId: String,
    ): GenerationJobDto

    @DELETE("api/v1/generations/{jobId}")
    suspend fun deleteGeneration(
        @Path("jobId") jobId: String,
    )
}
