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

class VoiceAssistManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        const val DEFAULT_VOICE_ID = "EXAVITQu4vr4xnSDxMaL" // Sarah (Soft, Reassuring Tagalog & English Bilingual)
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var mediaPlayer: MediaPlayer? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var broadcastJob: Job? = null
    private var isStopped = false

    var onTranscriptUpdate: ((transcript: String, cycle: Int) -> Unit)? = null
    var onHandoverFinished: (() -> Unit)? = null

    init {
        textToSpeech = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("VoiceAssistManager", "US English not supported, falling back to default")
                textToSpeech?.language = Locale.getDefault()
            }
            textToSpeech?.setSpeechRate(0.92f) // Deliberate pace for emergency clarity
            textToSpeech?.setPitch(1.0f)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            textToSpeech?.setAudioAttributes(audioAttributes)
            isTtsInitialized = true
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
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = true
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
        } catch (e: Exception) {
            Log.e("VoiceAssistManager", "Failed to force speakerphone: ${e.message}", e)
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
        val cleanName = userName
            .replace("(?i)google\\s*(user|account)?".toRegex(), "")
            .trim()
            .ifBlank { "Patient" }

        // Precise location / exact position without rattling off raw GPS numbers
        val cleanLocation = address
            .replace("(?i)location\\s*coordinates.*".toRegex(), "")
            .replace("(?i)lat.*long.*".toRegex(), "")
            .replace("(?i)google.*".toRegex(), "")
            .trim()
            .ifBlank { "Metro Manila area" }

        val cleanBlood = bloodType.ifBlank { "Not documented" }
        val cleanAllergies = allergies.ifBlank { "No known allergies" }

        return "Emergency alert from ResQLink AI. Emergency alert. " +
                "The patient is currently unresponsive following an accident. " +
                "Exact location: $cleanLocation. " +
                "Patient name: $cleanName. " +
                "Blood type: $cleanBlood. " +
                "Known allergies: $cleanAllergies. " +
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
            val resolvedVoice = voiceId.ifBlank { DEFAULT_VOICE_ID }
            val url = URL("https://api.elevenlabs.io/v1/text-to-speech/$resolvedVoice")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("xi-api-key", apiKey.trim())
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "audio/mpeg")
                connectTimeout = 8000
                readTimeout = 15000
                doOutput = true
            }

            val json = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.65)
                    put("similarity_boost", 0.85)
                    put("style", 0.0)
                    put("use_speaker_boost", true)
                })
            }.toString()

            conn.outputStream.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                val tempFile = File(context.cacheDir, "elevenlabs_sos_${System.currentTimeMillis()}.mp3")
                conn.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i("VoiceAssistManager", "ElevenLabs audio synthesized successfully (${tempFile.length()} bytes)")
                tempFile
            } else {
                Log.w("VoiceAssistManager", "ElevenLabs returned HTTP ${conn.responseCode}, will fall back to native TTS")
                null
            }
        } catch (e: Exception) {
            Log.w("VoiceAssistManager", "ElevenLabs synthesis failed: ${e.message}, falling back to native TTS")
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
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=${apiKey.trim()}")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 8000
                readTimeout = 15000
                doOutput = true
            }

            val resolvedVoice = voiceName.ifBlank { "Aoede" }
            val payload = JSONObject().apply {
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
                                put("voiceName", resolvedVoice)
                            })
                        })
                    })
                })
            }.toString()

            conn.outputStream.use { os ->
                os.write(payload.toByteArray(Charsets.UTF_8))
            }

            if (conn.responseCode in 200..299) {
                val respStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(respStr)
                val candidates = root.optJSONArray("candidates")
                val parts = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                val inlineData = parts?.optJSONObject(0)?.optJSONObject("inlineData")
                val base64Data = inlineData?.optString("data")
                val mimeType = inlineData?.optString("mimeType") ?: "audio/wav"

                if (!base64Data.isNullOrBlank()) {
                    val rawBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val processedBytes = if (mimeType.contains("pcm", ignoreCase = true)) {
                        addWavHeader(rawBytes, sampleRate = 24000, channels = 1)
                    } else {
                        rawBytes
                    }
                    val ext = if (mimeType.contains("mp3", ignoreCase = true)) "mp3" else "wav"
                    val tempFile = File(context.cacheDir, "gemini_voice_${System.currentTimeMillis()}.$ext")
                    tempFile.writeBytes(processedBytes)
                    Log.i("VoiceAssistManager", "Gemini Voice synthesized (${tempFile.length()} bytes)")
                    tempFile
                } else {
                    null
                }
            } else {
                Log.w("VoiceAssistManager", "Gemini Voice returned HTTP ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.w("VoiceAssistManager", "Gemini Voice synthesis error: ${e.message}")
            null
        }
    }

    private fun addWavHeader(pcmBytes: ByteArray, sampleRate: Int = 24000, channels: Int = 1): ByteArray {
        val totalAudioLen = pcmBytes.size
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * 2
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * 2).toByte()
        header[33] = 0
        header[34] = 16 // 16-bit
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
        return header + pcmBytes
    }

    /**
     * Plays an audio file through MediaPlayer on the voice communication speakerphone stream.
     */
    private suspend fun playAudioFile(file: File) = suspendCancellableCoroutine<Unit> { continuation ->
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
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
                setOnErrorListener { _, what, extra ->
                    Log.e("VoiceAssistManager", "MediaPlayer error: $what, $extra")
                    if (continuation.isActive) continuation.resume(Unit)
                    true
                }
                prepare()
                start()
            }
            continuation.invokeOnCancellation {
                try {
                    mediaPlayer?.stop()
                    mediaPlayer?.release()
                    mediaPlayer = null
                } catch (e: Exception) {
                    // Ignored
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceAssistManager", "Failed to play ElevenLabs audio file: ${e.message}")
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
        isStopped = false
        forceSpeakerphoneMaxVolume()

        broadcastJob?.cancel()
        broadcastJob = scope.launch {
            val script = buildEmergencyScript(userName, address, latitude, longitude, bloodType, allergies)

            // 1. Attempt ElevenLabs synthesis first, then Gemini Voice, then Android TTS
            var voiceFile: File? = null
            var voiceEngineLabel = "On-Device Android TTS"

            val key = elevenLabsApiKey?.takeIf { it.isNotBlank() } ?: AppConfig.ELEVENLABS_API_KEY
            val voice = elevenLabsVoiceId?.takeIf { it.isNotBlank() } ?: DEFAULT_VOICE_ID

            if (key.isNotBlank()) {
                onTranscriptUpdate?.invoke("[Contacting ElevenLabs Multilingual Engine (Sarah Soft Voice)...]\n$script", 1)
                voiceFile = synthesizeWithElevenLabs(
                    text = script,
                    apiKey = key,
                    voiceId = voice
                )
                if (voiceFile != null && voiceFile.exists()) {
                    voiceEngineLabel = "ElevenLabs HD Multilingual (Sarah - Soft Voice)"
                }
            } else if (!geminiApiKey.isNullOrBlank()) {
                onTranscriptUpdate?.invoke("[Contacting Gemini Voice Engine ($geminiVoiceName)...]\n$script", 1)
                voiceFile = synthesizeWithGemini(
                    text = script,
                    apiKey = geminiApiKey,
                    voiceName = geminiVoiceName
                )
                if (voiceFile != null && voiceFile.exists()) {
                    voiceEngineLabel = "Gemini 2.0 Flash Audio ($geminiVoiceName)"
                }
            }

            val hasSyntheticAudioFile = voiceFile != null && voiceFile.exists()

            // Wait for native TTS initialization if no synthetic audio file
            if (!hasSyntheticAudioFile) {
                var waited = 0
                while (!isTtsInitialized && waited < 20) {
                    delay(100)
                    waited++
                }
            }

            for (cycle in 1..3) {
                if (isStopped) break
                val label = "[Voice: $voiceEngineLabel]\n$script"
                onTranscriptUpdate?.invoke(label, cycle)

                // Play the audio
                if (hasSyntheticAudioFile) {
                    playAudioFile(voiceFile)
                } else {
                    speakUtterance(script, utteranceId = "EMERGENCY_CYCLE_$cycle")
                }

                if (cycle < 3 && !isStopped) {
                    onTranscriptUpdate?.invoke("[Pausing 5s before repeating broadcast...]", cycle)
                    delay(5000L)
                }
            }

            if (!isStopped) {
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
        broadcastJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignored
        }
        textToSpeech?.stop()

        scope.launch {
            val key = elevenLabsApiKey?.takeIf { it.isNotBlank() } ?: AppConfig.ELEVENLABS_API_KEY
            val voice = elevenLabsVoiceId?.takeIf { it.isNotBlank() } ?: DEFAULT_VOICE_ID

            var voiceFile: File? = null
            if (key.isNotBlank()) {
                voiceFile = synthesizeWithElevenLabs(
                    text = answerText,
                    apiKey = key,
                    voiceId = voice
                )
            } else if (!geminiApiKey.isNullOrBlank()) {
                voiceFile = synthesizeWithGemini(
                    text = answerText,
                    apiKey = geminiApiKey,
                    voiceName = geminiVoiceName
                )
            }

            if (voiceFile != null && voiceFile.exists()) {
                playAudioFile(voiceFile)
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
        isStopped = true
        broadcastJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignored
        }
        textToSpeech?.stop()

        val handoverText = buildHandoverScript()
        onTranscriptUpdate?.invoke(handoverText, 0)

        scope.launch {
            val key = elevenLabsApiKey?.takeIf { it.isNotBlank() } ?: AppConfig.ELEVENLABS_API_KEY
            val voice = elevenLabsVoiceId?.takeIf { it.isNotBlank() } ?: DEFAULT_VOICE_ID

            var voiceFile: File? = null
            if (key.isNotBlank()) {
                voiceFile = synthesizeWithElevenLabs(
                    text = handoverText,
                    apiKey = key,
                    voiceId = voice
                )
            }

            if (voiceFile != null && voiceFile.exists()) {
                playAudioFile(voiceFile)
            } else {
                speakUtterance(handoverText, utteranceId = "HANDOVER_UTTERANCE")
            }
            delay(300L)
            onComplete()
            onHandoverFinished?.invoke()
        }
    }

    private suspend fun speakUtterance(text: String, utteranceId: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (continuation.isActive) continuation.resume(Unit)
            }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        })

        val res = textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (res == TextToSpeech.ERROR) {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }

    fun stop() {
        isStopped = true
        broadcastJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            // Ignored
        }
        textToSpeech?.stop()
    }

    fun release() {
        stop()
        scope.cancel()
        textToSpeech?.shutdown()
        textToSpeech = null
        try {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e("VoiceAssistManager", "Error restoring audio mode: ${e.message}")
        }
    }
}
