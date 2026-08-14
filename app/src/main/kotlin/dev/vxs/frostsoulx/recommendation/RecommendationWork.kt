/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package dev.vxs.frostsoulx.recommendation

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

class OfflineRecommendationWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val engine =
            EntryPointAccessors
                .fromApplication(applicationContext, OfflineRecommendationWorkerEntryPoint::class.java)
                .engine()
        return when (
            engine.refresh(
                context =
                    RecommendationContext(
                        hourOfDay = java.time.LocalDateTime.now().hour,
                        dayOfWeek = java.time.LocalDateTime.now().dayOfWeek.value,
                        isHeadphones = false,
                        isBluetooth = false,
                        isCharging = false,
                        isOffline = true,
                    ),
            )
        ) {
            is RecommendationRefreshState.Success,
            RecommendationRefreshState.Empty,
            -> Result.success()
            is RecommendationRefreshState.Failure -> Result.retry()
            RecommendationRefreshState.Idle,
            RecommendationRefreshState.Refreshing,
            -> Result.success()
        }
    }
}

object OfflineRecommendationScheduler {
    fun enqueue(context: Context) {
        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()
        val request =
            OneTimeWorkRequestBuilder<OfflineRecommendationWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .addTag(WorkName)
                .build()
        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(WorkName, ExistingWorkPolicy.KEEP, request)
    }

    private const val WorkName = "offline-recommendation-refresh"
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface OfflineRecommendationWorkerEntryPoint {
    fun engine(): OfflineRecommendationEngine
}
