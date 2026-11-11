package com.resqlink.ai.domain.repository

import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.TrustedContact

/**
 * # EmergencyRepository
 *
 * The abstract interface (contract) defining all data operations required by the emergency system.
 *
 * In Clean Architecture, repositories hide where data comes from (SQLite database, remote server,
 * or memory cache). The ViewModel talks only to this interface without caring whether the data is
 * stored in SQLite, Room, or JSON.
 */
interface EmergencyRepository {

    /**
     * Finds verified emergency hotlines located within a specified radius of the user's GPS coordinates.
     *
     * @param latitude The user's live latitude coordinate.
     * @param longitude The user's live longitude coordinate.
     * @param radiusKm The maximum search radius in kilometers (defaults to 2.5 km).
     * @return List of matching hotlines, sorted by distance from nearest to furthest.
     */
    suspend fun getNearestHotlines(latitude: Double, longitude: Double, radiusKm: Double = 2.5): List<Hotline>

    /**
     * Retrieves the complete catalog of all pre-seeded offline hotlines stored in the database.
     */
    suspend fun getAllHotlines(): List<Hotline>

    /**
     * Returns the nationwide 911 fallback hotline used when no local municipal station is within range.
     */
    suspend fun getUniversalFallback(): Hotline

    /**
     * Inserts new hotlines or updates existing hotlines in the database (e.g. from background AI scanning).
     */
    suspend fun upsertHotlines(hotlines: List<Hotline>)

    /**
     * Pins or unpins a specific hotline so it remains at the top of the user's dialing list.
     */
    suspend fun setPinnedHotline(id: String, isPinned: Boolean)

    /**
     * Retrieves the currently pinned emergency hotline, if the user selected one.
     */
    suspend fun getPinnedHotline(): Hotline?

    /**
     * Retrieves all saved In-Case-of-Emergency (ICE) trusted contacts.
     */
    suspend fun getTrustedContacts(): List<TrustedContact>

    /**
     * Saves a new trusted contact or updates an existing one in the local database.
     *
     * @return The database row ID of the saved contact.
     */
    suspend fun saveTrustedContact(contact: TrustedContact): Long

    /**
     * Deletes a trusted contact by its unique database row ID.
     *
     * @return True if the contact was successfully removed.
     */
    suspend fun deleteTrustedContact(id: Long): Boolean
}
