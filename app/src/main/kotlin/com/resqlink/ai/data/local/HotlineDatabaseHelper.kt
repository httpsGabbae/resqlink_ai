package com.resqlink.ai.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.HotlineCategory
import com.resqlink.ai.domain.model.TrustedContact
import com.resqlink.ai.incidents.IncidentRecord
import com.resqlink.ai.incidents.IncidentTimelineEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.*

/**
 * # HotlineDatabaseHelper
 *
 * The offline SQLite database engine powering ResQLink AI.
 *
 * Ordinary Human Overview:
 * When an earthquake strikes or mobile data towers go down, online map searches fail.
 * This helper pre-loads and maintains an on-device SQLite database containing:
 * 1. Over 50+ official Philippine emergency hotlines (PNP, BFP, Red Cross, DPWH, DRRMO).
 * 2. In-Case-of-Emergency (ICE) trusted family contacts.
 * 3. Chronological incident audit logs recording every alert trigger, GPS location, and outcome.
 *
 * It uses the mathematical **Haversine formula** to calculate physical distances directly
 * on the phone CPU without needing any internet connection or cloud API.
 */
class HotlineDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "resqlink.db"
        private const val DATABASE_VERSION = 7

        // Hotlines table & columns
        const val TABLE_HOTLINES = "hotlines"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_CATEGORY = "category"
        const val COL_PRIMARY_NUMBER = "primary_number"
        const val COL_ALT_NUMBER = "alternate_number"
        const val COL_CITY = "city_municipality"
        const val COL_PROVINCE = "province"
        const val COL_REGION = "region"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
        const val COL_IS_CUSTOM = "is_custom"
        const val COL_IS_PINNED = "is_pinned"
        const val COL_LAST_VERIFIED = "last_verified"
        const val COL_SOURCE = "source"

        // Trusted Contacts table & columns
        const val TABLE_TRUSTED_CONTACTS = "trusted_contacts"
        const val COL_CONTACT_ID = "id"
        const val COL_CONTACT_NAME = "name"
        const val COL_CONTACT_PHONE = "phone_number"
        const val COL_CONTACT_RELATIONSHIP = "relationship"
        const val COL_CONTACT_IS_AUTHORIZED = "is_authorized"

        // Incident Logs table & columns
        const val TABLE_INCIDENT_LOGS = "incident_logs"
        const val COL_LOG_INCIDENT_ID = "incident_id"
        const val COL_LOG_TRIGGER_SOURCE = "trigger_source"
        const val COL_LOG_DETECTED_PHRASE = "detected_phrase"
        const val COL_LOG_FINAL_STATE = "final_state"
        const val COL_LOG_START_TIME = "start_time"
        const val COL_LOG_RESOLVED_TIME = "resolved_time"
        const val COL_LOG_LATITUDE = "latitude"
        const val COL_LOG_LONGITUDE = "longitude"
        const val COL_LOG_ACCURACY = "accuracy"
        const val COL_LOG_IS_SIMULATED = "is_simulated"
        const val COL_LOG_CONTACTS_ATTEMPTED = "contacts_attempted"
        const val COL_LOG_CONTACTS_SENT = "contacts_sent"
        const val COL_LOG_CALL_INITIATED = "call_initiated"
        const val COL_LOG_TIMELINE_JSON = "timeline_json"
        const val COL_LOG_NOTES = "notes"
    }

    override fun onCreate(database: SQLiteDatabase) {
        // Create hotlines directory table
        database.execSQL(
            """
            CREATE TABLE $TABLE_HOTLINES (
                $COL_ID TEXT PRIMARY KEY,
                $COL_NAME TEXT NOT NULL,
                $COL_CATEGORY TEXT NOT NULL,
                $COL_PRIMARY_NUMBER TEXT NOT NULL,
                $COL_ALT_NUMBER TEXT,
                $COL_CITY TEXT NOT NULL,
                $COL_PROVINCE TEXT NOT NULL,
                $COL_REGION TEXT NOT NULL,
                $COL_LATITUDE REAL NOT NULL,
                $COL_LONGITUDE REAL NOT NULL,
                $COL_IS_CUSTOM INTEGER DEFAULT 0,
                $COL_IS_PINNED INTEGER DEFAULT 0,
                $COL_LAST_VERIFIED INTEGER DEFAULT 0,
                $COL_SOURCE TEXT DEFAULT 'SYSTEM'
            )
            """.trimIndent()
        )

        // Create trusted emergency contacts table
        database.execSQL(
            """
            CREATE TABLE $TABLE_TRUSTED_CONTACTS (
                $COL_CONTACT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_CONTACT_NAME TEXT NOT NULL,
                $COL_CONTACT_PHONE TEXT NOT NULL,
                $COL_CONTACT_RELATIONSHIP TEXT NOT NULL,
                $COL_CONTACT_IS_AUTHORIZED INTEGER DEFAULT 1
            )
            """.trimIndent()
        )

        createIncidentLogsTable(database)
        seedInitialHotlines(database)
        seedSampleContact(database)
    }

    private fun createIncidentLogsTable(database: SQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_INCIDENT_LOGS (
                $COL_LOG_INCIDENT_ID TEXT PRIMARY KEY,
                $COL_LOG_TRIGGER_SOURCE TEXT NOT NULL,
                $COL_LOG_DETECTED_PHRASE TEXT,
                $COL_LOG_FINAL_STATE TEXT NOT NULL,
                $COL_LOG_START_TIME INTEGER NOT NULL,
                $COL_LOG_RESOLVED_TIME INTEGER,
                $COL_LOG_LATITUDE REAL,
                $COL_LOG_LONGITUDE REAL,
                $COL_LOG_ACCURACY REAL,
                $COL_LOG_IS_SIMULATED INTEGER DEFAULT 0,
                $COL_LOG_CONTACTS_ATTEMPTED INTEGER DEFAULT 0,
                $COL_LOG_CONTACTS_SENT INTEGER DEFAULT 0,
                $COL_LOG_CALL_INITIATED INTEGER DEFAULT 0,
                $COL_LOG_TIMELINE_JSON TEXT,
                $COL_LOG_NOTES TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            try {
                database.execSQL("ALTER TABLE $TABLE_HOTLINES ADD COLUMN $COL_IS_PINNED INTEGER DEFAULT 0")
            } catch (ignored: Exception) {}
            try {
                database.execSQL("ALTER TABLE $TABLE_HOTLINES ADD COLUMN $COL_LAST_VERIFIED INTEGER DEFAULT 0")
            } catch (ignored: Exception) {}
            try {
                database.execSQL("ALTER TABLE $TABLE_HOTLINES ADD COLUMN $COL_SOURCE TEXT DEFAULT 'SYSTEM'")
            } catch (ignored: Exception) {}
        }
        if (oldVersion < 6) {
            seedInitialHotlines(database)
        }
        if (oldVersion < 7) {
            createIncidentLogsTable(database)
        }
    }

    /**
     * Pre-seeds the database with official nationwide and regional Philippine emergency hotlines.
     */
    private fun seedInitialHotlines(database: SQLiteDatabase) {
        val initialHotlinesList = listOf(
            // Universal National Hotlines (Official Kaya Natin / PH Emergency Callout)
            Hotline("911-NAT", "National Emergency Hotline", HotlineCategory.UNIVERSAL_911, "911", null, "National", "Philippines", "NCR", 14.5995, 120.9842),
            Hotline("PNP-117", "Philippine National Police (PNP)", HotlineCategory.POLICE_PNP, "117", "0917-847-5757", "National", "Philippines", "NCR", 14.6150, 121.0543),
            Hotline("PRC-143", "Philippine Red Cross", HotlineCategory.MEDICAL_HOSPITAL, "143", "02-8527-0000", "National", "Philippines", "NCR", 14.5778, 121.0372),
            Hotline("DPWH-16502", "Department of Public Works and Highways (DPWH)", HotlineCategory.DISASTER_DRRMO, "165-02", "02-8530-0000", "National", "Philippines", "NCR", 14.5885, 120.9752),
            Hotline("BFP-NAT", "Bureau of Fire Protection National HQ", HotlineCategory.FIRE_BFP, "02-8426-0219", null, "Quezon City", "Metro Manila", "NCR", 14.6468, 121.0494),

            // Quezon City
            Hotline("QC-DRRMO", "Quezon City DRRMO Disaster Operations", HotlineCategory.DISASTER_DRRMO, "122", "02-8927-5914", "Quezon City", "Metro Manila", "NCR", 14.6507, 121.0509),
            Hotline("QC-PNP", "Quezon City Police District (QCPD)", HotlineCategory.POLICE_PNP, "02-8925-8417", "0917-840-3925", "Quezon City", "Metro Manila", "NCR", 14.6534, 121.0540),
            Hotline("QC-PNP-PS9", "PNP Anonas Police Station (PS 9)", HotlineCategory.POLICE_PNP, "02-8921-2550", "0917-899-8727", "Quezon City", "Metro Manila", "NCR", 14.6295, 121.0664),
            Hotline("QC-BFP", "Bureau of Fire Protection - QC Central", HotlineCategory.FIRE_BFP, "02-8924-0048", "02-8928-3665", "Quezon City", "Metro Manila", "NCR", 14.6517, 121.0494),
            Hotline("QC-PHC", "Philippine Heart Center ER Trauma", HotlineCategory.MEDICAL_HOSPITAL, "02-8925-2401", "02-8925-2411", "Quezon City", "Metro Manila", "NCR", 14.6465, 121.0478),
            Hotline("QC-EAMC", "East Avenue Medical Center ER", HotlineCategory.MEDICAL_HOSPITAL, "02-8928-0611", null, "Quezon City", "Metro Manila", "NCR", 14.6432, 121.0482),
            Hotline("QC-QMMC", "Quirino Memorial Medical Center ER", HotlineCategory.MEDICAL_HOSPITAL, "02-8530-4800", null, "Quezon City", "Metro Manila", "NCR", 14.6277, 121.0699),
            Hotline("QC-BRGY-UP", "Barangay UP Campus Emergency Hall", HotlineCategory.BARANGAY, "02-8981-8500", "0928-501-1234", "Quezon City", "Metro Manila", "NCR", 14.6538, 121.0685),
            Hotline("QC-BRGY-CEN", "Barangay Central Disaster Action", HotlineCategory.BARANGAY, "02-8920-5471", null, "Quezon City", "Metro Manila", "NCR", 14.6475, 121.0505),
            Hotline("QC-BRGY-BAT", "Barangay Batasan Hills Emergency Ops", HotlineCategory.BARANGAY, "02-8951-2489", null, "Quezon City", "Metro Manila", "NCR", 14.6854, 121.0942),

            // Manila
            Hotline("MNL-DRRMO", "Manila Disaster Risk Reduction (MDRRMO)", HotlineCategory.DISASTER_DRRMO, "02-8527-5174", "0917-857-4263", "Manila", "Metro Manila", "NCR", 14.5995, 120.9842),
            Hotline("MNL-PNP", "Manila Police District (MPD)", HotlineCategory.POLICE_PNP, "02-8523-3378", null, "Manila", "Metro Manila", "NCR", 14.5880, 120.9855),
            Hotline("MNL-PGH", "Philippine General Hospital (PGH) Trauma ER", HotlineCategory.MEDICAL_HOSPITAL, "02-8554-8400", null, "Manila", "Metro Manila", "NCR", 14.5772, 120.9880),
            Hotline("MNL-BFP", "Manila Fire Department Central Ops", HotlineCategory.FIRE_BFP, "02-8527-3932", null, "Manila", "Metro Manila", "NCR", 14.5901, 120.9840),
            Hotline("MNL-BRGY-ERM", "Barangay 659 Ermita Disaster Center", HotlineCategory.BARANGAY, "02-8527-1111", null, "Manila", "Metro Manila", "NCR", 14.5910, 120.9810),

            // Makati
            Hotline("MKT-C3", "Makati Command, Control & Comm Center", HotlineCategory.DISASTER_DRRMO, "168", "02-8870-1920", "Makati", "Metro Manila", "NCR", 14.5547, 121.0244),
            Hotline("MKT-MMC", "Makati Medical Center 24/7 ER", HotlineCategory.MEDICAL_HOSPITAL, "02-8888-8999", null, "Makati", "Metro Manila", "NCR", 14.5590, 121.0145),
            Hotline("MKT-PNP", "Makati Police District Headquarters", HotlineCategory.POLICE_PNP, "02-8887-1798", null, "Makati", "Metro Manila", "NCR", 14.5610, 121.0210),
            Hotline("MKT-BFP", "Makati Central Fire Station", HotlineCategory.FIRE_BFP, "02-8818-0158", null, "Makati", "Metro Manila", "NCR", 14.5600, 121.0180),
            Hotline("MKT-BRGY-SL", "Barangay San Lorenzo Emergency Center", HotlineCategory.BARANGAY, "02-8817-4886", null, "Makati", "Metro Manila", "NCR", 14.5510, 121.0230),

            // Pasig
            Hotline("PSG-RESCUE", "Pasig City Disaster Risk & Rescue Team", HotlineCategory.DISASTER_DRRMO, "02-8643-0000", "0908-899-7332", "Pasig", "Metro Manila", "NCR", 14.5764, 121.0851),
            Hotline("PSG-PCGH", "Pasig City General Hospital ER", HotlineCategory.MEDICAL_HOSPITAL, "02-8643-3333", null, "Pasig", "Metro Manila", "NCR", 14.5630, 121.0920),
            Hotline("PSG-PNP", "Pasig City Police Station", HotlineCategory.POLICE_PNP, "02-8641-0433", null, "Pasig", "Metro Manila", "NCR", 14.5750, 121.0820),
            Hotline("PSG-BFP", "Pasig City Central Fire Station", HotlineCategory.FIRE_BFP, "02-8641-2815", null, "Pasig", "Metro Manila", "NCR", 14.5770, 121.0830),
            Hotline("PSG-BRGY-SA", "Barangay San Antonio Rescue Center", HotlineCategory.BARANGAY, "02-8631-5080", null, "Pasig", "Metro Manila", "NCR", 14.5820, 121.0650),

            // Taguig / BGC
            Hotline("TGG-C3", "Taguig City Command Center Rescue", HotlineCategory.DISASTER_DRRMO, "02-8789-3200", "0917-550-3727", "Taguig", "Metro Manila", "NCR", 14.5204, 121.0539),
            Hotline("TGG-TPDH", "Taguig-Pateros District Hospital ER", HotlineCategory.MEDICAL_HOSPITAL, "02-8837-8344", null, "Taguig", "Metro Manila", "NCR", 14.5120, 121.0680),
            Hotline("TGG-PNP", "Taguig City Police Station", HotlineCategory.POLICE_PNP, "02-8642-3582", null, "Taguig", "Metro Manila", "NCR", 14.5240, 121.0540),
            Hotline("TGG-BFP", "Taguig City Fire Station", HotlineCategory.FIRE_BFP, "02-8837-0740", null, "Taguig", "Metro Manila", "NCR", 14.5260, 121.0560),
            Hotline("TGG-BRGY-FB", "Barangay Fort Bonifacio Emergency Command", HotlineCategory.BARANGAY, "02-8888-2422", null, "Taguig", "Metro Manila", "NCR", 14.5500, 121.0500),

            // Other NCR Centers
            Hotline("MRK-161", "Marikina Rescue 161 Operations", HotlineCategory.DISASTER_DRRMO, "161", "02-8646-2436", "Marikina", "Metro Manila", "NCR", 14.6507, 121.1029),
            Hotline("MDL-DRRMO", "Mandaluyong Emergency Rescue Service", HotlineCategory.DISASTER_DRRMO, "02-8534-1760", null, "Mandaluyong", "Metro Manila", "NCR", 14.5794, 121.0359),
            Hotline("PSY-RESCUE", "Pasay City Rescue Emergency Team", HotlineCategory.DISASTER_DRRMO, "02-8551-7777", null, "Pasay", "Metro Manila", "NCR", 14.5378, 121.0014),
            Hotline("PRQ-DRRMO", "Parañaque City Emergency Dispatch", HotlineCategory.DISASTER_DRRMO, "02-8820-7777", null, "Parañaque", "Metro Manila", "NCR", 14.4793, 121.0198),
            Hotline("LP-RESCUE", "Las Piñas Disaster & Rescue Operations", HotlineCategory.DISASTER_DRRMO, "02-8871-4264", null, "Las Piñas", "Metro Manila", "NCR", 14.4445, 120.9939),
            Hotline("MNT-ALERT", "Alert Muntinlupa Emergency Command", HotlineCategory.DISASTER_DRRMO, "02-8925-4357", null, "Muntinlupa", "Metro Manila", "NCR", 14.4081, 121.0415),
            Hotline("CLC-DRRMO", "Caloocan City Disaster Rescue Team", HotlineCategory.DISASTER_DRRMO, "02-8888-2856", null, "Caloocan", "Metro Manila", "NCR", 14.6488, 120.9742),

            // Cavite & Neighboring Centers
            Hotline("BCR-DRRMO", "Bacoor City Disaster Risk Reduction (BDRRMO)", HotlineCategory.DISASTER_DRRMO, "046-417-0727", "0917-827-2667", "Bacoor", "Cavite", "Region IV-A", 14.4624, 120.9645),
            Hotline("BCR-PNP", "Bacoor City Police Station", HotlineCategory.POLICE_PNP, "046-417-7560", null, "Bacoor", "Cavite", "Region IV-A", 14.4600, 120.9620),
            Hotline("BCR-STRH", "Southern Tagalog Regional Hospital ER", HotlineCategory.MEDICAL_HOSPITAL, "046-417-0727", null, "Bacoor", "Cavite", "Region IV-A", 14.4550, 120.9580),
            Hotline("IMS-DRRMO", "Imus City CDRRMO Rescue Center", HotlineCategory.DISASTER_DRRMO, "046-471-0629", "0917-512-4687", "Imus", "Cavite", "Region IV-A", 14.4296, 120.9367),
            Hotline("IMS-PNP", "Imus City Police Station", HotlineCategory.POLICE_PNP, "046-471-0629", null, "Imus", "Cavite", "Region IV-A", 14.4250, 120.9380),
            Hotline("DAS-DRRMO", "Dasmariñas City Emergency Action Team", HotlineCategory.DISASTER_DRRMO, "046-416-0275", "0917-848-6911", "Dasmariñas", "Cavite", "Region IV-A", 14.3294, 120.9367),
            Hotline("DAS-PNP", "Dasmariñas City Police Station", HotlineCategory.POLICE_PNP, "046-416-0275", null, "Dasmariñas", "Cavite", "Region IV-A", 14.3280, 120.9350),
            Hotline("DAS-DLSUMC", "DLSUMC University Medical Center ER", HotlineCategory.MEDICAL_HOSPITAL, "046-481-8000", null, "Dasmariñas", "Cavite", "Region IV-A", 14.3230, 120.9410),

            // Regional Centers
            Hotline("BAG-911", "Baguio City SMART 911 Command", HotlineCategory.DISASTER_DRRMO, "074-442-1900", "0911-123-4567", "Baguio City", "Benguet", "CAR", 16.4023, 120.5960),
            Hotline("ANG-RESCUE", "Angeles City Disaster Risk Team", HotlineCategory.DISASTER_DRRMO, "045-322-7796", null, "Angeles City", "Pampanga", "Region III", 15.1450, 120.5887),
            Hotline("SFP-DRRMO", "City of San Fernando Disaster Operations", HotlineCategory.DISASTER_DRRMO, "045-961-4384", null, "San Fernando", "Pampanga", "Region III", 15.0298, 120.6896),
            Hotline("ANT-DRRMO", "Antipolo CDRRMO Operations Center", HotlineCategory.DISASTER_DRRMO, "02-8697-0308", null, "Antipolo", "Rizal", "Region IV-A", 14.5842, 121.1763),
            Hotline("SPR-DRRMO", "San Pedro City Disaster Risk & Rescue", HotlineCategory.DISASTER_DRRMO, "02-8808-2020", "0998-592-3868", "San Pedro", "Laguna", "Region IV-A", 14.3583, 121.0583),
            Hotline("STR-DRRMO", "Santa Rosa City CDRRMO Operations", HotlineCategory.DISASTER_DRRMO, "049-530-0015", "0917-501-4444", "Santa Rosa", "Laguna", "Region IV-A", 14.3122, 121.1114),
            Hotline("CLM-DRRMO", "Calamba City Emergency Response Command", HotlineCategory.DISASTER_DRRMO, "049-545-6789", "0917-848-2273", "Calamba", "Laguna", "Region IV-A", 14.2117, 121.1656),
            Hotline("SJD-DRRMO", "San Jose del Monte CDRRMO Action Team", HotlineCategory.DISASTER_DRRMO, "044-815-2838", "0917-888-7536", "San Jose del Monte", "Bulacan", "Region III", 14.8137, 121.0453),
            Hotline("MAL-DRRMO", "Malolos City Disaster Risk Team", HotlineCategory.DISASTER_DRRMO, "044-791-0566", "0923-741-2345", "Malolos", "Bulacan", "Region III", 14.8527, 120.8160),
            Hotline("LIP-DRRMO", "Lipa City CDRRMO Emergency Dispatch", HotlineCategory.DISASTER_DRRMO, "043-756-1234", "0917-504-8888", "Lipa", "Batangas", "Region IV-A", 13.9419, 121.1644),
            Hotline("CEB-ERUF", "Cebu Emergency Rescue Unit Foundation (ERUF)", HotlineCategory.DISASTER_DRRMO, "166", "032-255-7575", "Cebu City", "Cebu", "Region VII", 10.3157, 123.8854),
            Hotline("ILO-RESCUE", "Iloilo City Emergency Action Team", HotlineCategory.DISASTER_DRRMO, "033-333-1111", null, "Iloilo City", "Iloilo", "Region VI", 10.7202, 122.5621),
            Hotline("BCD-DRRMO", "Bacolod City CDRRMO Action Center", HotlineCategory.DISASTER_DRRMO, "034-432-3883", null, "Bacolod City", "Negros Occidental", "Region VI", 10.6765, 122.9509),
            Hotline("DAV-911", "Davao City Central 911 Operations", HotlineCategory.UNIVERSAL_911, "911", "082-299-1111", "Davao City", "Davao del Sur", "Region XI", 7.1907, 125.4553),
            Hotline("CDO-911", "Cagayan de Oro Oro Rescue 911", HotlineCategory.DISASTER_DRRMO, "911", "088-858-4080", "Cagayan de Oro", "Misamis Oriental", "Region X", 8.4542, 124.6319)
        )

        for (hotlineItem in initialHotlinesList) {
            val databaseValues = ContentValues().apply {
                put(COL_ID, hotlineItem.id)
                put(COL_NAME, hotlineItem.name)
                put(COL_CATEGORY, hotlineItem.category.name)
                put(COL_PRIMARY_NUMBER, hotlineItem.primaryNumber)
                put(COL_ALT_NUMBER, hotlineItem.alternateNumber)
                put(COL_CITY, hotlineItem.cityMunicipality)
                put(COL_PROVINCE, hotlineItem.province)
                put(COL_REGION, hotlineItem.region)
                put(COL_LATITUDE, hotlineItem.latitude)
                put(COL_LONGITUDE, hotlineItem.longitude)
                put(COL_IS_CUSTOM, 0)
            }
            database.insertWithOnConflict(TABLE_HOTLINES, null, databaseValues, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }

    private fun seedSampleContact(database: SQLiteDatabase) {
        val sampleContactValues = ContentValues().apply {
            put(COL_CONTACT_NAME, "Sample Guardian Contact")
            put(COL_CONTACT_PHONE, "09171234567")
            put(COL_CONTACT_RELATIONSHIP, "Family")
            put(COL_CONTACT_IS_AUTHORIZED, 1)
        }
        database.insert(TABLE_TRUSTED_CONTACTS, null, sampleContactValues)
    }

    /**
     * Reads all emergency hotlines stored in the offline database.
     */
    fun getAllHotlines(): List<Hotline> {
        val hotlinesList = mutableListOf<Hotline>()
        val readableDb = readableDatabase
        val queryCursor: Cursor = readableDb.rawQuery("SELECT * FROM $TABLE_HOTLINES", null)
        queryCursor.use { cursor ->
            while (cursor.moveToNext()) {
                hotlinesList.add(cursorToHotline(cursor))
            }
        }
        return hotlinesList
    }

    /**
     * Finds the nearest emergency hotlines within a radius of the user's GPS coordinates.
     *
     * Fallback Strategy:
     * 1. Query within primary radius (default: 5.0 km + 1.5 km margin).
     * 2. If no station found, expands radius to 25.0 km (for rural/provincial areas).
     * 3. If still empty, finds the closest local station by distance.
     * 4. Ultimate failsafe: Returns the National 911 fallback hotline.
     */
    fun findNearestHotlinesWithinRadius(
        userLatitude: Double,
        userLongitude: Double,
        maxRadiusKilometers: Double = 5.0
    ): List<Hotline> {
        // Step 1: Search within primary radius
        val primaryCandidates = queryStationsWithinRadius(userLatitude, userLongitude, maxRadiusKilometers + 1.5)
        if (primaryCandidates.isNotEmpty()) {
            return primaryCandidates
        }

        // Step 2: Expand search radius to 25.0 km for suburban and provincial coverage
        val expandedProvincialCandidates = queryStationsWithinRadius(userLatitude, userLongitude, 25.0)
        if (expandedProvincialCandidates.isNotEmpty()) {
            return expandedProvincialCandidates
        }

        // Step 3: Fallback - Find the closest non-911 municipal station by distance
        val allStoredHotlines = getAllHotlines()
        val nonUniversalHotlines = allStoredHotlines.filter {
            it.category != HotlineCategory.UNIVERSAL_911 && it.primaryNumber != "911"
        }
        if (nonUniversalHotlines.isNotEmpty()) {
            val closestStations = nonUniversalHotlines.map { hotline ->
                val distanceKm = calculateHaversineDistanceKm(
                    userLatitude,
                    userLongitude,
                    hotline.latitude,
                    hotline.longitude
                )
                hotline.copy(distanceKm = (round(distanceKm * 100.0) / 100.0))
            }.sortedBy { it.distanceKm ?: Double.MAX_VALUE }

            return closestStations.take(3)
        }

        // Step 4: Fallback to Universal 911
        return listOf(getUniversalFallback911())
    }

    /**
     * Performs a fast bounding-box SQL query followed by exact Haversine distance verification.
     */
    private fun queryStationsWithinRadius(
        userLatitude: Double,
        userLongitude: Double,
        radiusKilometers: Double
    ): List<Hotline> {
        // 1 degree of latitude is approximately 111.0 kilometers
        val latitudeDelta = radiusKilometers / 111.0
        val longitudeDelta = radiusKilometers / (111.0 * cos(Math.toRadians(userLatitude)))

        val minimumLatitude = userLatitude - latitudeDelta
        val maximumLatitude = userLatitude + latitudeDelta
        val minimumLongitude = userLongitude - abs(longitudeDelta)
        val maximumLongitude = userLongitude + abs(longitudeDelta)

        val readableDb = readableDatabase
        val boundingBoxQuery = """
            SELECT * FROM $TABLE_HOTLINES 
            WHERE $COL_LATITUDE BETWEEN ? AND ? 
            AND $COL_LONGITUDE BETWEEN ? AND ?
        """.trimIndent()

        val candidateHotlines = mutableListOf<Hotline>()
        val queryCursor = readableDb.rawQuery(
            boundingBoxQuery,
            arrayOf(
                minimumLatitude.toString(),
                maximumLatitude.toString(),
                minimumLongitude.toString(),
                maximumLongitude.toString()
            )
        )

        queryCursor.use { cursor ->
            while (cursor.moveToNext()) {
                val hotlineItem = cursorToHotline(cursor)
                val exactDistanceKilometers = calculateHaversineDistanceKm(
                    userLatitude,
                    userLongitude,
                    hotlineItem.latitude,
                    hotlineItem.longitude
                )
                if (exactDistanceKilometers <= radiusKilometers) {
                    val roundedDistance = (round(exactDistanceKilometers * 100.0) / 100.0)
                    candidateHotlines.add(hotlineItem.copy(distanceKm = roundedDistance))
                }
            }
        }

        // Sort: Pinned stations first, then closest distance
        return candidateHotlines.sortedWith(
            compareByDescending<Hotline> { it.isPinned }
                .thenBy { it.distanceKm ?: Double.MAX_VALUE }
        )
    }

    /**
     * Returns the universal 911 fallback hotline.
     */
    fun getUniversalFallback911(): Hotline {
        val readableDb = readableDatabase
        val queryCursor = readableDb.rawQuery(
            "SELECT * FROM $TABLE_HOTLINES WHERE $COL_CATEGORY = ?",
            arrayOf(HotlineCategory.UNIVERSAL_911.name)
        )
        queryCursor.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToHotline(cursor)
            }
        }
        return Hotline(
            id = "911-FALLBACK",
            name = "National Emergency 911",
            category = HotlineCategory.UNIVERSAL_911,
            primaryNumber = "911",
            cityMunicipality = "National",
            province = "Philippines",
            region = "NCR",
            latitude = 14.5995,
            longitude = 120.9842
        )
    }

    /**
     * Retrieves all saved In-Case-of-Emergency (ICE) trusted contacts.
     */
    fun getTrustedContacts(): List<TrustedContact> {
        val contactList = mutableListOf<TrustedContact>()
        val readableDb = readableDatabase
        val queryCursor = readableDb.rawQuery("SELECT * FROM $TABLE_TRUSTED_CONTACTS", null)
        queryCursor.use { cursor ->
            while (cursor.moveToNext()) {
                contactList.add(
                    TrustedContact(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CONTACT_ID)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTACT_NAME)),
                        phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTACT_PHONE)),
                        relationship = cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTACT_RELATIONSHIP)),
                        isAuthorizedForSafetyCheck = cursor.getInt(cursor.getColumnIndexOrThrow(COL_CONTACT_IS_AUTHORIZED)) == 1
                    )
                )
            }
        }
        return contactList
    }

    /**
     * Adds a new trusted contact to the local database.
     */
    fun addTrustedContact(contact: TrustedContact): Long {
        val writableDb = writableDatabase
        val contactValues = ContentValues().apply {
            put(COL_CONTACT_NAME, contact.name)
            put(COL_CONTACT_PHONE, contact.phoneNumber)
            put(COL_CONTACT_RELATIONSHIP, contact.relationship)
            put(COL_CONTACT_IS_AUTHORIZED, if (contact.isAuthorizedForSafetyCheck) 1 else 0)
        }
        return writableDb.insert(TABLE_TRUSTED_CONTACTS, null, contactValues)
    }

    /**
     * Deletes a trusted contact by its unique database row ID.
     */
    fun deleteTrustedContact(contactId: Long): Int {
        val writableDb = writableDatabase
        return writableDb.delete(TABLE_TRUSTED_CONTACTS, "$COL_CONTACT_ID = ?", arrayOf(contactId.toString()))
    }

    /**
     * Inserts or updates multiple hotlines in a single atomic database transaction.
     */
    fun upsertHotlines(hotlinesList: List<Hotline>) {
        val writableDb = writableDatabase
        writableDb.beginTransaction()
        try {
            for (hotlineItem in hotlinesList) {
                // Check if the hotline is already pinned by the user so we don't accidentally unpin it
                var isAlreadyPinned = false
                val checkCursor = writableDb.rawQuery(
                    "SELECT $COL_IS_PINNED FROM $TABLE_HOTLINES WHERE $COL_ID = ?",
                    arrayOf(hotlineItem.id)
                )
                checkCursor.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val pinColumnIndex = cursor.getColumnIndex(COL_IS_PINNED)
                        if (pinColumnIndex != -1) {
                            isAlreadyPinned = cursor.getInt(pinColumnIndex) == 1
                        }
                    }
                }

                val hotlineValues = ContentValues().apply {
                    put(COL_ID, hotlineItem.id)
                    put(COL_NAME, hotlineItem.name)
                    put(COL_CATEGORY, hotlineItem.category.name)
                    put(COL_PRIMARY_NUMBER, hotlineItem.primaryNumber)
                    put(COL_ALT_NUMBER, hotlineItem.alternateNumber)
                    put(COL_CITY, hotlineItem.cityMunicipality)
                    put(COL_PROVINCE, hotlineItem.province)
                    put(COL_REGION, hotlineItem.region)
                    put(COL_LATITUDE, hotlineItem.latitude)
                    put(COL_LONGITUDE, hotlineItem.longitude)
                    put(COL_IS_CUSTOM, if (hotlineItem.isCustom) 1 else 0)
                    put(COL_IS_PINNED, if (hotlineItem.isPinned || isAlreadyPinned) 1 else 0)
                    put(COL_LAST_VERIFIED, hotlineItem.lastVerifiedTimestamp)
                    put(COL_SOURCE, hotlineItem.source)
                }
                writableDb.insertWithOnConflict(TABLE_HOTLINES, null, hotlineValues, SQLiteDatabase.CONFLICT_REPLACE)
            }
            writableDb.setTransactionSuccessful()
        } finally {
            writableDb.endTransaction()
        }
    }

    /**
     * Pins a specific hotline so it remains at the top of lists. Clears any existing pin.
     */
    fun setPinnedHotline(hotlineId: String, isPinned: Boolean) {
        val writableDb = writableDatabase
        writableDb.beginTransaction()
        try {
            // Clear existing pins so only 1 station is pinned at a time
            val clearPinValues = ContentValues().apply { put(COL_IS_PINNED, 0) }
            writableDb.update(TABLE_HOTLINES, clearPinValues, null, null)

            if (isPinned) {
                val setPinValues = ContentValues().apply { put(COL_IS_PINNED, 1) }
                writableDb.update(TABLE_HOTLINES, setPinValues, "$COL_ID = ?", arrayOf(hotlineId))
            }
            writableDb.setTransactionSuccessful()
        } finally {
            writableDb.endTransaction()
        }
    }

    /**
     * Retrieves the currently pinned emergency hotline, if one is pinned.
     */
    fun getPinnedHotline(): Hotline? {
        val readableDb = readableDatabase
        val queryCursor = readableDb.rawQuery(
            "SELECT * FROM $TABLE_HOTLINES WHERE $COL_IS_PINNED = 1 LIMIT 1",
            null
        )
        queryCursor.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursorToHotline(cursor)
            }
        }
        return null
    }

    /**
     * Converts an active SQLite database [Cursor] row into a [Hotline] domain object.
     */
    private fun cursorToHotline(cursor: Cursor): Hotline {
        val isPinnedIndex = cursor.getColumnIndex(COL_IS_PINNED)
        val isPinned = if (isPinnedIndex != -1) cursor.getInt(isPinnedIndex) == 1 else false

        val lastVerifiedIndex = cursor.getColumnIndex(COL_LAST_VERIFIED)
        val lastVerifiedTimestamp = if (lastVerifiedIndex != -1) cursor.getLong(lastVerifiedIndex) else 0L

        val sourceIndex = cursor.getColumnIndex(COL_SOURCE)
        val source = if (sourceIndex != -1) cursor.getString(sourceIndex) ?: "SYSTEM" else "SYSTEM"

        return Hotline(
            id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
            name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
            category = try {
                HotlineCategory.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)))
            } catch (exception: Exception) {
                HotlineCategory.UNIVERSAL_911
            },
            primaryNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRIMARY_NUMBER)),
            alternateNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_ALT_NUMBER)),
            cityMunicipality = cursor.getString(cursor.getColumnIndexOrThrow(COL_CITY)),
            province = cursor.getString(cursor.getColumnIndexOrThrow(COL_PROVINCE)),
            region = cursor.getString(cursor.getColumnIndexOrThrow(COL_REGION)),
            latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)),
            longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE)),
            isPinned = isPinned,
            lastVerifiedTimestamp = lastVerifiedTimestamp,
            source = source
        )
    }

    /**
     * Calculates the great-circle distance between two geographic coordinates in kilometers
     * using the mathematical Haversine formula.
     *
     * Formula:
     * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
     * c = 2 * atan2(√a, √(1-a))
     * distance = Earth_Radius * c
     */
    private fun calculateHaversineDistanceKm(
        latitude1: Double,
        longitude1: Double,
        latitude2: Double,
        longitude2: Double
    ): Double {
        val earthRadiusKilometers = 6371.0
        val deltaLatitudeRadians = Math.toRadians(latitude2 - latitude1)
        val deltaLongitudeRadians = Math.toRadians(longitude2 - longitude1)

        val haversineA = sin(deltaLatitudeRadians / 2) * sin(deltaLatitudeRadians / 2) +
                cos(Math.toRadians(latitude1)) * cos(Math.toRadians(latitude2)) *
                sin(deltaLongitudeRadians / 2) * sin(deltaLongitudeRadians / 2)

        val angularDistanceC = 2 * atan2(sqrt(haversineA), sqrt(1 - haversineA))
        return earthRadiusKilometers * angularDistanceC
    }

    // ==========================================
    // INCIDENT LOGS (OFFLINE CHRONOLOGICAL AUDIT)
    // ==========================================

    /**
     * Inserts an incident record into the audit log table.
     */
    fun insertIncidentLog(incidentRecord: IncidentRecord): Long {
        val writableDb = writableDatabase
        val timelineJson = try {
            Json.encodeToString(incidentRecord.timeline)
        } catch (serializationException: Exception) {
            "[]"
        }

        val logValues = ContentValues().apply {
            put(COL_LOG_INCIDENT_ID, incidentRecord.incidentId)
            put(COL_LOG_TRIGGER_SOURCE, incidentRecord.triggerSource)
            put(COL_LOG_DETECTED_PHRASE, incidentRecord.initialDetectedPhrase)
            put(COL_LOG_FINAL_STATE, incidentRecord.finalState)
            put(COL_LOG_START_TIME, incidentRecord.startTimeMillis)
            if (incidentRecord.resolvedTimeMillis != null) {
                put(COL_LOG_RESOLVED_TIME, incidentRecord.resolvedTimeMillis)
            } else {
                putNull(COL_LOG_RESOLVED_TIME)
            }
            if (incidentRecord.latitude != null) put(COL_LOG_LATITUDE, incidentRecord.latitude) else putNull(COL_LOG_LATITUDE)
            if (incidentRecord.longitude != null) put(COL_LOG_LONGITUDE, incidentRecord.longitude) else putNull(COL_LOG_LONGITUDE)
            if (incidentRecord.locationAccuracyMeters != null) put(COL_LOG_ACCURACY, incidentRecord.locationAccuracyMeters) else putNull(COL_LOG_ACCURACY)
            put(COL_LOG_IS_SIMULATED, if (incidentRecord.isSimulated) 1 else 0)
            put(COL_LOG_CONTACTS_ATTEMPTED, incidentRecord.contactsAttempted)
            put(COL_LOG_CONTACTS_SENT, incidentRecord.contactsSent)
            put(COL_LOG_CALL_INITIATED, if (incidentRecord.emergencyCallInitiated) 1 else 0)
            put(COL_LOG_TIMELINE_JSON, timelineJson)
            put(COL_LOG_NOTES, incidentRecord.notes)
        }

        return writableDb.insertWithOnConflict(
            TABLE_INCIDENT_LOGS,
            null,
            logValues,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    /**
     * Retrieves all historical incident logs sorted by newest first.
     */
    fun getAllIncidentLogs(): List<IncidentRecord> {
        val readableDb = readableDatabase
        val incidentList = mutableListOf<IncidentRecord>()
        val queryCursor = readableDb.query(
            TABLE_INCIDENT_LOGS,
            null,
            null,
            null,
            null,
            null,
            "$COL_LOG_START_TIME DESC"
        )
        queryCursor.use { cursor ->
            while (cursor.moveToNext()) {
                incidentList.add(cursorToIncidentRecord(cursor))
            }
        }
        return incidentList
    }

    /**
     * Retrieves a single incident record by its unique incident ID.
     */
    fun getIncidentLogById(incidentId: String): IncidentRecord? {
        val readableDb = readableDatabase
        val queryCursor = readableDb.query(
            TABLE_INCIDENT_LOGS,
            null,
            "$COL_LOG_INCIDENT_ID = ?",
            arrayOf(incidentId),
            null,
            null,
            null
        )
        return queryCursor.use { cursor ->
            if (cursor.moveToFirst()) cursorToIncidentRecord(cursor) else null
        }
    }

    /**
     * Deletes a single incident record.
     */
    fun deleteIncidentLog(incidentId: String): Int {
        val writableDb = writableDatabase
        return writableDb.delete(TABLE_INCIDENT_LOGS, "$COL_LOG_INCIDENT_ID = ?", arrayOf(incidentId))
    }

    /**
     * Clears all incident logs from disk.
     */
    fun clearAllIncidentLogs(): Int {
        val writableDb = writableDatabase
        return writableDb.delete(TABLE_INCIDENT_LOGS, null, null)
    }

    /**
     * Converts a database cursor row into an [IncidentRecord] object.
     */
    private fun cursorToIncidentRecord(cursor: Cursor): IncidentRecord {
        val timelineRawJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_TIMELINE_JSON))
        val parsedTimeline = try {
            if (!timelineRawJson.isNullOrBlank()) {
                Json.decodeFromString<List<IncidentTimelineEntry>>(timelineRawJson)
            } else {
                emptyList()
            }
        } catch (deserializationException: Exception) {
            emptyList()
        }

        return IncidentRecord(
            incidentId = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_INCIDENT_ID)),
            triggerSource = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_TRIGGER_SOURCE)),
            initialDetectedPhrase = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_DETECTED_PHRASE)),
            finalState = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_FINAL_STATE)),
            startTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_START_TIME)),
            resolvedTimeMillis = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_LOG_RESOLVED_TIME))) null else cursor.getLong(cursor.getColumnIndexOrThrow(COL_LOG_RESOLVED_TIME)),
            latitude = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_LOG_LATITUDE))) null else cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_LATITUDE)),
            longitude = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_LOG_LONGITUDE))) null else cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LOG_LONGITUDE)),
            locationAccuracyMeters = if (cursor.isNull(cursor.getColumnIndexOrThrow(COL_LOG_ACCURACY))) null else cursor.getFloat(cursor.getColumnIndexOrThrow(COL_LOG_ACCURACY)),
            isSimulated = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_IS_SIMULATED)) == 1,
            contactsAttempted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_CONTACTS_ATTEMPTED)),
            contactsSent = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_CONTACTS_SENT)),
            emergencyCallInitiated = cursor.getInt(cursor.getColumnIndexOrThrow(COL_LOG_CALL_INITIATED)) == 1,
            timeline = parsedTimeline,
            notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_LOG_NOTES))
        )
    }
}
