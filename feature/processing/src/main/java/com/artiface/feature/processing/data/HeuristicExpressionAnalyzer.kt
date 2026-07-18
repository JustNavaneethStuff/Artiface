package com.artiface.feature.processing.data

import com.artiface.core.common.generation.ExpressionAnalyzer
import com.artiface.core.model.ExpressionCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

/**
 * Lightweight heuristic placeholder — not a medical or personality assessment.
 * A future ML Kit / TFLite implementation can replace this without UI changes.
 */
@Singleton
class HeuristicExpressionAnalyzer @Inject constructor() : ExpressionAnalyzer {
    override suspend fun analyze(selfieLocalUri: String): ExpressionCategory {
        val seed = selfieLocalUri.hashCode().absoluteValue
        val values = ExpressionCategory.entries.filter { it != ExpressionCategory.Unknown }
        return values[seed % values.size]
    }
}
