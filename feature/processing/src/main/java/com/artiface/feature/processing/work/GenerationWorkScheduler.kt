package com.artiface.feature.processing.work

/**
 * Enqueues durable generation work. Implemented with WorkManager in production.
 */
interface GenerationWorkScheduler {
    fun enqueue(jobId: String, replace: Boolean = false)
}
