package com.resqlink.ai.data.repository

import com.resqlink.ai.data.local.HotlineDatabaseHelper
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.TrustedContact
import com.resqlink.ai.domain.repository.EmergencyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmergencyRepositoryImpl(
    private val dbHelper: HotlineDatabaseHelper
) : EmergencyRepository {

    override suspend fun getNearestHotlines(latitude: Double, longitude: Double, radiusKm: Double): List<Hotline> {
        return withContext(Dispatchers.IO) {
            val nearest = dbHelper.findNearestHotlinesWithinRadius(latitude, longitude, radiusKm)
            if (nearest.isEmpty()) {
                listOf(dbHelper.getUniversalFallback911())
            } else {
                nearest
            }
        }
    }

    override suspend fun getAllHotlines(): List<Hotline> {
        return withContext(Dispatchers.IO) {
            dbHelper.getAllHotlines()
        }
    }

    override suspend fun getUniversalFallback(): Hotline {
        return withContext(Dispatchers.IO) {
            dbHelper.getUniversalFallback911()
        }
    }

    override suspend fun upsertHotlines(hotlines: List<Hotline>) {
        withContext(Dispatchers.IO) {
            dbHelper.upsertHotlines(hotlines)
        }
    }

    override suspend fun setPinnedHotline(id: String, isPinned: Boolean) {
        withContext(Dispatchers.IO) {
            dbHelper.setPinnedHotline(id, isPinned)
        }
    }

    override suspend fun getPinnedHotline(): Hotline? {
        return withContext(Dispatchers.IO) {
            dbHelper.getPinnedHotline()
        }
    }

    override suspend fun getTrustedContacts(): List<TrustedContact> {
        return withContext(Dispatchers.IO) {
            dbHelper.getTrustedContacts()
        }
    }

    override suspend fun saveTrustedContact(contact: TrustedContact): Long {
        return withContext(Dispatchers.IO) {
            dbHelper.addTrustedContact(contact)
        }
    }

    override suspend fun deleteTrustedContact(id: Long): Boolean {
        return withContext(Dispatchers.IO) {
            dbHelper.deleteTrustedContact(id) > 0
        }
    }
}
