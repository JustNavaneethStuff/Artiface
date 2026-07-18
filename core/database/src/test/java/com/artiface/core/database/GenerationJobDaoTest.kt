package com.artiface.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.artiface.core.model.GenerationStatus
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GenerationJobDaoTest {

    private lateinit var database: ArtifaceDatabase
    private lateinit var dao: GenerationJobDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ArtifaceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.generationJobDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsert_and_observe_by_id() = runTest {
        dao.upsert(sampleJob(id = "j1", status = GenerationStatus.PreparingImage))

        val observed = dao.observeById("j1").first()
        assertThat(observed).isNotNull()
        assertThat(observed!!.selfieId).isEqualTo("selfie-j1")
        assertThat(observed.styleId).isEqualTo("comic_burst")
        assertThat(observed.toDomain().status).isEqualTo(GenerationStatus.PreparingImage)
    }

    @Test
    fun update_progress_and_mark_failed() = runTest {
        dao.upsert(sampleJob(id = "j1"))
        dao.updateProgress(
            id = "j1",
            status = GenerationStatus.Uploading.name,
            progress = 0.35f,
            updatedAtEpochMs = 2_000L,
        )
        assertThat(dao.getById("j1")!!.progress).isEqualTo(0.35f)

        dao.markTerminal(
            id = "j1",
            status = GenerationStatus.Failed.name,
            progress = 0.35f,
            updatedAtEpochMs = 3_000L,
            errorMessage = "Interrupted — tap retry",
            resultId = null,
        )
        val failed = dao.getById("j1")!!
        assertThat(failed.status).isEqualTo(GenerationStatus.Failed.name)
        assertThat(failed.errorMessage).isEqualTo("Interrupted — tap retry")
        assertThat(dao.getActive()).isEmpty()
    }

    @Test
    fun mark_completed_attaches_result_id() = runTest {
        dao.upsert(sampleJob(id = "j1"))
        dao.markTerminal(
            id = "j1",
            status = GenerationStatus.Completed.name,
            progress = 1f,
            updatedAtEpochMs = 4_000L,
            errorMessage = null,
            resultId = "result-9",
        )
        val done = dao.getById("j1")!!
        assertThat(done.resultId).isEqualTo("result-9")
        assertThat(dao.getActive()).isEmpty()
    }

    @Test
    fun get_active_returns_only_non_terminal() = runTest {
        dao.upsert(sampleJob(id = "active", status = GenerationStatus.WaitingForProcessing))
        dao.upsert(sampleJob(id = "done", status = GenerationStatus.Completed, updatedAt = 2_000L))
        dao.upsert(sampleJob(id = "fail", status = GenerationStatus.Failed, updatedAt = 3_000L))

        assertThat(dao.getActive().map { it.id }).containsExactly("active")
    }

    private fun sampleJob(
        id: String,
        status: GenerationStatus = GenerationStatus.PreparingImage,
        updatedAt: Long = 1_000L,
    ) = GenerationJobEntity(
        id = id,
        selfieId = "selfie-$id",
        styleId = "comic_burst",
        status = status.name,
        progress = 0f,
        createdAtEpochMs = 1_000L,
        updatedAtEpochMs = updatedAt,
        errorMessage = null,
        resultId = null,
    )
}
