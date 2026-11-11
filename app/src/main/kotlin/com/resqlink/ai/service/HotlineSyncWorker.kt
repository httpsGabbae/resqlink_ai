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

/**
 * # HotlineSyncWorker
 *
 * Periodic background synchronization worker scheduled via Android Jetpack `WorkManager`.
 *
 * Ordinary Human Overview:
 * When you travel to a new province or city, emergency hotline numbers change.
 * This worker runs quietly in the background:
 * 1. Checks every 2 hours when the phone is connected to the internet.
 * 2. Uses the user's current GPS position to query Overpass OpenStreetMap and Gemini AI
 *    for police stations, fire stations, and hospital trauma centers within a 5-kilometer radius.
 * 3. Saves all discovered stations directly into the on-device SQLite database.
 * 4. Ensures that if the internet goes out later, the phone ALREADY has all local barangay
 *    and city emergency numbers cached 100% offline.
 */
class HotlineSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    companion object {
        private const val LOG_TAG = "HotlineSyncWorker"
        const val WORK_NAME = "resqlink_2h_hotline_sync"
        private const val SYNC_INTERVAL_HOURS = 2L
        private const val RETRY_BACKOFF_MINUTES = 15L

        /**
         * Enqueues recurring background sync every 2 hours whenever network is available.
         */
        fun schedulePeriodicSync(context: Context) {
            val networkConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicSyncWorkRequest = PeriodicWorkRequestBuilder<HotlineSyncWorker>(SYNC_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, RETRY_BACKOFF_MINUTES, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicSyncWorkRequest
            )
            Log.d(LOG_TAG, "2-Hour Hotline Background Sync scheduled via WorkManager")
        }

        /**
         * Triggers an immediate one-time background scan (e.g. on manual button press or area change).
         */
        fun triggerImmediateScan(context: Context) {
            val networkConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val immediateSyncWorkRequest = OneTimeWorkRequestBuilder<HotlineSyncWorker>()
                .setConstraints(networkConstraints)
                .build()

            WorkManager.getInstance(context).enqueue(immediateSyncWorkRequest)
        }
    }

    override suspend fun doWork(): Result {
        Log.d(LOG_TAG, "Starting 2-hour 5km hotline AI scan...")
        val locationProvider = LocationProvider(applicationContext)
        val currentLocationSnapshot = locationProvider.getCurrentOrCachedLocation()

        if (currentLocationSnapshot == null) {
            Log.w(LOG_TAG, "Location unavailable for hotline AI scan; deferring to WorkManager retry.")
            return Result.retry()
        }

        val sessionManager = SessionManager(applicationContext)
        val geminiApiKey = sessionManager.getGeminiApiKey()
        val hotlineAiService = HotlineAiService()

        return try {
            val discoveredHotlinesList = hotlineAiService.discover5kmHotlines(
                latitude = currentLocationSnapshot.latitude,
                longitude = currentLocationSnapshot.longitude,
                resolvedCity = currentLocationSnapshot.address,
                apiKey = geminiApiKey
            )

            if (discoveredHotlinesList.isNotEmpty()) {
                val databaseHelper = HotlineDatabaseHelper(applicationContext)
                val emergencyRepository = EmergencyRepositoryImpl(databaseHelper)
                emergencyRepository.upsertHotlines(discoveredHotlinesList)
                sessionManager.setLastAiScanTime(System.currentTimeMillis())
                Log.d(LOG_TAG, "Successfully synced ${discoveredHotlinesList.size} local stations into offline SQLite cache.")
            }
            Result.success()
        } catch (exception: Exception) {
            Log.e(LOG_TAG, "Error during hotline AI sync: ${exception.message}", exception)
            Result.retry()
        }
    }
}
