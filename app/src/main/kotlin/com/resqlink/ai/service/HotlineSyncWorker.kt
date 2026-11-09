package com.resqlink.ai.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.resqlink.ai.ai.HotlineAiService
import com.resqlink.ai.data.local.HotlineDatabaseHelper
import com.resqlink.ai.data.local.SessionManager
import com.resqlink.ai.data.location.LocationProvider
import com.resqlink.ai.data.repository.EmergencyRepositoryImpl
import java.util.concurrent.TimeUnit

class HotlineSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "HotlineSyncWorker"
        const val WORK_NAME = "resqlink_2h_hotline_sync"

        /**
         * Enqueues recurring background sync every 2 hours whenever network is available.
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<HotlineSyncWorker>(2, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "2-Hour Hotline Background Sync scheduled via WorkManager")
        }

        /**
         * Triggers an immediate one-time background scan (e.g. on manual button press or area change).
         */
        fun triggerImmediateScan(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val oneTimeRequest = OneTimeWorkRequestBuilder<HotlineSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting 2-hour 5km hotline AI scan...")
        val locationProvider = LocationProvider(applicationContext)
        val location = locationProvider.getCurrentOrCachedLocation()

        if (location == null) {
            Log.w(TAG, "Location unavailable for hotline AI scan; deferring.")
            return Result.retry()
        }

        val sessionManager = SessionManager(applicationContext)
        val apiKey = sessionManager.getGeminiApiKey()
        val aiService = HotlineAiService()

        return try {
            val discovered = aiService.discover5kmHotlines(
                latitude = location.latitude,
                longitude = location.longitude,
                resolvedCity = location.address,
                apiKey = apiKey
            )

            if (discovered.isNotEmpty()) {
                val dbHelper = HotlineDatabaseHelper(applicationContext)
                val repository = EmergencyRepositoryImpl(dbHelper)
                repository.upsertHotlines(discovered)
                sessionManager.setLastAiScanTime(System.currentTimeMillis())
                Log.d(TAG, "Successfully synced ${discovered.size} local stations into offline SQLite cache.")
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during hotline AI sync: ${e.message}", e)
            Result.retry()
        }
    }
}
