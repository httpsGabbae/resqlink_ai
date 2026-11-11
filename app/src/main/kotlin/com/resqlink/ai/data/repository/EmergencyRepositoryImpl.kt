package com.resqlink.ai.data.repository

import com.resqlink.ai.data.local.HotlineDatabaseHelper
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.TrustedContact
import com.resqlink.ai.domain.repository.EmergencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * # EmergencyRepositoryImpl
 *
 * Concrete implementation of the [EmergencyRepository] interface.
 *
 * Ordinary Human Overview:
 * In Android development, database reading and writing on the "Main Thread" (UI thread)
 * will cause the screen to freeze and stutter. This class acts as a safety bridge:
 * it wraps every database operation in `withContext(Dispatchers.IO)` so that all database
 * queries happen quietly on background worker threads, keeping the user interface buttery-smooth.
 */
class EmergencyRepositoryImpl(
    private val databaseHelper: HotlineDatabaseHelper
) : EmergencyRepository {

    /**
     * Queries the offline database for emergency hotlines located within a specific radius of the user.
     */
    override suspend fun getNearestHotlines(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<Hotline> = withContext(Dispatchers.IO) {
        val nearestHotlinesList = databaseHelper.findNearestHotlinesWithinRadius(
            userLatitude = latitude,
            userLongitude = longitude,
            maxRadiusKilometers = radiusKm
        )

        if (nearestHotlinesList.isEmpty()) {
            listOf(databaseHelper.getUniversalFallback911())
        } else {
            nearestHotlinesList
        }
    }

    /**
     * Reads the entire catalog of pre-seeded Philippine hotlines on a background thread.
     */
    override suspend fun getAllHotlines(): List<Hotline> = withContext(Dispatchers.IO) {
        databaseHelper.getAllHotlines()
    }

    /**
     * Returns the universal National 911 fallback hotline.
     */
    override suspend fun getUniversalFallback(): Hotline = withContext(Dispatchers.IO) {
        databaseHelper.getUniversalFallback911()
    }

    /**
     * Saves newly discovered hotlines into SQLite on a background thread.
     */
    override suspend fun upsertHotlines(hotlines: List<Hotline>) = withContext(Dispatchers.IO) {
        databaseHelper.upsertHotlines(hotlines)
    }

    /**
     * Pins a hotline so it stays at the top of the dialing directory.
     */
    override suspend fun setPinnedHotline(id: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        databaseHelper.setPinnedHotline(hotlineId = id, isPinned = isPinned)
    }

    /**
     * Retrieves the currently pinned hotline, if any.
     */
    override suspend fun getPinnedHotline(): Hotline? = withContext(Dispatchers.IO) {
        databaseHelper.getPinnedHotline()
    }

    /**
     * Loads the user's In-Case-of-Emergency (ICE) trusted contacts.
     */
    override suspend fun getTrustedContacts(): List<TrustedContact> = withContext(Dispatchers.IO) {
        databaseHelper.getTrustedContacts()
    }

    /**
     * Saves or updates a trusted contact record.
     */
    override suspend fun saveTrustedContact(contact: TrustedContact): Long = withContext(Dispatchers.IO) {
        databaseHelper.addTrustedContact(contact)
    }

    /**
     * Deletes a trusted contact by row ID.
     */
    override suspend fun deleteTrustedContact(id: Long): Boolean = withContext(Dispatchers.IO) {
        databaseHelper.deleteTrustedContact(contactId = id) > 0
    }
}
