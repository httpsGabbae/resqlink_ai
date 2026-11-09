package com.resqlink.ai.ai

import android.util.Log
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.HotlineCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlin.math.*

class HotlineAiService {

    companion object {
        private const val TAG = "HotlineAiService"
        const val DEFAULT_MODEL = "gemini-3.1-flash-lite"
        const val ALT_MODEL = "gemini-2.5-flash-lite"
    }

    /**
     * Discovers emergency stations within a 5km radius using Gemini AI
     * and falls back to OpenStreetMap Overpass if offline or if no API key is provided.
     */
    suspend fun discover5kmHotlines(
        latitude: Double,
        longitude: Double,
        resolvedCity: String? = null,
        apiKey: String? = null,
        modelName: String = DEFAULT_MODEL
    ): List<Hotline> = withContext(Dispatchers.IO) {
        val key = apiKey?.trim()
        if (!key.isNullOrBlank()) {
            try {
                val geminiResults = queryGemini(latitude, longitude, resolvedCity, key, modelName)
                if (geminiResults.isNotEmpty()) {
                    Log.d(TAG, "Gemini AI ($modelName) discovered ${geminiResults.size} stations within 5km")
                    return@withContext geminiResults
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API query with $modelName failed: ${e.message}, trying fallback $ALT_MODEL")
                if (modelName != ALT_MODEL) {
                    try {
                        val fallbackResults = queryGemini(latitude, longitude, resolvedCity, key, ALT_MODEL)
                        if (fallbackResults.isNotEmpty()) {
                            Log.d(TAG, "Gemini AI fallback ($ALT_MODEL) discovered ${fallbackResults.size} stations within 5km")
                            return@withContext fallbackResults
                        }
                    } catch (e2: Exception) {
                        Log.e(TAG, "Gemini AI fallback also failed: ${e2.message}")
                    }
                }
            }
        }

        // Fallback: Query OpenStreetMap Overpass API (Free, no API key needed)
        try {
            val osmResults = queryOverpassApi(latitude, longitude, 5000.0)
            if (osmResults.isNotEmpty()) {
                Log.d(TAG, "Overpass API discovered ${osmResults.size} local stations")
                return@withContext osmResults
            }
        } catch (e: Exception) {
            Log.e(TAG, "Overpass fallback failed: ${e.message}")
        }

        emptyList()
    }

    private fun queryGemini(
        lat: Double,
        lng: Double,
        city: String?,
        apiKey: String,
        model: String
    ): List<Hotline> {
        val locationDesc = if (!city.isNullOrBlank() && !city.contains("Current GPS", ignoreCase = true)) "$city, Philippines" else "Philippines"
        val prompt = """
            Search and find verified official emergency hotline contact numbers within 5 kilometers of GPS coordinates ($lat, $lng) in $locationDesc.
            Include 5 to 8 stations across these categories:
            1. Local Barangay Halls, Barangay DRRMC, or Barangay Tanod emergency hotlines in this 5km sector.
            2. Philippine National Police (PNP) precinct or police station covering this location.
            3. Nearest 24/7 Hospitals, ER trauma centers, and medical rescue clinics.
            4. Bureau of Fire Protection (BFP) station.
            5. City/Municipal Disaster Risk Reduction and Management Office (CDRRMO/MDRRMO).
            
            Return ONLY a raw JSON array adhering to this schema:
            [
              {
                "name": "Barangay Holy Spirit Emergency Hall",
                "category": "BARANGAY",
                "primaryNumber": "02-8931-5047",
                "alternateNumber": "0917-123-4567",
                "cityMunicipality": "Quezon City",
                "province": "Metro Manila",
                "region": "NCR",
                "latitude": 14.6854,
                "longitude": 121.0742
              }
            ]
            Category must be exactly one of: BARANGAY, POLICE_PNP, MEDICAL_HOSPITAL, DISASTER_DRRMO, FIRE_BFP.
            Phone numbers must be real Philippine format. Return raw JSON without markdown code fences.
        """.trimIndent()

        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val requestBody = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            }
            put("contents", contentsArray)

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            })
        }

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use {
            it.write(requestBody.toString())
            it.flush()
        }

        val responseCode = conn.responseCode
        if (responseCode !in 200..299) {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
            conn.disconnect()
            throw RuntimeException("Gemini error $responseCode: $err")
        }

        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        return parseGeminiResponse(responseText, lat, lng)
    }

    private fun parseGeminiResponse(jsonStr: String, userLat: Double, userLng: Double): List<Hotline> {
        val hotlines = mutableListOf<Hotline>()
        try {
            val root = JSONObject(jsonStr)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            if (candidates.length() == 0) return emptyList()

            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return emptyList()
            val parts = content.optJSONArray("parts") ?: return emptyList()
            if (parts.length() == 0) return emptyList()

            var rawText = parts.getJSONObject(0).optString("text", "")
            rawText = rawText.trim()
            if (rawText.startsWith("```json")) rawText = rawText.removePrefix("```json")
            if (rawText.startsWith("```")) rawText = rawText.removePrefix("```")
            if (rawText.endsWith("```")) rawText = rawText.removeSuffix("```")
            rawText = rawText.trim()

            val array = JSONArray(rawText)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.optString("name", "Local Emergency Station").trim()
                val catStr = obj.optString("category", "DISASTER_DRRMO").uppercase()
                val category = try {
                    HotlineCategory.valueOf(catStr)
                } catch (e: Exception) {
                    when {
                        catStr.contains("BARANGAY") -> HotlineCategory.BARANGAY
                        catStr.contains("POLICE") || catStr.contains("PNP") -> HotlineCategory.POLICE_PNP
                        catStr.contains("HOSPITAL") || catStr.contains("MEDIC") -> HotlineCategory.MEDICAL_HOSPITAL
                        catStr.contains("FIRE") || catStr.contains("BFP") -> HotlineCategory.FIRE_BFP
                        else -> HotlineCategory.DISASTER_DRRMO
                    }
                }

                val primary = sanitizePhoneNumber(obj.optString("primaryNumber", "911"))
                val alt = obj.optString("alternateNumber", "").takeIf { it.isNotBlank() }?.let { sanitizePhoneNumber(it) }
                val city = obj.optString("cityMunicipality", "Local Area")
                val province = obj.optString("province", "Philippines")
                val region = obj.optString("region", "NCR")
                val sLat = obj.optDouble("latitude", userLat)
                val sLng = obj.optDouble("longitude", userLng)

                val dist = calculateDistanceKm(userLat, userLng, sLat, sLng)
                val hotline = Hotline(
                    id = "AI-" + UUID.randomUUID().toString().take(8).uppercase(),
                    name = name,
                    category = category,
                    primaryNumber = primary,
                    alternateNumber = alt,
                    cityMunicipality = city,
                    province = province,
                    region = region,
                    latitude = sLat,
                    longitude = sLng,
                    distanceKm = round(dist * 100.0) / 100.0,
                    source = "AI_GEMINI"
                )
                hotlines.add(hotline)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini JSON: ${e.message}", e)
        }
        return hotlines.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
    }

    private fun queryOverpassApi(lat: Double, lng: Double, radiusMeters: Double = 5000.0): List<Hotline> {
        val query = """
            [out:json][timeout:15];
            (
              node["amenity"~"police|hospital|clinic|fire_station|townhall"](around:$radiusMeters,$lat,$lng);
              way["amenity"~"police|hospital|clinic|fire_station|townhall"](around:$radiusMeters,$lat,$lng);
            );
            out center 15;
        """.trimIndent()

        val endpoint = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8")
        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 10000
        }

        if (conn.responseCode != 200) {
            conn.disconnect()
            return emptyList()
        }

        val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val results = mutableListOf<Hotline>()
        val root = JSONObject(jsonStr)
        val elements = root.optJSONArray("elements") ?: return emptyList()

        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            val tags = el.optJSONObject("tags") ?: continue
            val name = tags.optString("name", "").takeIf { it.isNotBlank() } ?: continue
            val amenity = tags.optString("amenity", "")

            val phone = tags.optString("emergency:phone", "")
                .takeIf { it.isNotBlank() }
                ?: tags.optString("phone", "")
                    .takeIf { it.isNotBlank() }
                ?: tags.optString("contact:phone", "")

            val category = when (amenity) {
                "police" -> HotlineCategory.POLICE_PNP
                "hospital", "clinic" -> HotlineCategory.MEDICAL_HOSPITAL
                "fire_station" -> HotlineCategory.FIRE_BFP
                "townhall" -> HotlineCategory.BARANGAY
                else -> HotlineCategory.DISASTER_DRRMO
            }

            val eLat = if (el.has("lat")) el.getDouble("lat") else el.optJSONObject("center")?.optDouble("lat", lat) ?: lat
            val eLng = if (el.has("lon")) el.getDouble("lon") else el.optJSONObject("center")?.optDouble("lon", lng) ?: lng
            val dist = calculateDistanceKm(lat, lng, eLat, eLng)

            results.add(
                Hotline(
                    id = "OSM-" + el.optLong("id", i.toLong()),
                    name = name,
                    category = category,
                    primaryNumber = if (phone.isNotBlank()) sanitizePhoneNumber(phone) else "122",
                    alternateNumber = null,
                    cityMunicipality = tags.optString("addr:city", "Local Area"),
                    province = "Philippines",
                    region = "NCR",
                    latitude = eLat,
                    longitude = eLng,
                    distanceKm = round(dist * 100.0) / 100.0,
                    source = "OVERPASS"
                )
            )
        }

        return results.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
    }

    private fun sanitizePhoneNumber(raw: String): String {
        return raw.trim()
            .replace("+63", "0")
            .replace(" ", "-")
            .replace("/", " / ")
            .trim()
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
}
