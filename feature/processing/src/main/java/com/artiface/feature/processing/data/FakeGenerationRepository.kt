package com.artiface.feature.processing.data

import com.artiface.core.common.generation.CaricatureGenerator
import com.artiface.core.common.generation.ExpressionAnalyzer
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.common.generation.LocationContextProvider
import com.artiface.core.common.generation.TimeContextProvider
import com.artiface.core.common.selfie.SelfieRepository
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GenerationContext
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleCatalog
import com.artiface.core.model.StyleId
import com.artiface.core.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeGenerationRepository @Inject constructor(
    private val selfieRepository: SelfieRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val expressionAnalyzer: ExpressionAnalyzer,
    private val timeContextProvider: TimeContextProvider,
    private val locationContextProvider: LocationContextProvider,
    private val caricatureGenerator: CaricatureGenerator,
) : GenerationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val jobs = ConcurrentHashMap<String, GenerationJob>()
    private val results = ConcurrentHashMap<String, CaricatureResult>()
    private val jobToResult = ConcurrentHashMap<String, String>()
    private val resultToSelfie = ConcurrentHashMap<String, String>()
    private val jobToStyle = ConcurrentHashMap<String, StyleId>()
    private val version = MutableStateFlow(0L)

    override suspend fun startGeneration(selfieId: String, styleId: StyleId): GenerationJob {
        val resolvedStyle = StyleCatalog.resolveSelection(styleId)
        val now = Instant.now()
        val job = GenerationJob(
            id = UUID.randomUUID().toString(),
            selfieId = selfieId,
            status = GenerationStatus.PreparingImage,
            progress = 0f,
            createdAt = now,
            updatedAt = now,
        )
        mutex.withLock {
            jobs[job.id] = job
            jobToStyle[job.id] = resolvedStyle.id
            bump()
        }
        scope.launch { runJob(job.id, resolvedStyle.id) }
        return job
    }

    override fun observeJob(jobId: String): Flow<GenerationJob?> =
        version.map { jobs[jobId] }

    override suspend fun getJob(jobId: String): GenerationJob? = jobs[jobId]

    override suspend fun getResult(resultId: String): CaricatureResult? = results[resultId]

    override fun observeResult(resultId: String): Flow<CaricatureResult?> =
        version.map { results[resultId] }

    override suspend fun getResultIdForJob(jobId: String): String? = jobToResult[jobId]

    override fun getSelfieIdForResult(resultId: String): String? = resultToSelfie[resultId]

    override suspend fun setFavourite(resultId: String, favourite: Boolean) {
        mutex.withLock {
            val current = results[resultId] ?: return
            results[resultId] = current.copy(isFavourite = favourite)
            bump()
        }
    }

    override suspend fun retry(jobId: String): GenerationJob {
        val existing = jobs[jobId] ?: error("Unknown job $jobId")
        val styleId = jobToStyle[jobId] ?: StyleCatalog.ComicBurst.id
        mutex.withLock {
            jobs[jobId] = existing.copy(
                status = GenerationStatus.PreparingImage,
                progress = 0f,
                errorMessage = null,
                updatedAt = Instant.now(),
            )
            bump()
        }
        scope.launch { runJob(jobId, styleId) }
        return jobs.getValue(jobId)
    }

    private suspend fun runJob(jobId: String, styleId: StyleId) {
        val job = jobs[jobId] ?: return
        try {
            val selfie = selfieRepository.getById(job.selfieId)
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
                updateJob(jobId) {
                    it.copy(status = status, progress = progress, updatedAt = Instant.now())
                }
            }

            mutex.withLock {
                results[result.id] = result
                jobToResult[jobId] = result.id
                resultToSelfie[result.id] = job.selfieId
                jobs[jobId] = jobs.getValue(jobId).copy(
                    status = GenerationStatus.Completed,
                    progress = 1f,
                    updatedAt = Instant.now(),
                    errorMessage = null,
                )
                bump()
            }
        } catch (t: Throwable) {
            updateJob(jobId) {
                it.copy(
                    status = GenerationStatus.Failed,
                    errorMessage = t.message ?: "Generation failed",
                    updatedAt = Instant.now(),
                )
            }
        }
    }

    private suspend fun updateJob(jobId: String, transform: (GenerationJob) -> GenerationJob) {
        mutex.withLock {
            val current = jobs[jobId] ?: return
            jobs[jobId] = transform(current)
            bump()
        }
    }

    private fun bump() {
        version.update { it + 1 }
    }
}
