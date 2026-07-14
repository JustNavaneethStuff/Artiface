package com.artiface.feature.camera.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.artiface.core.common.result.Result
import com.artiface.core.common.selfie.SelfieRepository
import com.artiface.core.model.CapturedSelfie
import com.artiface.feature.camera.domain.SelfieCaptureStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileSelfieRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SelfieRepository, SelfieCaptureStore {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val mutex = Mutex()
    private val cache = ConcurrentHashMap<String, CapturedSelfie>()
    private val updates = MutableStateFlow(0L)

    private val selfiesDir: File
        get() = File(context.filesDir, SELFIES_DIR).also { it.mkdirs() }

    init {
        // Best-effort warm cache for process-death recovery.
        selfiesDir.listFiles()
            ?.filter { it.extension.equals("jpg", ignoreCase = true) }
            ?.forEach { imageFile ->
                val id = imageFile.nameWithoutExtension
                readMeta(id)?.let { cache[id] = it }
            }
    }

    override suspend fun save(selfie: CapturedSelfie) {
        mutex.withLock {
            cache[selfie.id] = selfie
            writeMeta(selfie)
            updates.update { it + 1 }
        }
    }

    override suspend fun getById(id: String): CapturedSelfie? = mutex.withLock {
        cache[id] ?: readMeta(id)?.also { cache[id] = it }
    }

    override fun observeById(id: String): Flow<CapturedSelfie?> =
        updates.map { getCachedOrDisk(id) }

    override suspend fun delete(id: String) {
        mutex.withLock {
            cache.remove(id)
            imageFile(id).delete()
            metaFile(id).delete()
            updates.update { it + 1 }
        }
    }

    /**
     * Persists a captured JPEG into app storage and builds a [CapturedSelfie].
     */
    override suspend fun persistCapturedImage(
        sourceFile: File,
    ): Result<CapturedSelfie> = persistCapturedImage(sourceFile, UUID.randomUUID().toString())

    suspend fun persistCapturedImage(
        sourceFile: File,
        requestedId: String,
    ): Result<CapturedSelfie> = withContext(ioDispatcher) {
        try {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                error("Captured image is missing or empty")
            }
            val destination = imageFile(requestedId)
            sourceFile.copyTo(destination, overwrite = true)
            if (sourceFile.absolutePath != destination.absolutePath) {
                sourceFile.delete()
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, bounds)
            val exif = ExifInterface(destination.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )

            val selfie = CapturedSelfie(
                id = requestedId,
                localUri = destination.toUri().toString(),
                capturedAt = Instant.now(),
                width = bounds.outWidth.coerceAtLeast(0),
                height = bounds.outHeight.coerceAtLeast(0),
                orientation = orientation,
            )
            save(selfie)
            Result.Success(selfie)
        } catch (t: Throwable) {
            Result.Error(t)
        }
    }

    override fun createTempCaptureFile(): File =
        File(selfiesDir, "capture_${UUID.randomUUID()}.jpg")

    private fun getCachedOrDisk(id: String): CapturedSelfie? =
        cache[id] ?: readMeta(id)?.also { cache[id] = it }

    private fun imageFile(id: String): File = File(selfiesDir, "$id.jpg")

    private fun metaFile(id: String): File = File(selfiesDir, "$id.meta.json")

    private fun writeMeta(selfie: CapturedSelfie) {
        val json = JSONObject()
            .put("id", selfie.id)
            .put("localUri", selfie.localUri)
            .put("capturedAt", selfie.capturedAt.toString())
            .put("width", selfie.width)
            .put("height", selfie.height)
            .put("orientation", selfie.orientation)
        metaFile(selfie.id).writeText(json.toString())
    }

    private fun readMeta(id: String): CapturedSelfie? {
        val file = metaFile(id)
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            CapturedSelfie(
                id = json.getString("id"),
                localUri = json.getString("localUri"),
                capturedAt = Instant.parse(json.getString("capturedAt")),
                width = json.getInt("width"),
                height = json.getInt("height"),
                orientation = json.getInt("orientation"),
            )
        }.getOrNull()
    }

    companion object {
        const val SELFIES_DIR = "selfies"
    }
}
