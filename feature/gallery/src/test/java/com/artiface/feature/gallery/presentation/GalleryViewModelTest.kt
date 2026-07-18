package com.artiface.feature.gallery.presentation

import app.cash.turbine.test
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GalleryItem
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleCatalog
import com.artiface.core.model.StyleId
import com.artiface.core.model.TimeOfDay
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class GalleryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun favourites_filter_hides_non_favourites() = runTest {
        val repo = FakeGalleryGenerationRepository(
            listOf(
                galleryItem("a", favourite = true),
                galleryItem("b", favourite = false),
            ),
        )
        val viewModel = GalleryViewModel(repo)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items.map { it.id }).containsExactly("a", "b")

        viewModel.onEvent(GalleryEvent.ToggleFavouritesFilter)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.favouritesOnly).isTrue()
        assertThat(viewModel.uiState.value.items.map { it.id }).containsExactly("a")
        assertThat(viewModel.uiState.value.isEmpty).isFalse()
    }

    @Test
    fun delete_confirmation_removes_item() = runTest {
        val repo = FakeGalleryGenerationRepository(
            listOf(galleryItem("a"), galleryItem("b")),
        )
        val viewModel = GalleryViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(GalleryEvent.DeleteClicked("a"))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.pendingDeleteId).isEqualTo("a")

        viewModel.onEvent(GalleryEvent.DeleteConfirmed)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.pendingDeleteId).isNull()
        assertThat(viewModel.uiState.value.items.map { it.id }).containsExactly("b")
        assertThat(repo.deletedIds).containsExactly("a")
    }

    @Test
    fun open_result_emits_navigation_effect() = runTest {
        val viewModel = GalleryViewModel(FakeGalleryGenerationRepository(emptyList()))
        viewModel.effects.test {
            viewModel.onEvent(GalleryEvent.OpenResult("result-9"))
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(GalleryEffect.NavigateToResult("result-9"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun galleryItem(id: String, favourite: Boolean = false) = GalleryItem(
        result = CaricatureResult(
            id = id,
            originalImageUri = "file:///o/$id.jpg",
            generatedImageUri = "file:///g/$id.jpg",
            styleId = StyleCatalog.ComicBurst.id,
            title = "Title $id",
            expression = ExpressionCategory.Neutral,
            timeOfDay = TimeOfDay.Afternoon,
            broadLocationLabel = null,
            createdAt = Instant.parse("2026-07-18T10:00:00Z"),
            isFavourite = favourite,
        ),
        selfieId = "selfie-$id",
        status = GenerationStatus.Completed,
    )
}

private class FakeGalleryGenerationRepository(
    initial: List<GalleryItem>,
) : GenerationRepository {
    private val items = MutableStateFlow(initial)
    val deletedIds = mutableListOf<String>()

    override fun observeGalleryItems(): Flow<List<GalleryItem>> = items
    override suspend fun deleteResult(resultId: String) {
        deletedIds += resultId
        items.value = items.value.filterNot { it.result.id == resultId }
    }

    override suspend fun clearGallery() {
        items.value = emptyList()
    }

    override suspend fun startGeneration(selfieId: String, styleId: StyleId): GenerationJob =
        error("unused")
    override fun observeJob(jobId: String): Flow<GenerationJob?> = MutableStateFlow(null)
    override suspend fun getJob(jobId: String): GenerationJob? = null
    override suspend fun getResult(resultId: String): CaricatureResult? = null
    override fun observeResult(resultId: String): Flow<CaricatureResult?> = MutableStateFlow(null)
    override suspend fun getResultIdForJob(jobId: String): String? = null
    override fun getSelfieIdForResult(resultId: String): String? = null
    override suspend fun setFavourite(resultId: String, favourite: Boolean) = Unit
    override suspend fun retry(jobId: String): GenerationJob = error("unused")
}
