package com.artiface.core.network.mapper

import com.artiface.core.model.GenerationStatus
import com.artiface.core.network.dto.GenerationJobDto
import com.artiface.core.network.dto.GenerationResultDto
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GenerationMappersTest {

    @Test
    fun api_status_maps_to_domain() {
        assertThat(mapApiStatus("preparing_image")).isEqualTo(GenerationStatus.PreparingImage)
        assertThat(mapApiStatus("uploading")).isEqualTo(GenerationStatus.Uploading)
        assertThat(mapApiStatus("waiting_for_processing"))
            .isEqualTo(GenerationStatus.WaitingForProcessing)
        assertThat(mapApiStatus("downloading_result"))
            .isEqualTo(GenerationStatus.DownloadingResult)
        assertThat(mapApiStatus("completed")).isEqualTo(GenerationStatus.Completed)
        assertThat(mapApiStatus("failed")).isEqualTo(GenerationStatus.Failed)
    }

    @Test
    fun domain_status_maps_to_api() {
        assertThat(GenerationStatus.Uploading.toApiStatus()).isEqualTo("uploading")
        assertThat(GenerationStatus.Completed.toApiStatus()).isEqualTo("completed")
    }

    @Test
    fun job_dto_maps_to_domain_job() {
        val dto = GenerationJobDto(
            jobId = "job-1",
            status = "waiting_for_processing",
            progress = 0.65f,
            createdAt = "2026-07-18T12:00:00Z",
            updatedAt = "2026-07-18T12:00:03Z",
            errorMessage = null,
            result = null,
        )
        val job = dto.toDomainJob(selfieId = "selfie-9")
        assertThat(job.id).isEqualTo("job-1")
        assertThat(job.selfieId).isEqualTo("selfie-9")
        assertThat(job.status).isEqualTo(GenerationStatus.WaitingForProcessing)
        assertThat(job.progress).isEqualTo(0.65f)
    }

    @Test
    fun result_dto_maps_to_domain_result() {
        val dto = GenerationResultDto(
            resultId = "res-1",
            styleId = "comic_burst",
            title = "The Midnight Schemer",
            expression = "Joyful",
            timeOfDay = "Night",
            broadLocationLabel = "Downtown",
            resultImageUrl = "https://cdn.example/r.jpg",
            createdAt = "2026-07-18T12:00:05Z",
        )
        val result = dto.toDomainResult(
            originalImageUri = "file:///selfie.jpg",
            localGeneratedImageUri = "file:///results/local.jpg",
        )
        assertThat(result.id).isEqualTo("res-1")
        assertThat(result.styleId.value).isEqualTo("comic_burst")
        assertThat(result.title).isEqualTo("The Midnight Schemer")
        assertThat(result.generatedImageUri).isEqualTo("file:///results/local.jpg")
        assertThat(result.broadLocationLabel).isEqualTo("Downtown")
    }
}
