package com.artiface.feature.result.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GalleryItem
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.StyleCatalog
import com.artiface.core.model.StyleId
import com.artiface.core.model.TimeOfDay
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
class ResultViewModelTest {

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
    fun loads_result_and_builds_context_summary() = runTest {
        val result = sampleResult(favourite = false)
        val repo = FakeResultGenerationRepository(result)
        val viewModel = createViewModel(result.id, repo)

        viewModel.uiState.test {
            runCurrent()
            val state = expectMostRecentItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.missing).isFalse()
            assertThat(state.result?.id).isEqualTo(result.id)
            assertThat(state.styleName).isEqualTo(StyleCatalog.ComicBurst.name)
            assertThat(state.contextSummary).contains("joyful")
            assertThat(state.contextSummary).contains("night")
            assertThat(state.selfieId).isEqualTo("selfie-1")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun missing_result_sets_missing_flag() = runTest {
        val repo = FakeResultGenerationRepository(result = null)
        val viewModel = createViewModel("missing-id", repo)

        viewModel.uiState.test {
            runCurrent()
            val state = expectMostRecentItem()
            assertThat(state.missing).isTrue()
            assertThat(state.isLoading).isFalse()
            assertThat(state.result).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun toggle_favourite_updates_repository() = runTest {
        val result = sampleResult(favourite = false)
        val repo = FakeResultGenerationRepository(result)
        val viewModel = createViewModel(result.id, repo)

        viewModel.uiState.test {
            runCurrent()
            assertThat(expectMostRecentItem().result?.isFavourite).isFalse()

            viewModel.onEvent(ResultEvent.ToggleFavourite)
            runCurrent()

            assertThat(repo.lastFavourite).isTrue()
            assertThat(expectMostRecentItem().result?.isFavourite).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun share_and_save_emit_effects() = runTest {
        val result = sampleResult()
        val viewModel = createViewModel(result.id, FakeResultGenerationRepository(result))

        viewModel.effects.test {
            runCurrent()
            viewModel.onEvent(ResultEvent.Share)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                ResultEffect.ShareImage(result.generatedImageUri, result.title),
            )

            viewModel.onEvent(ResultEvent.Save)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(
                ResultEffect.SaveImage(result.generatedImageUri, result.title),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun navigation_events_emit_effects() = runTest {
        val result = sampleResult()
        val viewModel = createViewModel(result.id, FakeResultGenerationRepository(result))

        viewModel.effects.test {
            runCurrent()

            viewModel.onEvent(ResultEvent.TryAnotherStyle)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(ResultEffect.NavigateTryAnotherStyle("selfie-1"))

            viewModel.onEvent(ResultEvent.CreateAnother)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(ResultEffect.NavigateCreateAnother)

            viewModel.onEvent(ResultEvent.OpenGallery)
            runCurrent()
            assertThat(awaitItem()).isEqualTo(ResultEffect.NavigateGallery)

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createViewModel(
        resultId: String,
        repo: GenerationRepository,
    ): ResultViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ResultViewModel(
                    SavedStateHandle(mapOf("resultId" to resultId)),
                    repo,
                ) as T
            }
        }
        return ViewModelProvider(store, factory)[ResultViewModel::class.java]
    }

    private fun sampleResult(favourite: Boolean = false) = CaricatureResult(
        id = "result-1",
        originalImageUri = "file:///selfie.jpg",
        generatedImageUri = "file:///results/out.jpg",
        styleId = StyleCatalog.ComicBurst.id,
        title = "The Midnight Schemer",
        expression = ExpressionCategory.Joyful,
        timeOfDay = TimeOfDay.Night,
        broadLocationLabel = null,
        createdAt = Instant.parse("2026-07-18T12:00:00Z"),
        isFavourite = favourite,
    )
}

private class FakeResultGenerationRepository(
    result: CaricatureResult?,
) : GenerationRepository {
    private val results = MutableStateFlow(
        result?.let { mapOf(it.id to it) } ?: emptyMap(),
    )
    var lastFavourite: Boolean? = null

    override fun observeResult(resultId: String): Flow<CaricatureResult?> =
        results.map { it[resultId] }

    override suspend fun getResult(resultId: String): CaricatureResult? = results.value[resultId]

    override fun getSelfieIdForResult(resultId: String): String? =
        results.value[resultId]?.let { "selfie-1" }

    override suspend fun setFavourite(resultId: String, favourite: Boolean) {
        lastFavourite = favourite
        val current = results.value[resultId] ?: return
        results.value = results.value + (resultId to current.copy(isFavourite = favourite))
    }

    override suspend fun startGeneration(selfieId: String, styleId: StyleId): GenerationJob =
        error("unused")
    override fun observeJob(jobId: String): Flow<GenerationJob?> = MutableStateFlow(null)
    override suspend fun getJob(jobId: String): GenerationJob? = null
    override suspend fun getResultIdForJob(jobId: String): String? = null
    override suspend fun retry(jobId: String): GenerationJob = error("unused")
    override fun observeGalleryItems(): Flow<List<GalleryItem>> = MutableStateFlow(emptyList())
    override suspend fun deleteResult(resultId: String) = Unit
    override suspend fun clearGallery() = Unit
}
