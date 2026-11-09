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

class SpeechHandoverDetector(
    private val context: Context,
    private val onClearSpeechDetected: (spokenWords: String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isDestroyed = false
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("SpeechHandoverDetector", "Speech recognition is not available on this device")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    if (isDestroyed || !isListening) return
                    // Re-arm listener after brief delay for continuous accident monitoring
                    scope.launch {
                        delay(500L)
                        if (isListening && !isDestroyed) {
                            startListeningInternal()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim() ?: ""
                    Log.d("SpeechHandoverDetector", "Heard speech candidate: '$spoken'")

                    if (isValidPatientSpeech(spoken)) {
                        Log.i("SpeechHandoverDetector", "Valid coherent patient speech confirmed: '$spoken'")
                        stopListening()
                        onClearSpeechDetected(spoken)
                    } else if (isListening && !isDestroyed) {
                        // Keep listening
                        scope.launch {
                            delay(300L)
                            startListeningInternal()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim() ?: ""
                    if (isValidPatientSpeech(partial)) {
                        Log.i("SpeechHandoverDetector", "Partial coherent speech detected: '$partial'")
                        stopListening()
                        onClearSpeechDetected(partial)
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
        val words = text.split("\\s+".toRegex()).filter { it.isNotBlank() }

        // High priority single words signaling consciousness
        val consciousSignals = setOf(
            "tulong", "help", "hello", "opo", "oo", "nandito", "nasugatan", 
            "aray", "sino", "yes", "here", "can", "hear", "buhay", "resqlink"
        )

        val containsSignal = words.any { it.lowercase(Locale.ROOT) in consciousSignals }
        return words.size >= 2 || containsSignal
    }

    fun startListening() {
        if (isListening || isDestroyed) return
        isListening = true
        startListeningInternal()
    }

    private fun startListeningInternal() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-PH")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("SpeechHandoverDetector", "Error starting listening: ${e.message}")
        }
    }

    fun stopListening() {
        isListening = false
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e("SpeechHandoverDetector", "Error stopping listening: ${e.message}")
        }
    }

    fun destroy() {
        isDestroyed = true
        stopListening()
        scope.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
