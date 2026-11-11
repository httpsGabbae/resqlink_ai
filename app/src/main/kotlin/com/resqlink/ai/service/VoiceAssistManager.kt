package com.resqlink.ai.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.resqlink.ai.data.local.AppConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

/**
 * # VoiceAssistManager
 *
 * Coordinates in-call automated voice broadcasting, Q&A synthesis, and patient handover over speakerphone.
 *
 * Ordinary Human Overview:
 * If an unconscious victim is in a car crash, their phone can place a call to 911.
 * But how does an unconscious person speak to the dispatcher?
 *
 * This manager is the automated voice:
 * 1. **Forces Speakerphone to Max Volume**: So the dispatcher can hear the phone clearly from across the vehicle.
 * 2. **3-Cycle Emergency Briefing**: Speaks a calm, clear bilingual (English + Filipino) announcement
 *    identifying the victim, their exact location, blood type, and severe allergies.
 * 3. **Multi-Tier Audio Engine**:
 *    - Primary: ElevenLabs Multilingual HD TTS (natural, compassionate voice).
 *    - Secondary: Gemini 2.0 Flash Audio stream.
 *    - Offline Fallback: Built-in Android `TextToSpeech` (100% offline, requires 0 internet).
 * 4. **Live Patient Handover**: If the victim regains consciousness and groans or speaks,
 *    the manager announces: "The patient is now conscious, handing over the line," and yields control.
 */
class VoiceAssistManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        const val DEFAULT_VOICE_ID = "EXAVITQu4vr4xnSDxMaL" // Sarah (Soft, Reassuring Tagalog & English Bilingual)
    }

    private val systemAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var nativeTextToSpeech: TextToSpeech? = null
    private var isTextToSpeechInitialized = false
    private var activeMediaPlayer: MediaPlayer? = null

    private val managerCoroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var activeBroadcastCoroutineJob: Job? = null
    private var isBroadcastStopped = false

    var onTranscriptUpdate: ((transcript: String, cycle: Int) -> Unit)? = null
    var onHandoverFinished: (() -> Unit)? = null

    init {
        nativeTextToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val languageResult = nativeTextToSpeech?.setLanguage(Locale.US)
            if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceAssistManager", "US English not supported, falling back to system default")
                nativeTextToSpeech?.language = Locale.getDefault()
            }
            nativeTextToSpeech?.setSpeechRate(0.92f) // Deliberate pace for emergency clarity
            nativeTextToSpeech?.setPitch(1.0f)

            val speechAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            nativeTextToSpeech?.setAudioAttributes(speechAudioAttributes)
            isTextToSpeechInitialized = true
            Log.i("VoiceAssistManager", "TextToSpeech successfully initialized for Emergency Voice Assist")
        } else {
            Log.e("VoiceAssistManager", "TextToSpeech initialization failed with status $status")
        }
    }

    /**
     * Forces device audio to speakerphone at maximum volume so synthetic audio
     * pipes directly into the call microphone for the dispatcher and on-scene helpers.
     */
    fun forceSpeakerphoneMaxVolume() {
        try {
            systemAudioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            systemAudioManager.isSpeakerphoneOn = true
            val maxCallVolume = systemAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            systemAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxCallVolume, 0)
            val maxMediaVolume = systemAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            systemAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMediaVolume, 0)
        } catch (exception: Exception) {
            Log.e("VoiceAssistManager", "Failed to force speakerphone: ${exception.message}", exception)
        }
    }

    /**
     * Builds the bilingual English + Filipino emergency briefing.
     * Precise and natural for dispatchers: uses exact location/address, strips raw decimal
     * GPS numbers, and ensures "Google" is never added or spoken in the patient name.
     */
    fun buildEmergencyScript(
        userName: String,
        address: String,
        latitude: Double,
        longitude: Double,
        bloodType: String,
        allergies: String
    ): String {
        // Sanitize patient name: ensure "Google" is never added or spoken in the name
        val sanitizedPatientName = userName
            .replace("(?i)google\\s*(user|account)?".toRegex(), "")
            .trim()
            .ifBlank { "Patient" }

        // Precise location / exact position without rattling off raw GPS numbers
        val sanitizedLocationName = address
            .replace("(?i)location\\s*coordinates.*".toRegex(), "")
            .replace("(?i)lat.*long.*".toRegex(), "")
            .replace("(?i)google.*".toRegex(), "")
            .trim()
            .ifBlank { "Metro Manila area" }

        val bloodTypeDescription = bloodType.ifBlank { "Not documented" }
        val allergiesDescription = allergies.ifBlank { "No known allergies" }

        return "Emergency alert from ResQLink AI. Emergency alert. " +
                "The patient is currently unresponsive following an accident. " +
                "Exact location: $sanitizedLocationName. " +
                "Patient name: $sanitizedPatientName. " +
                "Blood type: $bloodTypeDescription. " +
                "Known allergies: $allergiesDescription. " +
                "Mangyaring magpadala agad ng ambulansya o rescuer sa nasabing lokasyon. " +
                "The line is open on speakerphone. ResQLink AI is actively monitoring the patient."
    }

    /**
     * Handover speech spoken to the dispatcher when user consciousness or speech is detected.
     */
    fun buildHandoverScript(): String {
        return "Attention dispatcher: The patient is now conscious and speaking directly to you. " +
                "Handing over the line now. Narito na po ang pasyente."
    }

    /**
     * Synthesizes audio using ElevenLabs REST API. Returns the downloaded MP3 File if successful.
     */
    private suspend fun synthesizeWithElevenLabs(
        text: String,
        apiKey: String,
        voiceId: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            val resolvedVoiceId = voiceId.ifBlank { DEFAULT_VOICE_ID }
            val elevenLabsEndpointUrl = URL("https://api.elevenlabs.io/v1/text-to-speech/$resolvedVoiceId")
            val httpConnection = (elevenLabsEndpointUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("xi-api-key", apiKey.trim())
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "audio/mpeg")
                connectTimeout = 8000
                readTimeout = 15000
                doOutput = true
            }

            val requestJsonPayload = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.65)
                    put("similarity_boost", 0.85)
                    put("style", 0.0)
                    put("use_speaker_boost", true)
                })
            }.toString()

            httpConnection.outputStream.use { outputStream ->
                outputStream.write(requestJsonPayload.toByteArray(Charsets.UTF_8))
            }

            if (httpConnection.responseCode in 200..299) {
                val tempAudioFile = File(context.cacheDir, "elevenlabs_sos_${System.currentTimeMillis()}.mp3")
                httpConnection.inputStream.use { inputStream ->
                    tempAudioFile.outputStream().use { fileOutputStream ->
                        inputStream.copyTo(fileOutputStream)
                    }
                }
                Log.i("VoiceAssistManager", "ElevenLabs audio synthesized successfully (${tempAudioFile.length()} bytes)")
                tempAudioFile
            } else {
                Log.w("VoiceAssistManager", "ElevenLabs returned HTTP ${httpConnection.responseCode}, will fall back to native TTS")
                null
            }
        } catch (exception: Exception) {
            Log.w("VoiceAssistManager", "ElevenLabs synthesis failed: ${exception.message}, falling back to native TTS")
            null
        }
    }

    /**
     * Synthesizes voice audio using Google Gemini 2.0 Flash native audio output.
     */
    private suspend fun synthesizeWithGemini(
        text: String,
        apiKey: String,
        voiceName: String = "Aoede"
    ): File? = withContext(Dispatchers.IO) {
        try {
            val geminiEndpointUrl = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey.trim()}")
            val httpConnection = (geminiEndpointUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 8000
                readTimeout = 15000
                doOutput = true
            }

            val resolvedVoiceName = voiceName.ifBlank { "Aoede" }
            val requestPayload = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Speak this emergency announcement in a clear, urgent, lifelike responder voice: $text")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", org.json.JSONArray().apply {
                        put("AUDIO")
                    })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", resolvedVoiceName)
                            })
                        })
                    })
                })
            }.toString()

            httpConnection.outputStream.use { outputStream ->
                outputStream.write(requestPayload.toByteArray(Charsets.UTF_8))
            }

            if (httpConnection.responseCode in 200..299) {
                val responseString = httpConnection.inputStream.bufferedReader().use { it.readText() }
                val rootJson = JSONObject(responseString)
                val candidatesArray = rootJson.optJSONArray("candidates")
                val contentPartsArray = candidatesArray?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                val inlineDataObject = contentPartsArray?.optJSONObject(0)?.optJSONObject("inlineData")
                val base64EncodedData = inlineDataObject?.optString("data")
                val audioMimeType = inlineDataObject?.optString("mimeType") ?: "audio/wav"

                if (!base64EncodedData.isNullOrBlank()) {
                    val rawAudioBytes = android.util.Base64.decode(base64EncodedData, android.util.Base64.DEFAULT)
                    val processedAudioBytes = if (audioMimeType.contains("pcm", ignoreCase = true)) {
                        addWavHeader(rawAudioBytes, sampleRate = 24000, channels = 1)
                    } else {
                        rawAudioBytes
                    }
                    val fileExtension = if (audioMimeType.contains("mp3", ignoreCase = true)) "mp3" else "wav"
                    val tempAudioFile = File(context.cacheDir, "gemini_voice_${System.currentTimeMillis()}.$fileExtension")
                    tempAudioFile.writeBytes(processedAudioBytes)
                    Log.i("VoiceAssistManager", "Gemini Voice synthesized (${tempAudioFile.length()} bytes)")
                    tempAudioFile
                } else {
                    null
                }
            } else {
                Log.w("VoiceAssistManager", "Gemini Voice returned HTTP ${httpConnection.responseCode}")
                null
            }
        } catch (exception: Exception) {
            Log.w("VoiceAssistManager", "Gemini Voice synthesis error: ${exception.message}")
            null
        }
    }

    private fun addWavHeader(pcmBytes: ByteArray, sampleRate: Int = 24000, channels: Int = 1): ByteArray {
        val totalAudioLength = pcmBytes.size
        val totalDataLength = totalAudioLength + 36
        val byteRate = sampleRate * channels * 2
        val wavHeader = ByteArray(44)
        wavHeader[0] = 'R'.code.toByte()
        wavHeader[1] = 'I'.code.toByte()
        wavHeader[2] = 'F'.code.toByte()
        wavHeader[3] = 'F'.code.toByte()
        wavHeader[4] = (totalDataLength and 0xff).toByte()
        wavHeader[5] = ((totalDataLength shr 8) and 0xff).toByte()
        wavHeader[6] = ((totalDataLength shr 16) and 0xff).toByte()
        wavHeader[7] = ((totalDataLength shr 24) and 0xff).toByte()
        wavHeader[8] = 'W'.code.toByte()
        wavHeader[9] = 'A'.code.toByte()
        wavHeader[10] = 'V'.code.toByte()
        wavHeader[11] = 'E'.code.toByte()
        wavHeader[12] = 'f'.code.toByte()
        wavHeader[13] = 'm'.code.toByte()
        wavHeader[14] = 't'.code.toByte()
        wavHeader[15] = ' '.code.toByte()
        wavHeader[16] = 16
        wavHeader[17] = 0
        wavHeader[18] = 0
        wavHeader[19] = 0
        wavHeader[20] = 1 // PCM
        wavHeader[21] = 0
        wavHeader[22] = channels.toByte()
        wavHeader[23] = 0
        wavHeader[24] = (sampleRate and 0xff).toByte()
        wavHeader[25] = ((sampleRate shr 8) and 0xff).toByte()
        wavHeader[26] = ((sampleRate shr 16) and 0xff).toByte()
        wavHeader[27] = ((sampleRate shr 24) and 0xff).toByte()
        wavHeader[28] = (byteRate and 0xff).toByte()
        wavHeader[29] = ((byteRate shr 8) and 0xff).toByte()
        wavHeader[30] = ((byteRate shr 16) and 0xff).toByte()
        wavHeader[31] = ((byteRate shr 24) and 0xff).toByte()
        wavHeader[32] = (channels * 2).toByte()
        wavHeader[33] = 0
        wavHeader[34] = 16 // 16-bit
        wavHeader[35] = 0
        wavHeader[36] = 'd'.code.toByte()
        wavHeader[37] = 'a'.code.toByte()
        wavHeader[38] = 't'.code.toByte()
        wavHeader[39] = 'a'.code.toByte()
        wavHeader[40] = (totalAudioLength and 0xff).toByte()
        wavHeader[41] = ((totalAudioLength shr 8) and 0xff).toByte()
        wavHeader[42] = ((totalAudioLength shr 16) and 0xff).toByte()
        wavHeader[43] = ((totalAudioLength shr 24) and 0xff).toByte()
        return wavHeader + pcmBytes
    }

    /**
     * Plays an audio file through MediaPlayer on the voice communication speakerphone stream.
     */
    private suspend fun playAudioFile(file: File) = suspendCancellableCoroutine<Unit> { continuation ->
        try {
            activeMediaPlayer?.release()
            activeMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    if (continuation.isActive) continuation.resume(Unit)
                }
                setOnErrorListener { _, errorCode, extraCode ->
                    Log.e("VoiceAssistManager", "MediaPlayer error: $errorCode, $extraCode")
                    if (continuation.isActive) continuation.resume(Unit)
                    true
                }
                prepare()
                start()
            }
            continuation.invokeOnCancellation {
                try {
                    activeMediaPlayer?.stop()
                    activeMediaPlayer?.release()
                    activeMediaPlayer = null
                } catch (ignored: Exception) {}
            }
        } catch (exception: Exception) {
            Log.e("VoiceAssistManager", "Failed to play audio file: ${exception.message}")
            if (continuation.isActive) continuation.resume(Unit)
        }
    }

    /**
     * Starts the 3-cycle emergency broadcast loop with a 5-second pause between cycles.
     * Uses ElevenLabs when an API key is available and online, otherwise seamlessly
     * falls back to Android's built-in on-device TextToSpeech.
     */
    fun startEmergencyBroadcast(
        userName: String,
        address: String,
        latitude: Double,
        longitude: Double,
        bloodType: String,
        allergies: String,
        elevenLabsApiKey: String? = null,
        elevenLabsVoiceId: String? = null,
        geminiApiKey: String? = null,
        geminiVoiceName: String = "Aoede"
    ) {
        isBroadcastStopped = false
        forceSpeakerphoneMaxVolume()

        activeBroadcastCoroutineJob?.cancel()
        activeBroadcastCoroutineJob = managerCoroutineScope.launch {
            val emergencyBriefingScript = buildEmergencyScript(userName, address, latitude, longitude, bloodType, allergies)

            // 1. Attempt ElevenLabs synthesis first, then Gemini Voice, then Android TTS
            var synthesizedAudioFile: File? = null
            var activeVoiceEngineLabel = "On-Device Android TTS"

            val resolvedElevenLabsKey = elevenLabsApiKey?.takeIf { it.isNotBlank() } ?: AppConfig.ELEVENLABS_API_KEY
            val resolvedVoiceId = elevenLabsVoiceId?.takeIf { it.isNotBlank() } ?: DEFAULT_VOICE_ID

            if (resolvedElevenLabsKey.isNotBlank()) {
                onTranscriptUpdate?.invoke("[Contacting ElevenLabs Multilingual Engine (Sarah Soft Voice)...]\n$emergencyBriefingScript", 1)
                synthesizedAudioFile = synthesizeWithElevenLabs(
                    text = emergencyBriefingScript,
                    apiKey = resolvedElevenLabsKey,
                    voiceId = resolvedVoiceId
                )
                if (synthesizedAudioFile != null && synthesizedAudioFile.exists()) {
                    activeVoiceEngineLabel = "ElevenLabs HD Multilingual (Sarah - Soft Voice)"
                }
            } else if (!geminiApiKey.isNullOrBlank()) {
                onTranscriptUpdate?.invoke("[Contacting Gemini Voice Engine ($geminiVoiceName)...]\n$emergencyBriefingScript", 1)
                synthesizedAudioFile = synthesizeWithGemini(
                    text = emergencyBriefingScript,
                    apiKey = geminiApiKey,
                    voiceName = geminiVoiceName
                )
                if (synthesizedAudioFile != null && synthesizedAudioFile.exists()) {
                    activeVoiceEngineLabel = "Gemini 2.0 Flash Audio ($geminiVoiceName)"
                }
            }

            val hasSyntheticAudioFile = synthesizedAudioFile != null && synthesizedAudioFile.exists()

            // Wait for native TTS initialization if no cloud audio file was generated
            if (!hasSyntheticAudioFile) {
                var waitAttempts = 0
                while (!isTextToSpeechInitialized && waitAttempts < 20) {
                    delay(100)
                    waitAttempts++
                }
            }

            // Speak 3 cycles with a 5-second silence between each cycle
            for (cycleNumber in 1..3) {
                if (isBroadcastStopped) break
                val transcriptDisplayText = "[Voice: $activeVoiceEngineLabel]\n$emergencyBriefingScript"
                onTranscriptUpdate?.invoke(transcriptDisplayText, cycleNumber)

                // Play the audio
                if (hasSyntheticAudioFile) {
                    playAudioFile(synthesizedAudioFile)
                } else {
                    speakUtterance(emergencyBriefingScript, utteranceId = "EMERGENCY_CYCLE_$cycleNumber")
                }

                if (cycleNumber < 3 && !isBroadcastStopped) {
                    onTranscriptUpdate?.invoke("[Pausing 5s before repeating broadcast...]", cycleNumber)
                    delay(5000L)
                }
            }

            if (!isBroadcastStopped) {
                onTranscriptUpdate?.invoke("[Broadcast complete. Line remains open on speakerphone.]", 3)
            }
        }
    }

    /**
     * Speaks a direct synthesized response to a dispatcher's follow-up question.
     * Uses ElevenLabs when configured, Gemini Voice if available, with seamless on-device TTS fallback.
     */
    fun speakDispatcherAnswer(
        answerText: String,
        elevenLabsApiKey: String? = null,
        elevenLabsVoiceId: String? = null,
        geminiApiKey: String? = null,
        geminiVoiceName: String = "Aoede",
        onComplete: (() -> Unit)? = null
    ) {
        activeBroadcastCoroutineJob?.cancel()
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (ignored: Exception) {}
        nativeTextToSpeech?.stop()

        managerCoroutineScope.launch {
            val resolvedElevenLabsKey = elevenLabsApiKey?.takeIf { it.isNotBlank() } ?: AppConfig.ELEVENLABS_API_KEY
            val resolvedVoiceId = elevenLabsVoiceId?.takeIf { it.isNotBlank() } ?: DEFAULT_VOICE_ID

            var synthesizedVoiceFile: File? = null
            if (resolvedElevenLabsKey.isNotBlank()) {
                synthesizedVoiceFile = synthesizeWithElevenLabs(
                    text = answerText,
                    apiKey = resolvedElevenLabsKey,
                    voiceId = resolvedVoiceId
                )
            } else if (!geminiApiKey.isNullOrBlank()) {
                synthesizedVoiceFile = synthesizeWithGemini(
                    text = answerText,
                    apiKey = geminiApiKey,
                    voiceName = geminiVoiceName
                )
            }

            if (synthesizedVoiceFile != null && synthesizedVoiceFile.exists()) {
                playAudioFile(synthesizedVoiceFile)
            } else {
                speakUtterance(answerText, utteranceId = "DISPATCHER_QA_${System.currentTimeMillis()}")
            }
            onComplete?.invoke()
        }
    }

    /**
     * Gracefully announces the handover to the dispatcher using Sarah soft voice, then releases TTS & audio players.
     */
    fun performHandover(
        elevenLabsApiKey: String? = null,
        elevenLabsVoiceId: String? = null,
        onComplete: () -> Unit
    ) {
        isBroadcastStopped = true
        activeBroadcastCoroutineJob?.cancel()
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (ignored: Exception) {}
        nativeTextToSpeech?.stop()

        val handoverScriptText = buildHandoverScript()
        onTranscriptUpdate?.invoke(handoverScriptText, 0)

        managerCoroutineScope.launch {
            val resolvedElevenLabsKey = elevenLabsApiKey?.takeIf { it.isNotBlank() } ?: AppConfig.ELEVENLABS_API_KEY
            val resolvedVoiceId = elevenLabsVoiceId?.takeIf { it.isNotBlank() } ?: DEFAULT_VOICE_ID

            var synthesizedVoiceFile: File? = null
            if (resolvedElevenLabsKey.isNotBlank()) {
                synthesizedVoiceFile = synthesizeWithElevenLabs(
                    text = handoverScriptText,
                    apiKey = resolvedElevenLabsKey,
                    voiceId = resolvedVoiceId
                )
            }

            if (synthesizedVoiceFile != null && synthesizedVoiceFile.exists()) {
                playAudioFile(synthesizedVoiceFile)
            } else {
                speakUtterance(handoverScriptText, utteranceId = "HANDOVER_UTTERANCE")
            }
            delay(300L)
            onComplete()
            onHandoverFinished?.invoke()
        }
    }

    private suspend fun speakUtterance(text: String, utteranceId: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val utteranceBundleParams = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        nativeTextToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (continuation.isActive) continuation.resume(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        })

        val speechQueueResult = nativeTextToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, utteranceBundleParams, utteranceId)
        if (speechQueueResult == TextToSpeech.ERROR) {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }

    /**
     * Halts speech playback and cancels background coroutines.
     */
    fun stop() {
        isBroadcastStopped = true
        activeBroadcastCoroutineJob?.cancel()
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (ignored: Exception) {}
        nativeTextToSpeech?.stop()
    }

    /**
     * Completely shuts down TTS engines, restores system audio routing, and releases audio resources.
     */
    fun release() {
        stop()
        managerCoroutineScope.cancel()
        nativeTextToSpeech?.shutdown()
        nativeTextToSpeech = null
        try {
            @Suppress("DEPRECATION")
            systemAudioManager.isSpeakerphoneOn = false
            systemAudioManager.mode = AudioManager.MODE_NORMAL
        } catch (exception: Exception) {
            Log.e("VoiceAssistManager", "Error restoring audio mode: ${exception.message}")
        }
    }
}
