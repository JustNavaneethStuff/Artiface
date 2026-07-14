package com.artiface.core.common.selfie

import app.cash.turbine.test
import com.artiface.core.model.CapturedSelfie
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.Instant

class FakeSelfieRepositoryTest {

    @Test
    fun save_and_get_round_trip() = runTest {
        val repo = FakeSelfieRepository()
        val selfie = CapturedSelfie(
            id = "id-1",
            localUri = "file:///tmp/id-1.jpg",
            capturedAt = Instant.parse("2026-07-14T08:00:00Z"),
            width = 640,
            height = 480,
            orientation = 1,
        )
        repo.save(selfie)
        assertThat(repo.getById("id-1")).isEqualTo(selfie)
    }

    @Test
    fun observe_emits_updates() = runTest {
        val repo = FakeSelfieRepository()
        val selfie = CapturedSelfie(
            id = "id-2",
            localUri = "file:///tmp/id-2.jpg",
            capturedAt = Instant.parse("2026-07-14T08:00:00Z"),
            width = 100,
            height = 100,
            orientation = 1,
        )
        repo.observeById("id-2").test {
            assertThat(awaitItem()).isNull()
            repo.save(selfie)
            assertThat(awaitItem()).isEqualTo(selfie)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
