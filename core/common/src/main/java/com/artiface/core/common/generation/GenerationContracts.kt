package com.artiface.core.common.generation

import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.StyleId
import kotlinx.coroutines.flow.Flow

interface GenerationRepository {
    suspend fun startGeneration(selfieId: String, styleId: StyleId): GenerationJob

    fun observeJob(jobId: String): Flow<GenerationJob?>

    suspend fun getJob(jobId: String): GenerationJob?

    suspend fun getResult(resultId: String): CaricatureResult?

    fun observeResult(resultId: String): Flow<CaricatureResult?>

    suspend fun getResultIdForJob(jobId: String): String?

    fun getSelfieIdForResult(resultId: String): String?

    suspend fun setFavourite(resultId: String, favourite: Boolean)

    suspend fun retry(jobId: String): GenerationJob
}

interface ExpressionAnalyzer {
    suspend fun analyze(selfieLocalUri: String): ExpressionCategory
}

interface TimeContextProvider {
    fun currentTimeOfDay(): com.artiface.core.model.TimeOfDay
}

interface LocationContextProvider {
    /**
     * Returns a broad, user-friendly label only when consent/prefs allow it.
     * Never returns precise GPS coordinates.
     */
    suspend fun broadLocationLabel(): String?
}

interface CaricatureGenerator {
    suspend fun generate(
        jobId: String,
        selfieLocalUri: String,
        context: com.artiface.core.model.GenerationContext,
        onProgress: suspend (status: com.artiface.core.model.GenerationStatus, progress: Float) -> Unit,
    ): CaricatureResult
}
