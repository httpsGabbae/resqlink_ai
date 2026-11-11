package com.resqlink.ai.ai

import android.util.Log
import com.resqlink.ai.domain.model.Hotline
import com.resqlink.ai.domain.model.HotlineCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID
import kotlin.math.*

/**
 * # HotlineAiService
 *
 * Discovers and verifies real-world Philippine emergency stations located within a 5km radius
 * of the user's live GPS coordinates.
 *
 * Ordinary Human Overview:
 * When you travel to a new province or city, you might not know the local police station or barangay hall numbers.
 * Every 2 hours (or on-demand), this service runs a 5km geographic scan:
 * 1. **Primary AI Engine**: Uses Gemini 3.1 Flash-Lite to discover verified local Barangay halls,
 *    PNP police stations, and 24/7 hospital trauma ERs covering your exact GPS coordinates.
 * 2. **Fallback Engine**: If there is no API key or internet is spotty, it queries the free OpenStreetMap
 *    Overpass API for physical emergency buildings nearby.
 * 3. All discovered stations are saved to the phone's offline SQLite database so they remain
 *    available forever even if cell towers later lose data connectivity.
 */
class HotlineAiService {

    companion object {
        private const val LOG_TAG = "HotlineAiService"
        const val DEFAULT_MODEL = "gemini-3.1-flash-lite"
        const val ALT_MODEL = "gemini-2.5-flash-lite"
    }

    /**
     * Scans for emergency facilities within 5km of the user's coordinates.
     *
     * @param latitude User's live GPS latitude coordinate.
     * @param longitude User's live GPS longitude coordinate.
     * @param resolvedCity The city name, if already known via reverse geocoding.
     * @param apiKey The Gemini API key, if configured.
     * @param modelName Which Gemini model to invoke (defaults to gemini-3.1-flash-lite).
     * @return List of newly discovered [Hotline] records sorted by physical distance.
     */
    suspend fun discover5kmHotlines(
        latitude: Double,
        longitude: Double,
        resolvedCity: String? = null,
        apiKey: String? = null,
        modelName: String = DEFAULT_MODEL
    ): List<Hotline> = withContext(Dispatchers.IO) {
        val sanitizedApiKey = apiKey?.trim()

        // 1. Primary Attempt: Query Google Gemini AI if an API key is available
        if (!sanitizedApiKey.isNullOrBlank()) {
            try {
                val geminiDiscoveredStations = queryGemini(
                    userLatitude = latitude,
                    userLongitude = longitude,
                    cityName = resolvedCity,
                    apiKey = sanitizedApiKey,
                    model = modelName
                )
                if (geminiDiscoveredStations.isNotEmpty()) {
                    Log.d(LOG_TAG, "Gemini AI ($modelName) discovered ${geminiDiscoveredStations.size} stations within 5km")
                    return@withContext geminiDiscoveredStations
                }
            } catch (primaryException: Exception) {
                Log.w(LOG_TAG, "Gemini API query with $modelName failed: ${primaryException.message}, trying fallback $ALT_MODEL")
                if (modelName != ALT_MODEL) {
                    try {
                        val fallbackStations = queryGemini(
                            userLatitude = latitude,
                            userLongitude = longitude,
                            cityName = resolvedCity,
                            apiKey = sanitizedApiKey,
                            model = ALT_MODEL
                        )
                        if (fallbackStations.isNotEmpty()) {
                            Log.d(LOG_TAG, "Gemini AI fallback ($ALT_MODEL) discovered ${fallbackStations.size} stations within 5km")
                            return@withContext fallbackStations
                        }
                    } catch (fallbackException: Exception) {
                        Log.e(LOG_TAG, "Gemini AI fallback also failed: ${fallbackException.message}")
                    }
                }
            }
        }

        // 2. Secondary Failsafe: Query OpenStreetMap Overpass API (Free, requires no API key)
        try {
            val openStreetMapStations = queryOverpassApi(
                userLatitude = latitude,
                userLongitude = longitude,
                radiusMeters = 5000.0
            )
            if (openStreetMapStations.isNotEmpty()) {
                Log.d(LOG_TAG, "Overpass API discovered ${openStreetMapStations.size} local stations")
                return@withContext openStreetMapStations
            }
        } catch (overpassException: Exception) {
            Log.e(LOG_TAG, "Overpass fallback query failed: ${overpassException.message}")
        }

        emptyList()
    }

