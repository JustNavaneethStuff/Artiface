package com.artiface.feature.processing.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.net.toUri
import com.artiface.core.common.generation.CaricatureGenerator
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.ExpressionCategory
import com.artiface.core.model.GenerationContext
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleCatalog
import com.artiface.core.model.TimeOfDay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Local fake generator with a realistic staged delay.
 * Replaceable later with a remote [CaricatureGenerator] implementation.
 */
@Singleton
class FakeCaricatureGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
) : CaricatureGenerator {

    @Volatile
    var forceNextFailure: Boolean = false

    override suspend fun generate(
        jobId: String,
        selfieLocalUri: String,
        context: GenerationContext,
        onProgress: suspend (GenerationStatus, Float) -> Unit,
    ): CaricatureResult {
        val generationContext = context
        val stages = listOf(
            GenerationStatus.PreparingImage to 0.15f,
            GenerationStatus.Uploading to 0.35f,
            GenerationStatus.WaitingForProcessing to 0.65f,
            GenerationStatus.DownloadingResult to 0.90f,
        )
        for ((status, progress) in stages) {
            onProgress(status, progress)
            delay(stageDelayMs)
            if (forceNextFailure && status == GenerationStatus.WaitingForProcessing) {
                forceNextFailure = false
                error("The colour spirits declined this negotiation")
            }
        }

        val style = StyleCatalog.require(generationContext.styleId)
        val title = TitleFactory.create(
            generationContext.expression,
            generationContext.timeOfDay,
            style.name,
        )
        val generatedFile = renderMockCaricature(
            sourceUri = selfieLocalUri,
            styleName = style.name,
            title = title,
            tint = styleTint(style.id.value),
        )

        onProgress(GenerationStatus.Completed, 1f)
        return CaricatureResult(
            id = UUID.randomUUID().toString(),
            originalImageUri = selfieLocalUri,
            generatedImageUri = generatedFile.toUri().toString(),
            styleId = generationContext.styleId,
            title = title,
            expression = generationContext.expression,
            timeOfDay = generationContext.timeOfDay,
            broadLocationLabel = generationContext.broadLocationLabel,
            createdAt = Instant.now(),
            isFavourite = false,
        )
    }

    private fun renderMockCaricature(
        sourceUri: String,
        styleName: String,
        title: String,
        tint: Int,
    ): File {
        val source = decodeBitmap(sourceUri)
        val width = source?.width?.coerceAtLeast(720) ?: 720
        val height = source?.height?.coerceAtLeast(960) ?: 960
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        if (source != null) {
            val scaled = Bitmap.createScaledBitmap(source, width, height, true)
            canvas.drawBitmap(scaled, 0f, 0f, null)
            if (scaled != source) scaled.recycle()
            source.recycle()
        } else {
            canvas.drawColor(Color.rgb(40, 28, 26))
        }

        val overlay = Paint().apply {
            color = tint
            alpha = 90
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)

        val band = Paint().apply { color = Color.argb(180, 20, 12, 10) }
        canvas.drawRect(0f, height * 0.72f, width.toFloat(), height.toFloat(), band)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = width * 0.055f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val stylePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 200, 87)
            textSize = width * 0.035f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText(title, width * 0.06f, height * 0.82f, titlePaint)
        canvas.drawText(styleName.uppercase(), width * 0.06f, height * 0.90f, stylePaint)

        val dir = File(this.context.filesDir, RESULTS_DIR).also { it.mkdirs() }
        val file = File(dir, "result_${UUID.randomUUID()}.jpg")
        FileOutputStream(file).use { out ->
            output.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        output.recycle()
        return file
    }

    private fun decodeBitmap(uriString: String): Bitmap? = runCatching {
        val uri = uriString.toUri()
        when (uri.scheme) {
            "file" -> BitmapFactory.decodeFile(uri.path)
            else -> this.context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
    }.getOrNull()

    private fun styleTint(styleId: String): Int = when (styleId) {
        "comic_burst" -> Color.rgb(255, 90, 60)
        "royal_absurdity" -> Color.rgb(92, 42, 77)
        "neon_mischief" -> Color.rgb(31, 166, 160)
        "storybook_chaos" -> Color.rgb(255, 200, 87)
        "retro_poster" -> Color.rgb(196, 92, 42)
        else -> Color.rgb(255, 90, 60)
    }

    companion object {
        const val RESULTS_DIR = "results"
        private val stageDelayMs = 700L
    }
}

object TitleFactory {
    private val titles = listOf(
        "The Midnight Schemer",
        "Commander of Questionable Decisions",
        "The Overqualified Daydreamer",
        "Supreme Minister of Snacks",
        "Duke of Mild Chaos",
        "Captain of Soft Rebellion",
        "The Dramatically Hydrated",
        "Baroness of Side Quests",
    )

    fun create(
        expression: ExpressionCategory,
        timeOfDay: TimeOfDay,
        styleName: String,
    ): String {
        val base = titles[Random.nextInt(titles.size)]
        return when {
            expression == ExpressionCategory.Mischievous -> "Agent of $styleName"
            timeOfDay == TimeOfDay.Night -> base.replace("Daydreamer", "Night Wanderer")
            else -> base
        }
    }
}
