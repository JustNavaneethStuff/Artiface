package com.artiface.feature.camera.domain

import com.artiface.core.common.result.Result
import com.artiface.core.model.CapturedSelfie
import java.io.File

interface SelfieCaptureStore {
    fun createTempCaptureFile(): File

    suspend fun persistCapturedImage(sourceFile: File): Result<CapturedSelfie>
}
