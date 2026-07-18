package com.artiface.core.network.remote

import android.content.Context
import androidx.core.net.toUri
import com.artiface.core.common.generation.CaricatureGenerator
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.GenerationContext
import com.artiface.core.model.GenerationStatus
import com.artiface.core.network.api.ArtifaceApi
import com.artiface.core.network.mapper.mapApiStatus
import com.artiface.core.network.mapper.toDomainResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote [CaricatureGenerator] that talks to [ArtifaceApi].
 * Enabled only when [com.artiface.core.network.config.NetworkConfig.useRemoteGenerator] is true.
 */
@Singleton
class RemoteCaricatureGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ArtifaceApi,
    private val okHttpClient: OkHttpClient,
) : CaricatureGenerator {

    override suspend fun generate(
        jobId: String,
        selfieLocalUri: String,
        context: GenerationContext,
        onProgress: suspend (GenerationStatus, Float) -> Unit,
    ): CaricatureResult {
        onProgress(GenerationStatus.PreparingImage, 0.1f)
        val imageFile = resolveSelfieFile(selfieLocalUri)
            ?: error("Unable to read selfie for upload")

        onProgress(GenerationStatus.Uploading, 0.25f)
        val imagePart = MultipartBody.Part.createFormData(
            name = "image",
            filename = imageFile.name,
            body = imageFile.asRequestBody("image/jpeg".toMediaType()),
        )
        val textType = "text/plain".toMediaType()
        val created = api.createGeneration(
            image = imagePart,
            styleId = context.styleId.value.toRequestBody(textType),
            expression = context.expression.name.toRequestBody(textType),
            timeOfDay = context.timeOfDay.name.toRequestBody(textType),
            broadLocationLabel = context.broadLocationLabel?.toRequestBody(textType),
            clientJobId = jobId.toRequestBody(textType),
        )

        val remoteJobId = created.jobId
        onProgress(mapApiStatus(created.status), created.progress.coerceAtLeast(0.35f))

        var latest = created
        var attempts = 0
        while (
            mapApiStatus(latest.status) != GenerationStatus.Completed &&
            mapApiStatus(latest.status) != GenerationStatus.Failed &&
            attempts < MAX_POLL_ATTEMPTS
        ) {
            onProgress(GenerationStatus.WaitingForProcessing, (0.4f + attempts * 0.02f).coerceAtMost(0.85f))
            delay(POLL_DELAY_MS)
            latest = api.getGeneration(remoteJobId)
            attempts += 1
        }

        if (mapApiStatus(latest.status) == GenerationStatus.Failed) {
            error(latest.errorMessage ?: "Remote generation failed")
        }
        val resultDto = latest.result
            ?: error("Remote generation completed without a result payload")

        onProgress(GenerationStatus.DownloadingResult, 0.9f)
        val localFile = downloadResultImage(resultDto.resultImageUrl)
        onProgress(GenerationStatus.Completed, 1f)

        return resultDto.toDomainResult(
            originalImageUri = selfieLocalUri,
            localGeneratedImageUri = localFile.toUri().toString(),
        )
    }

    private fun resolveSelfieFile(uriString: String): File? {
        val uri = uriString.toUri()
        return when (uri.scheme) {
            "file" -> uri.path?.let(::File)?.takeIf { it.exists() }
            else -> {
                val temp = File(context.cacheDir, "upload_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(temp).use { output -> input.copyTo(output) }
                } ?: return null
                temp
            }
        }
    }

    private suspend fun downloadResultImage(url: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Failed to download result image (${response.code})")
            }
            val body = response.body ?: error("Empty result image body")
            val dir = File(context.filesDir, RESULTS_DIR).also { it.mkdirs() }
            val file = File(dir, "remote_${UUID.randomUUID()}.jpg")
            FileOutputStream(file).use { out -> body.byteStream().copyTo(out) }
            file
        }
    }

    companion object {
        const val RESULTS_DIR = "results"
        private const val POLL_DELAY_MS = 1_000L
        private const val MAX_POLL_ATTEMPTS = 60
    }
}
