package com.artiface.feature.processing.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.GalleryItem
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleId
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ProcessingViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var store: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = ViewModelStore()
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun completed_job_navigates_to_result() = runTest {
        val job = sampleJob(GenerationStatus.Completed)
        val repo = FakeProcessingGenerationRepository(
            initialJob = job,
            resultIdForJob = "result-1",
        )
        val viewModel = createViewModel(job.id, repo)

        viewModel.effects.test {
            runCurrent()
            assertThat(awaitItem()).isEqualTo(ProcessingEffect.NavigateToResult("result-1"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retry_invokes_repository() = runTest {
        val job = sampleJob(GenerationStatus.Failed, error = "Interrupted — tap retry")
        val repo = FakeProcessingGenerationRepository(initialJob = job)
        val viewModel = createViewModel(job.id, repo)

        viewModel.uiState.test {
            runCurrent()
            val failedState = expectMostRecentItem()
            assertThat(failedState.isFailed).isTrue()

            viewModel.onEvent(ProcessingEvent.Retry)
            runCurrent()

            assertThat(repo.retryCalls).isEqualTo(1)
            val afterRetry = expectMostRecentItem()
            assertThat(afterRetry.isFailed).isFalse()
            assertThat(afterRetry.job?.status).isEqualTo(GenerationStatus.PreparingImage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun failed_job_exposes_error_message() = runTest {
        val job = sampleJob(GenerationStatus.Failed, error = "Interrupted — tap retry")
        val viewModel = createViewModel(
            job.id,
            FakeProcessingGenerationRepository(initialJob = job),
        )

        viewModel.uiState.test {
            runCurrent()
            val state = expectMostRecentItem()
            assertThat(state.isFailed).isTrue()
            assertThat(state.errorMessage).isEqualTo("Interrupted — tap retry")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        jobId: String,
        repo: GenerationRepository,
    ): ProcessingViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProcessingViewModel(
                    SavedStateHandle(mapOf("jobId" to jobId)),
                    repo,
                ) as T
            }
        }
        return ViewModelProvider(store, factory)[ProcessingViewModel::class.java]
    }

    private fun sampleJob(
        status: GenerationStatus,
        error: String? = null,
    ) = GenerationJob(
        id = "job-1",
        selfieId = "selfie-1",
        status = status,
        progress = if (status == GenerationStatus.Completed) 1f else 0.4f,
        createdAt = Instant.parse("2026-07-18T01:00:00Z"),
        updatedAt = Instant.parse("2026-07-18T01:00:00Z"),
        errorMessage = error,
    )
}

private class FakeProcessingGenerationRepository(
    initialJob: GenerationJob,
    private val resultIdForJob: String? = null,
) : GenerationRepository {
    private val jobs = MutableStateFlow(mapOf(initialJob.id to initialJob))
    var retryCalls: Int = 0

    override fun observeJob(jobId: String): Flow<GenerationJob?> = jobs.map { it[jobId] }
    override suspend fun getJob(jobId: String): GenerationJob? = jobs.value[jobId]
    override suspend fun getResultIdForJob(jobId: String): String? = resultIdForJob

    override suspend fun retry(jobId: String): GenerationJob {
        retryCalls += 1
        val current = jobs.value.getValue(jobId)
        val reset = current.copy(
            status = GenerationStatus.PreparingImage,
            progress = 0f,
            errorMessage = null,
        )
        jobs.value = jobs.value + (jobId to reset)
        return reset
    }

    override suspend fun startGeneration(selfieId: String, styleId: StyleId): GenerationJob =
        error("unused")
    override suspend fun getResult(resultId: String): CaricatureResult? = null
    override fun observeResult(resultId: String): Flow<CaricatureResult?> = MutableStateFlow(null)
    override fun getSelfieIdForResult(resultId: String): String? = null
    override suspend fun setFavourite(resultId: String, favourite: Boolean) = Unit
    override fun observeGalleryItems(): Flow<List<GalleryItem>> = MutableStateFlow(emptyList())
    override suspend fun deleteResult(resultId: String) = Unit
    override suspend fun clearGallery() = Unit
}
