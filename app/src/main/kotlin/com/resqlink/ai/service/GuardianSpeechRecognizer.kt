package com.resqlink.ai.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.resqlink.ai.ai.DistressKeyword
import java.util.Locale

/**
 * Continuous background Speech Recognizer for Guardian Mode.
 * Actively listens for distress keywords in Tagalog and English:
 *  - "Tulong", "Saklolo", "Kailangan ko ng tulong"
 *  - "Help", "Emergency", "ResQLink", "Save me"
 *  - "Cancel", "Stop", "Abort" (to cancel emergency countdown)
 */
class GuardianSpeechRecognizer(
    private val context: Context,
    private val onKeywordDetected: (DistressKeyword, String) -> Unit
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isDestroyed = false

    fun startListening() {
        if (isListening || isDestroyed) return
        isListening = true
        mainHandler.post { initAndStartRecognizer() }
    }

    private fun initAndStartRecognizer() {
        if (isDestroyed || !isListening) return
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("GuardianSpeech", "SpeechRecognizer is not available on this device")
                return
            }

            destroyRecognizer()

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        if (isDestroyed || !isListening) return
                        // Error codes: 7 = NO_MATCH, 6 = SPEECH_TIMEOUT, 8 = RECOGNIZER_BUSY
                        // In continuous ambient listening, timeouts are expected. Re-arm immediately.
                        val delayMs = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 800L else 350L
                        mainHandler.postDelayed({
                            if (isListening && !isDestroyed) {
                                startListeningInternal()
                            }
                        }, delayMs)
                    }

                    override fun onResults(results: Bundle?) {
                        handleSpeechResults(results)
                        // Seamless continuous loop
                        mainHandler.postDelayed({
                            if (isListening && !isDestroyed) {
                                startListeningInternal()
                            }
                        }, 250L)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        handleSpeechResults(partialResults)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            startListeningInternal()
        } catch (e: Exception) {
            Log.e("GuardianSpeech", "Error initializing speech recognizer: ${e.message}")
        }
    }

    private fun startListeningInternal() {
        if (isDestroyed || !isListening || speechRecognizer == null) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Listen with multilingual support for Philippines: Tagalog (fil-PH) and English (en-PH, en-US)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fil-PH")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fil-PH")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("tl-PH", "en-PH", "en-US"))
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("GuardianSpeech", "startListening error: ${e.message}")
        }
    }

    private fun handleSpeechResults(bundle: Bundle?) {
        val matches = bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
        for (candidate in matches) {
            val text = candidate.lowercase(Locale.ROOT).trim()
            if (text.isBlank()) continue
            Log.d("GuardianSpeech", "Speech heard: '$text'")

            // 1. Abort / Cancel keyword
            val cancelWords = listOf("cancel", "stop", "abort", "tama na", "huwag", "ayaw", "bawi")
            if (cancelWords.any { text.contains(it) }) {
                Log.i("GuardianSpeech", "Voice CANCEL recognized: '$candidate'")
                onKeywordDetected(DistressKeyword.CANCEL, candidate)
                return
            }

            // Normalize common speech recognizer phonetics for 'resqlink'
            val normalized = text
                .replace("rescue link", "resqlink")
                .replace("resq link", "resqlink")
                .replace("risk link", "resqlink")
                .replace("reslink", "resqlink")
                .replace("rest link", "resqlink")

            // 2. Primary Wake Keyword: "Resqlink Tulong!" or "Resqlink Help!"
            val isPrimaryTulong = normalized.contains("resqlink tulong") ||
                    normalized.contains("resqlink, tulong") ||
                    normalized.contains("resqlink saklolo")
            if (isPrimaryTulong) {
                Log.i("GuardianSpeech", "Primary Keyword MATCHED: 'Resqlink Tulong!' (Input: '$candidate')")
                onKeywordDetected(DistressKeyword.TULONG, "Resqlink Tulong!")
                return
            }

            val isPrimaryHelp = normalized.contains("resqlink help") ||
                    normalized.contains("resqlink emergency") ||
                    normalized.contains("resqlink save")
            if (isPrimaryHelp) {
                Log.i("GuardianSpeech", "Primary Keyword MATCHED: 'Resqlink Help!' (Input: '$candidate')")
                onKeywordDetected(DistressKeyword.HELP, "Resqlink Help!")
                return
            }

            // 3. Super Distress Standalone: "Tulong", "Saklolo", "Tulong po"
            // Ignore negated sentences (e.g. "wag tumulong", "hindi kailangan ng tulong")
            val isNegated = text.contains("wag") || text.contains("huwag") || text.contains("hindi")
            if (!isNegated) {
                val words = text.split("\\s+|,|!|\\?|\\.".toRegex()).filter { it.isNotBlank() }
                val hasSuperDistress = words.any { it == "tulong" || it == "saklolo" }

                if (hasSuperDistress) {
                    Log.i("GuardianSpeech", "Super Distress Keyword MATCHED: 'Tulong!' (Input: '$candidate')")
                    onKeywordDetected(DistressKeyword.TULONG, "Tulong!")
                    return
                }
            }

            // Discard all other casual speech, screams, or shouting without the keywords!
        }
    }

    fun stopListening() {
        isListening = false
        mainHandler.post { destroyRecognizer() }
    }

    private fun destroyRecognizer() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignored
        } finally {
            speechRecognizer = null
        }
    }

    fun destroy() {
        isDestroyed = true
        isListening = false
        destroyRecognizer()
    }
}
