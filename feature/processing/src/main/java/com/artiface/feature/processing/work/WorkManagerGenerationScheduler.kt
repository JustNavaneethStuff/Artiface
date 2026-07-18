package com.artiface.feature.processing.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerGenerationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : GenerationWorkScheduler {
    override fun enqueue(jobId: String, replace: Boolean) {
        val request = OneTimeWorkRequestBuilder<GenerationWorker>()
            .setInputData(workDataOf(GenerationWorker.KEY_JOB_ID to jobId))
            .addTag(GenerationWorker.uniqueWorkName(jobId))
            .build()
        val policy = if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP
        WorkManager.getInstance(context).enqueueUniqueWork(
            GenerationWorker.uniqueWorkName(jobId),
            policy,
            request,
        )
    }
}
