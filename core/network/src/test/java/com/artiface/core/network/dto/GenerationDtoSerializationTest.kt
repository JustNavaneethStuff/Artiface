package com.artiface.core.network.dto

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

class GenerationDtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun job_dto_deserializes_completed_payload() {
        val payload = """
            {
              "job_id": "job_abc",
              "status": "completed",
              "progress": 1.0,
              "created_at": "2026-07-18T12:00:00Z",
              "updated_at": "2026-07-18T12:00:05Z",
              "error_message": null,
              "result": {
                "result_id": "res_123",
                "style_id": "neon_mischief",
                "title": "Agent of Neon Mischief",
                "expression": "Mischievous",
                "time_of_day": "Evening",
                "broad_location_label": null,
                "result_image_url": "https://cdn.example/results/res_123.jpg",
                "created_at": "2026-07-18T12:00:05Z"
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString(GenerationJobDto.serializer(), payload)
        assertThat(dto.jobId).isEqualTo("job_abc")
        assertThat(dto.status).isEqualTo("completed")
        assertThat(dto.result).isNotNull()
        assertThat(dto.result!!.resultId).isEqualTo("res_123")
        assertThat(dto.result!!.styleId).isEqualTo("neon_mischief")
    }
}
