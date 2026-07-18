package com.artiface.feature.processing.data

import android.content.Context
import androidx.core.net.toUri
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.database.CaricatureResultDao
import com.artiface.core.database.GenerationJobDao
import com.artiface.core.database.toDomain
import com.artiface.core.database.toEntity
import com.artiface.core.database.toGalleryItem
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.GalleryItem
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleCatalog
import com.artiface.core.model.StyleId
import com.artiface.feature.processing.work.GenerationWorkScheduler
import com.artiface.feature.processing.work.GenerationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed generation repository that enqueues [GenerationWorker] via WorkManager.
 * Survives process death; interrupted in-flight jobs are marked Failed so the UI can retry.
 */
@Singleton
class FakeGenerationRepository @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val resultDao: CaricatureResultDao,
    private val jobDao: GenerationJobDao,
    private val workScheduler: GenerationWorkScheduler,
) : GenerationRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val resultSelfieCache = ConcurrentHashMap<String, String>()

    init {
        scope.launch { recoverInterruptedJobs() }
        scope.launch {
            resultDao.observeAll().collect { entities ->
                entities.forEach { resultSelfieCache[it.id] = it.selfieId }
            }
        }
    }

    private suspend fun recoverInterruptedJobs() {
        val active = jobDao.getActive()
        if (active.isEmpty()) return
        val workManager = WorkManager.getInstance(appContext)
        for (job in active) {
            val infos = runCatching {
                workManager
                    .getWorkInfosForUniqueWork(GenerationWorker.uniqueWorkName(job.id))
                    .get(2, TimeUnit.SECONDS)
            }.getOrDefault(emptyList())
            val runningOrEnqueued = infos.any {
                it.state == WorkInfo.State.RUNNING ||
                    it.state == WorkInfo.State.ENQUEUED ||
                    it.state == WorkInfo.State.BLOCKED
            }
            if (!runningOrEnqueued) {
                jobDao.markTerminal(
                    id = job.id,
                    status = GenerationStatus.Failed.name,
                    progress = job.progress,
                    updatedAtEpochMs = Instant.now().toEpochMilli(),
                    errorMessage = GenerationWorker.INTERRUPTED_MESSAGE,
                    resultId = null,
                )
            }
        }
    }

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
        jobDao.upsert(job.toEntity(styleId = resolvedStyle.id))
        workScheduler.enqueue(job.id, replace = false)
        return job
    }

    override fun observeJob(jobId: String): Flow<GenerationJob?> =
        jobDao.observeById(jobId).map { it?.toDomain() }

    override suspend fun getJob(jobId: String): GenerationJob? =
        jobDao.getById(jobId)?.toDomain()

    override suspend fun getResult(resultId: String): CaricatureResult? {
        val entity = resultDao.getById(resultId) ?: return null
        resultSelfieCache[entity.id] = entity.selfieId
        return entity.toDomain()
    }

    override fun observeResult(resultId: String): Flow<CaricatureResult?> =
        resultDao.observeById(resultId).onEach { entity ->
            if (entity != null) {
                resultSelfieCache[entity.id] = entity.selfieId
            }
        }.map { it?.toDomain() }

    override suspend fun getResultIdForJob(jobId: String): String? =
        jobDao.getById(jobId)?.resultId

    override fun getSelfieIdForResult(resultId: String): String? =
        resultSelfieCache[resultId]

    override suspend fun setFavourite(resultId: String, favourite: Boolean) {
        resultDao.setFavourite(resultId, favourite)
    }

    override suspend fun retry(jobId: String): GenerationJob {
        val existing = jobDao.getById(jobId) ?: error("Unknown job $jobId")
        val now = Instant.now()
        val reset = existing.copy(
            status = GenerationStatus.PreparingImage.name,
            progress = 0f,
            errorMessage = null,
            updatedAtEpochMs = now.toEpochMilli(),
            resultId = null,
        )
        jobDao.upsert(reset)
        workScheduler.enqueue(jobId, replace = true)
        return reset.toDomain()
    }

    override fun observeGalleryItems(): Flow<List<GalleryItem>> =
        resultDao.observeAll().map { entities -> entities.map { it.toGalleryItem() } }

    override suspend fun deleteResult(resultId: String) {
        val entity = resultDao.getById(resultId)
        resultDao.deleteById(resultId)
        resultSelfieCache.remove(resultId)
        entity?.generatedImageUri?.let { deleteLocalFile(it) }
    }

    override suspend fun clearGallery() {
        val uris = resultDao.getAll().map { it.generatedImageUri }
        resultDao.deleteAll()
        resultSelfieCache.clear()
        uris.forEach { deleteLocalFile(it) }
        File(appContext.filesDir, FakeCaricatureGenerator.RESULTS_DIR)
            .takeIf { it.isDirectory }
            ?.listFiles()
            ?.forEach { it.delete() }
    }

    private fun deleteLocalFile(uriString: String) {
        runCatching {
            val uri = uriString.toUri()
            val path = uri.path ?: return
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }
}
