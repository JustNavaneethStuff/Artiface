package com.artiface.feature.camera.presentation

import app.cash.turbine.test
import com.artiface.core.common.result.Result
import com.artiface.core.model.CapturedSelfie
import com.artiface.feature.camera.domain.SelfieCaptureStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class CameraViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun capture_while_already_capturing_is_ignored() = runTest {
        val store = FakeCaptureStore(delayPersist = true)
        val viewModel = CameraViewModel(store)
        viewModel.onEvent(CameraEvent.PermissionResultGranted)

        viewModel.effects.test {
            viewModel.onEvent(CameraEvent.Capture)
            advanceUntilIdle()
            assertThat(awaitItem()).isInstanceOf(CameraEffect.PerformCapture::class.java)

            viewModel.onEvent(CameraEvent.Capture)
            advanceUntilIdle()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun successful_persist_navigates_to_preview() = runTest {
        val selfie = CapturedSelfie(
            id = "abc",
            localUri = "file:///tmp/abc.jpg",
            capturedAt = Instant.parse("2026-07-14T10:00:00Z"),
            width = 100,
            height = 200,
            orientation = 1,
        )
        val store = FakeCaptureStore(persistResult = Result.Success(selfie))
        val viewModel = CameraViewModel(store)
        viewModel.onEvent(CameraEvent.PermissionResultGranted)

        viewModel.effects.test {
            viewModel.onImageSaved(File("unused"))
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(CameraEffect.NavigateToPreview("abc"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun switch_camera_toggles_lens() = runTest {
        val viewModel = CameraViewModel(FakeCaptureStore())
        assertThat(viewModel.uiState.value.lensFacing)
            .isEqualTo(androidx.camera.core.CameraSelector.LENS_FACING_FRONT)
        viewModel.onEvent(CameraEvent.SwitchCamera)
        assertThat(viewModel.uiState.value.lensFacing)
            .isEqualTo(androidx.camera.core.CameraSelector.LENS_FACING_BACK)
    }

    @Test
    fun permission_denied_permanently_updates_state() = runTest {
        val viewModel = CameraViewModel(FakeCaptureStore())
        viewModel.onEvent(CameraEvent.PermissionResultDenied(permanently = true))
        assertThat(viewModel.uiState.value.permissionStatus)
            .isEqualTo(CameraPermissionStatus.PermanentlyDenied)
    }
}

private class FakeCaptureStore(
    private val persistResult: Result<CapturedSelfie> = Result.Error(IllegalStateException("unused")),
    private val delayPersist: Boolean = false,
) : SelfieCaptureStore {
    override fun createTempCaptureFile(): File = File("temp.jpg")

    override suspend fun persistCapturedImage(sourceFile: File): Result<CapturedSelfie> {
        if (delayPersist) {
            kotlinx.coroutines.delay(10_000)
        }
        return persistResult
    }
}