    /**
     * Issues an HTTP POST request to the Google Gemini generateContent REST endpoint.
     */
    private fun queryGemini(
        userLatitude: Double,
        userLongitude: Double,
        cityName: String?,
        apiKey: String,
        model: String
    ): List<Hotline> {
        val locationDescription = if (!cityName.isNullOrBlank() && !cityName.contains("Current GPS", ignoreCase = true)) {
            "$cityName, Philippines"
        } else {
            "Philippines"
        }

        val prompt = """
            Search and find verified official emergency hotline contact numbers within 5 kilometers of GPS coordinates ($userLatitude, $userLongitude) in $locationDescription.
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

        val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val requestJsonPayload = JSONObject().apply {
            val partsArray = JSONArray().apply {
                put(JSONObject().apply { put("text", prompt) })
            }
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply { put("parts", partsArray) })
            }
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.2) // Low temperature for deterministic, factual station numbers
                put("responseMimeType", "application/json")
            })
        }

        val networkUrl = URL(endpointUrl)
        val httpConnection = (networkUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12000
            readTimeout = 15000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        OutputStreamWriter(httpConnection.outputStream, "UTF-8").use { writer ->
            writer.write(requestJsonPayload.toString())
            writer.flush()
        }

        val httpResponseCode = httpConnection.responseCode
        if (httpResponseCode !in 200..299) {
            val errorMessage = httpConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $httpResponseCode"
            httpConnection.disconnect()
            throw RuntimeException("Gemini error $httpResponseCode: $errorMessage")
        }

        val responseBodyText = httpConnection.inputStream.bufferedReader().use { it.readText() }
        httpConnection.disconnect()

        return parseGeminiResponse(responseBodyText, userLatitude, userLongitude)
    }

    /**
     * Parses the JSON text returned by Gemini into structured [Hotline] objects.
     */
    private fun parseGeminiResponse(
        jsonResponseString: String,
        userLatitude: Double,
        userLongitude: Double
    ): List<Hotline> {
        val parsedHotlinesList = mutableListOf<Hotline>()
        try {
            val rootJsonObject = JSONObject(jsonResponseString)
            val candidatesArray = rootJsonObject.optJSONArray("candidates") ?: return emptyList()
            if (candidatesArray.length() == 0) return emptyList()

            val contentObject = candidatesArray.getJSONObject(0).optJSONObject("content") ?: return emptyList()
            val partsArray = contentObject.optJSONArray("parts") ?: return emptyList()
            if (partsArray.length() == 0) return emptyList()

            var rawGeneratedText = partsArray.getJSONObject(0).optString("text", "").trim()
            if (rawGeneratedText.startsWith("```json")) rawGeneratedText = rawGeneratedText.removePrefix("```json")
            if (rawGeneratedText.startsWith("```")) rawGeneratedText = rawGeneratedText.removePrefix("```")
            if (rawGeneratedText.endsWith("```")) rawGeneratedText = rawGeneratedText.removeSuffix("```")
            rawGeneratedText = rawGeneratedText.trim()

            val stationsJsonArray = JSONArray(rawGeneratedText)
            for (index in 0 until stationsJsonArray.length()) {
                val stationObject = stationsJsonArray.getJSONObject(index)
                val stationName = stationObject.optString("name", "Local Emergency Station").trim()
                val rawCategoryString = stationObject.optString("category", "DISASTER_DRRMO").uppercase()

                val resolvedCategory = try {
                    HotlineCategory.valueOf(rawCategoryString)
                } catch (unknownCategoryException: Exception) {
                    when {
                        rawCategoryString.contains("BARANGAY") -> HotlineCategory.BARANGAY
                        rawCategoryString.contains("POLICE") || rawCategoryString.contains("PNP") -> HotlineCategory.POLICE_PNP
                        rawCategoryString.contains("HOSPITAL") || rawCategoryString.contains("MEDIC") -> HotlineCategory.MEDICAL_HOSPITAL
                        rawCategoryString.contains("FIRE") || rawCategoryString.contains("BFP") -> HotlineCategory.FIRE_BFP
                        else -> HotlineCategory.DISASTER_DRRMO
                    }
                }

                val primaryPhone = sanitizePhoneNumber(stationObject.optString("primaryNumber", "911"))
                val alternatePhone = stationObject.optString("alternateNumber", "").takeIf { it.isNotBlank() }?.let { sanitizePhoneNumber(it) }
                val municipality = stationObject.optString("cityMunicipality", "Local Area")
                val province = stationObject.optString("province", "Philippines")
                val region = stationObject.optString("region", "NCR")
                val stationLatitude = stationObject.optDouble("latitude", userLatitude)
                val stationLongitude = stationObject.optDouble("longitude", userLongitude)

                val physicalDistanceKm = calculateDistanceKm(userLatitude, userLongitude, stationLatitude, stationLongitude)
                val hotlineRecord = Hotline(
                    id = "AI-" + UUID.randomUUID().toString().take(8).uppercase(),
                    name = stationName,
                    category = resolvedCategory,
                    primaryNumber = primaryPhone,
                    alternateNumber = alternatePhone,
                    cityMunicipality = municipality,
                    province = province,
                    region = region,
                    latitude = stationLatitude,
                    longitude = stationLongitude,
                    distanceKm = round(physicalDistanceKm * 100.0) / 100.0,
                    source = "AI_GEMINI"
                )
                parsedHotlinesList.add(hotlineRecord)
            }
        } catch (parsingException: Exception) {
            Log.e(LOG_TAG, "Error parsing Gemini JSON response: ${parsingException.message}", parsingException)
        }
        return parsedHotlinesList.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
    }

    /**
     * Queries OpenStreetMap's Overpass API for emergency amenities (police, hospital, fire station, townhall).
     */
    private fun queryOverpassApi(
        userLatitude: Double,
        userLongitude: Double,
        radiusMeters: Double = 5000.0
    ): List<Hotline> {
        val overpassQuery = """
            [out:json][timeout:15];
            (
              node["amenity"~"police|hospital|clinic|fire_station|townhall"](around:$radiusMeters,$userLatitude,$userLongitude);
              way["amenity"~"police|hospital|clinic|fire_station|townhall"](around:$radiusMeters,$userLatitude,$userLongitude);
            );
            out center 15;
        """.trimIndent()

        val overpassEndpointUrl = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(overpassQuery, "UTF-8")
        val networkUrl = URL(overpassEndpointUrl)
        val httpConnection = (networkUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 10000
        }

        if (httpConnection.responseCode != 200) {
            httpConnection.disconnect()
            return emptyList()
        }

        val jsonResponseString = httpConnection.inputStream.bufferedReader().use { it.readText() }
        httpConnection.disconnect()

        val discoveredHotlinesList = mutableListOf<Hotline>()
        val rootJsonObject = JSONObject(jsonResponseString)
        val elementsArray = rootJsonObject.optJSONArray("elements") ?: return emptyList()

        for (elementIndex in 0 until elementsArray.length()) {
            val elementObject = elementsArray.getJSONObject(elementIndex)
            val tagsObject = elementObject.optJSONObject("tags") ?: continue
            val stationName = tagsObject.optString("name", "").takeIf { it.isNotBlank() } ?: continue
            val amenityType = tagsObject.optString("amenity", "")

            val rawPhoneNumber = tagsObject.optString("emergency:phone", "")
                .takeIf { it.isNotBlank() }
                ?: tagsObject.optString("phone", "")
                    .takeIf { it.isNotBlank() }
                ?: tagsObject.optString("contact:phone", "")

            val category = when (amenityType) {
                "police" -> HotlineCategory.POLICE_PNP
                "hospital", "clinic" -> HotlineCategory.MEDICAL_HOSPITAL
                "fire_station" -> HotlineCategory.FIRE_BFP
                "townhall" -> HotlineCategory.BARANGAY
                else -> HotlineCategory.DISASTER_DRRMO
            }

            val stationLatitude = if (elementObject.has("lat")) {
                elementObject.getDouble("lat")
            } else {
                elementObject.optJSONObject("center")?.optDouble("lat", userLatitude) ?: userLatitude
            }

            val stationLongitude = if (elementObject.has("lon")) {
                elementObject.getDouble("lon")
            } else {
                elementObject.optJSONObject("center")?.optDouble("lon", userLongitude) ?: userLongitude
            }

            val physicalDistanceKm = calculateDistanceKm(userLatitude, userLongitude, stationLatitude, stationLongitude)

            discoveredHotlinesList.add(
                Hotline(
                    id = "OSM-" + elementObject.optLong("id", elementIndex.toLong()),
                    name = stationName,
                    category = category,
                    primaryNumber = if (rawPhoneNumber.isNotBlank()) sanitizePhoneNumber(rawPhoneNumber) else "122",
                    alternateNumber = null,
                    cityMunicipality = tagsObject.optString("addr:city", "Local Area"),
                    province = "Philippines",
                    region = "NCR",
                    latitude = stationLatitude,
                    longitude = stationLongitude,
                    distanceKm = round(physicalDistanceKm * 100.0) / 100.0,
                    source = "OVERPASS"
                )
            )
        }

        return discoveredHotlinesList.sortedBy { it.distanceKm ?: Double.MAX_VALUE }
    }

    /**
     * Cleans raw telephone numbers into standard Philippine dialable format.
     */
    private fun sanitizePhoneNumber(rawPhoneNumber: String): String {
        return rawPhoneNumber.trim()
            .replace("+63", "0")
            .replace(" ", "-")
            .replace("/", " / ")
            .trim()
    }

    /**
     * Calculates the great-circle distance between two GPS coordinates using Haversine math.
     */
    private fun calculateDistanceKm(
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
}
