package com.resqlink.ai.domain.repository

import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.TrustedContact

interface EmergencyRepository {
    suspend fun getNearestHotlines(latitude: Double, longitude: Double, radiusKm: Double = 5.0): List<Hotline>
    suspend fun getAllHotlines(): List<Hotline>
    suspend fun getUniversalFallback(): Hotline
    suspend fun upsertHotlines(hotlines: List<Hotline>)
    suspend fun setPinnedHotline(id: String, isPinned: Boolean)
    suspend fun getPinnedHotline(): Hotline?
    suspend fun getTrustedContacts(): List<TrustedContact>
    suspend fun saveTrustedContact(contact: TrustedContact): Long
    suspend fun deleteTrustedContact(id: Long): Boolean
}
