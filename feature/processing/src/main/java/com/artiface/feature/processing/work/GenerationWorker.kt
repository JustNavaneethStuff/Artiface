package com.artiface.feature.processing.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.artiface.core.common.generation.CaricatureGenerator
import com.artiface.core.common.generation.ExpressionAnalyzer
import com.artiface.core.common.generation.LocationContextProvider
import com.artiface.core.common.generation.TimeContextProvider
import com.artiface.core.common.selfie.SelfieRepository
import com.artiface.core.database.CaricatureResultDao
import com.artiface.core.database.GenerationJobDao
import com.artiface.core.database.toEntity
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GenerationContext
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleId
import com.artiface.core.preferences.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.Instant

@HiltWorker
class GenerationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val jobDao: GenerationJobDao,
    private val resultDao: CaricatureResultDao,
    private val selfieRepository: SelfieRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val expressionAnalyzer: ExpressionAnalyzer,
    private val timeContextProvider: TimeContextProvider,
    private val locationContextProvider: LocationContextProvider,
    private val caricatureGenerator: CaricatureGenerator,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID) ?: return Result.failure()
        val entity = jobDao.getById(jobId) ?: return Result.failure()

        return try {
            val selfie = selfieRepository.getById(entity.selfieId)
                ?: error("Selfie missing for generation")
            val prefs = preferencesRepository.preferences.first()
            val expression = if (prefs.contextualPersonalizationEnabled) {
                expressionAnalyzer.analyze(selfie.localUri)
            } else {
                ExpressionCategory.Neutral
            }
            val timeOfDay = timeContextProvider.currentTimeOfDay()
            val location = if (prefs.contextualPersonalizationEnabled) {
                locationContextProvider.broadLocationLabel()
            } else {
                null
            }
            val styleId = StyleId(entity.styleId)
            val generationContext = GenerationContext(
                expression = expression,
                timeOfDay = timeOfDay,
                broadLocationLabel = location,
                styleId = styleId,
                contextualPersonalizationEnabled = prefs.contextualPersonalizationEnabled,
            )

            val result = caricatureGenerator.generate(
                jobId = jobId,
                selfieLocalUri = selfie.localUri,
                context = generationContext,
            ) { status, progress ->
                if (isStopped) return@generate
                jobDao.updateProgress(
                    id = jobId,
                    status = status.name,
                    progress = progress,
                    updatedAtEpochMs = Instant.now().toEpochMilli(),
                )
            }

            if (isStopped) {
                markInterrupted(jobId)
                return Result.failure()
            }

            resultDao.upsert(
                result.toEntity(
                    selfieId = entity.selfieId,
                    status = GenerationStatus.Completed,
                ),
            )
            jobDao.markTerminal(
                id = jobId,
                status = GenerationStatus.Completed.name,
                progress = 1f,
                updatedAtEpochMs = Instant.now().toEpochMilli(),
                errorMessage = null,
                resultId = result.id,
            )
            Result.success()
        } catch (t: Throwable) {
            if (isStopped) {
                markInterrupted(jobId)
            } else {
                jobDao.markTerminal(
                    id = jobId,
                    status = GenerationStatus.Failed.name,
                    progress = entity.progress,
                    updatedAtEpochMs = Instant.now().toEpochMilli(),
                    errorMessage = t.message ?: "Generation failed",
                    resultId = null,
                )
            }
            Result.failure()
        }
    }

    private suspend fun markInterrupted(jobId: String) {
        jobDao.markTerminal(
            id = jobId,
            status = GenerationStatus.Failed.name,
            progress = 0f,
            updatedAtEpochMs = Instant.now().toEpochMilli(),
            errorMessage = INTERRUPTED_MESSAGE,
            resultId = null,
        )
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val UNIQUE_WORK_PREFIX = "generation-"
        const val INTERRUPTED_MESSAGE = "Interrupted — tap retry"

        fun uniqueWorkName(jobId: String): String = "$UNIQUE_WORK_PREFIX$jobId"
    }
}
