package com.artiface.feature.processing.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.GalleryItem
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleCatalog
import com.artiface.core.model.StyleId
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class StyleSelectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun continue_starts_generation_and_navigates() = runTest {
        val repo = FakeGenerationRepositoryForUi()
        val viewModel = StyleSelectionViewModel(
            SavedStateHandle(mapOf("selfieId" to "selfie-1")),
            repo,
        )
        viewModel.onEvent(StyleSelectionEvent.StyleClicked(StyleCatalog.NeonMischief.id))

        viewModel.effects.test {
            viewModel.onEvent(StyleSelectionEvent.Continue)
            advanceUntilIdle()
            val effect = awaitItem() as StyleSelectionEffect.NavigateToProcessing
            assertThat(effect.jobId).isEqualTo("job-1")
            assertThat(repo.lastStyleId).isEqualTo(StyleCatalog.NeonMischief.id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private class FakeGenerationRepositoryForUi : GenerationRepository {
    var lastStyleId: StyleId? = null
    private val jobs = MutableStateFlow<Map<String, GenerationJob>>(emptyMap())

    override suspend fun startGeneration(selfieId: String, styleId: StyleId): GenerationJob {
        lastStyleId = styleId
        val job = GenerationJob(
            id = "job-1",
            selfieId = selfieId,
            status = GenerationStatus.PreparingImage,
            progress = 0f,
            createdAt = Instant.parse("2026-07-18T01:00:00Z"),
            updatedAt = Instant.parse("2026-07-18T01:00:00Z"),
        )
        jobs.value = mapOf(job.id to job)
        return job
    }

    override fun observeJob(jobId: String): Flow<GenerationJob?> = jobs.map { it[jobId] }
    override suspend fun getJob(jobId: String): GenerationJob? = jobs.value[jobId]
    override suspend fun getResult(resultId: String): CaricatureResult? = null
    override fun observeResult(resultId: String): Flow<CaricatureResult?> =
        MutableStateFlow(null)
    override suspend fun getResultIdForJob(jobId: String): String? = null
    override fun getSelfieIdForResult(resultId: String): String? = null
    override suspend fun setFavourite(resultId: String, favourite: Boolean) = Unit
    override suspend fun retry(jobId: String): GenerationJob = error("unused")
    override fun observeGalleryItems(): Flow<List<GalleryItem>> = MutableStateFlow(emptyList())
    override suspend fun deleteResult(resultId: String) = Unit
    override suspend fun clearGallery() = Unit
}
