package com.resqlink.ai.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.*
import java.util.Locale

/**
 * # SpeechHandoverDetector
 *
 * Monitors the microphone during an ongoing automated 911 call to detect when
 * an unconscious or injured patient regains consciousness and begins speaking.
 *
 * Ordinary Human Overview:
 * Imagine a car crash where the driver is knocked unconscious. ResQLink AI initiates
 * a 911 call and talks to the operator on speakerphone.
 *
 * If the driver suddenly wakes up and groans "Tulong!" or "Hello?", the AI must:
 * 1. Instantly recognize that human speech is occurring.
 * 2. Immediately shut off automated AI voice responses.
 * 3. Hand over the microphone and speakerphone directly to the patient.
 *
 * This detector discriminates coherent human words from background vehicle/road noise.
 */
class SpeechHandoverDetector(
    private val context: Context,
    private val onClearSpeechDetected: (spokenWords: String) -> Unit
) {

    private var systemSpeechRecognizer: SpeechRecognizer? = null
    private var isActivelyListening: Boolean = false
    private var isDetectorDestroyed: Boolean = false
    private val mainCoroutineScope = CoroutineScope(Dispatchers.Main + Job())

    init {
        initializeSpeechRecognizer()
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SpeechHandoverDetector", "Speech recognition is not available on this device")
            return
        }

        systemSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(errorCode: Int) {
                    if (isDetectorDestroyed || !isActivelyListening) return
                    // Re-arm listener after brief delay for continuous accident monitoring
                    mainCoroutineScope.launch {
                        delay(500L)
                        if (isActivelyListening && !isDetectorDestroyed) {
                            startListeningInternal()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val candidateSpeechMatches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val topSpokenCandidate = candidateSpeechMatches?.firstOrNull()?.trim() ?: ""
                    Log.d("SpeechHandoverDetector", "Heard speech candidate: '$topSpokenCandidate'")

                    if (isValidPatientSpeech(topSpokenCandidate)) {
                        Log.i("SpeechHandoverDetector", "Valid coherent patient speech confirmed: '$topSpokenCandidate'")
                        stopListening()
                        onClearSpeechDetected(topSpokenCandidate)
                    } else if (isActivelyListening && !isDetectorDestroyed) {
                        // Keep listening
                        mainCoroutineScope.launch {
                            delay(300L)
                            startListeningInternal()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partialSpeechMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val topPartialCandidate = partialSpeechMatches?.firstOrNull()?.trim() ?: ""
                    if (isValidPatientSpeech(topPartialCandidate)) {
                        Log.i("SpeechHandoverDetector", "Partial coherent speech detected: '$topPartialCandidate'")
                        stopListening()
                        onClearSpeechDetected(topPartialCandidate)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    /**
     * Discerns coherent words from ambient mumbles or vehicle engine groans.
     * Criteria: Either 2+ words OR recognized high-confidence emergency distress keywords.
     */
    private fun isValidPatientSpeech(text: String): Boolean {
        if (text.isBlank()) return false
        val spokenWordsList = text.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // High priority single words signaling consciousness
        val consciousSignals = setOf(
            "tulong", "help", "hello", "opo", "oo", "nandito", "nasugatan",
            "aray", "sino", "yes", "here", "can", "hear", "buhay", "resqlink"
        )

        val containsConsciousSignal = spokenWordsList.any { word -> word.lowercase(Locale.ROOT) in consciousSignals }
        return spokenWordsList.size >= 2 || containsConsciousSignal
    }

    /**
     * Starts listening for patient voice activity.
     */
    fun startListening() {
        if (isActivelyListening || isDetectorDestroyed) return
        isActivelyListening = true
        startListeningInternal()
    }

    private fun startListeningInternal() {
        try {
            val speechRecognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-PH")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            systemSpeechRecognizer?.startListening(speechRecognitionIntent)
        } catch (exception: Exception) {
            Log.e("SpeechHandoverDetector", "Error starting listening: ${exception.message}")
        }
    }

    /**
     * Halts speech recognition.
     */
    fun stopListening() {
        isActivelyListening = false
        try {
            systemSpeechRecognizer?.stopListening()
            systemSpeechRecognizer?.cancel()
        } catch (exception: Exception) {
            Log.e("SpeechHandoverDetector", "Error stopping listening: ${exception.message}")
        }
    }

    /**
     * Releases speech recognition and coroutine resources.
     */
    fun destroy() {
        isDetectorDestroyed = true
        stopListening()
        mainCoroutineScope.cancel()
        systemSpeechRecognizer?.destroy()
        systemSpeechRecognizer = null
    }
}
