package com.artiface.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.TimeOfDay
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaricatureResultDaoTest {

    private lateinit var database: ArtifaceDatabase
    private lateinit var dao: CaricatureResultDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ArtifaceDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.caricatureResultDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsert_and_observe_all() = runTest {
        dao.upsert(sampleEntity(id = "r1", favourite = false))
        dao.upsert(sampleEntity(id = "r2", favourite = true, createdAt = 2_000L))

        val items = dao.observeAll().first()
        assertThat(items.map { it.id }).containsExactly("r2", "r1").inOrder()
    }

    @Test
    fun set_favourite_and_observe_favourites() = runTest {
        dao.upsert(sampleEntity(id = "r1", favourite = false))
        dao.setFavourite("r1", true)

        val favourites = dao.observeFavourites().first()
        assertThat(favourites).hasSize(1)
        assertThat(favourites.first().isFavourite).isTrue()
    }

    @Test
    fun delete_by_id_and_delete_all() = runTest {
        dao.upsert(sampleEntity(id = "r1"))
        dao.upsert(sampleEntity(id = "r2", createdAt = 2_000L))
        dao.deleteById("r1")
        assertThat(dao.count()).isEqualTo(1)

        dao.deleteAll()
        assertThat(dao.count()).isEqualTo(0)
        assertThat(dao.observeAll().first()).isEmpty()
    }

    private fun sampleEntity(
        id: String,
        favourite: Boolean = false,
        createdAt: Long = 1_000L,
    ) = CaricatureResultEntity(
        id = id,
        selfieId = "selfie-$id",
        originalImageUri = "file:///selfies/$id.jpg",
        generatedImageUri = "file:///results/$id.jpg",
        styleId = "comic_burst",
        title = "Title $id",
        expression = ExpressionCategory.Joyful.name,
        timeOfDay = TimeOfDay.Morning.name,
        broadLocationLabel = null,
        createdAtEpochMs = createdAt,
        isFavourite = favourite,
        status = GenerationStatus.Completed.name,
    )
}
