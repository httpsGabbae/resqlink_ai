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
 * # GuardianSpeechRecognizer
 *
 * Continuous background Speech Recognizer for Guardian Mode.
 * Actively listens for distress keywords in Tagalog and English:
 *  - "Tulong", "Saklolo", "Kailangan ko ng tulong"
 *  - "Help", "Emergency", "ResQLink", "Save me"
 *  - "Cancel", "Stop", "Abort" (to cancel emergency countdown)
 *
 * Ordinary Human Overview:
 * When you turn on "Guardian Mode", your phone keeps its ears open while resting on a table
 * or in your pocket.
 *
 * This continuous listener:
 * 1. Configures Android's SpeechRecognizer for both Tagalog (`fil-PH`, `tl-PH`) and English (`en-PH`, `en-US`).
 * 2. Normalizes phonetic variations of the app's wake phrase ("rescue link", "risk link" -> "resqlink").
 * 3. Distinguishes genuine distress cries ("Tulong!", "Resqlink Tulong!") from negated phrases
 *    (e.g., "Huwag tumulong", "Hindi kailangan ng tulong").
 * 4. Listens for cancellation words ("Cancel", "Stop", "Tama na") to abort accidental countdowns hands-free.
 */
class GuardianSpeechRecognizer(
    private val context: Context,
    private val onKeywordDetected: (DistressKeyword, String) -> Unit
) {

    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private var systemSpeechRecognizer: SpeechRecognizer? = null
    private var isActivelyListening = false
    private var isRecognizerDestroyed = false

    /**
     * Starts continuous background listening on the main Android UI thread.
     */
    fun startListening() {
        if (isActivelyListening || isRecognizerDestroyed) return
        isActivelyListening = true
        mainThreadHandler.post { initializeAndStartSpeechRecognizer() }
    }

    private fun initializeAndStartSpeechRecognizer() {
        if (isRecognizerDestroyed || !isActivelyListening) return
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                Log.w("GuardianSpeech", "SpeechRecognizer is not available on this device")
                return
            }

            destroyRecognizerInternal()

            systemSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}

                    override fun onError(errorCode: Int) {
                        if (isRecognizerDestroyed || !isActivelyListening) return
                        // Error codes: 7 = NO_MATCH, 6 = SPEECH_TIMEOUT, 8 = RECOGNIZER_BUSY
                        // In continuous ambient listening, timeouts are normal. Re-arm immediately.
                        val delayMilliseconds = if (errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 800L else 350L
                        mainThreadHandler.postDelayed({
                            if (isActivelyListening && !isRecognizerDestroyed) {
                                startListeningInternal()
                            }
                        }, delayMilliseconds)
                    }

                    override fun onResults(results: Bundle?) {
                        evaluateSpeechResults(results)
                        // Seamless continuous ambient loop
                        mainThreadHandler.postDelayed({
                            if (isActivelyListening && !isRecognizerDestroyed) {
                                startListeningInternal()
                            }
                        }, 250L)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        evaluateSpeechResults(partialResults)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            startListeningInternal()
        } catch (exception: Exception) {
            Log.e("GuardianSpeech", "Error initializing speech recognizer: ${exception.message}")
        }
    }

    private fun startListeningInternal() {
        if (isRecognizerDestroyed || !isActivelyListening || systemSpeechRecognizer == null) return
        try {
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                // Listen with multilingual support for Philippines: Tagalog (fil-PH) and English (en-PH, en-US)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fil-PH")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fil-PH")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("tl-PH", "en-PH", "en-US"))
            }
            systemSpeechRecognizer?.startListening(speechIntent)
        } catch (exception: Exception) {
            Log.e("GuardianSpeech", "startListening error: ${exception.message}")
        }
    }

    /**
     * Parses transcribed speech candidates to detect wake words or cancel commands.
     */
    private fun evaluateSpeechResults(speechBundle: Bundle?) {
        val recognizedCandidateMatches = speechBundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return

        for (candidatePhrase in recognizedCandidateMatches) {
            val lowerCaseSpokenText = candidatePhrase.lowercase(Locale.ROOT).trim()
            if (lowerCaseSpokenText.isBlank()) continue
            Log.d("GuardianSpeech", "Speech heard: '$lowerCaseSpokenText'")

            // 1. Abort / Cancel keyword
            val cancelKeywordsList = listOf("cancel", "stop", "abort", "tama na", "huwag", "ayaw", "bawi")
            if (cancelKeywordsList.any { lowerCaseSpokenText.contains(it) }) {
                Log.i("GuardianSpeech", "Voice CANCEL recognized: '$candidatePhrase'")
                onKeywordDetected(DistressKeyword.CANCEL, candidatePhrase)
                return
            }

            // Normalize common speech recognizer phonetics for 'resqlink'
            val normalizedPhoneticText = lowerCaseSpokenText
                .replace("rescue link", "resqlink")
                .replace("resq link", "resqlink")
                .replace("risk link", "resqlink")
                .replace("reslink", "resqlink")
                .replace("rest link", "resqlink")

            // 2. Primary Wake Keyword: "Resqlink Tulong!" or "Resqlink Help!"
            val isPrimaryTulongTrigger = normalizedPhoneticText.contains("resqlink tulong") ||
                    normalizedPhoneticText.contains("resqlink, tulong") ||
                    normalizedPhoneticText.contains("resqlink saklolo")
            if (isPrimaryTulongTrigger) {
                Log.i("GuardianSpeech", "Primary Keyword MATCHED: 'Resqlink Tulong!' (Input: '$candidatePhrase')")
                onKeywordDetected(DistressKeyword.TULONG, "Resqlink Tulong!")
                return
            }

            val isPrimaryHelpTrigger = normalizedPhoneticText.contains("resqlink help") ||
                    normalizedPhoneticText.contains("resqlink emergency") ||
                    normalizedPhoneticText.contains("resqlink save")
            if (isPrimaryHelpTrigger) {
                Log.i("GuardianSpeech", "Primary Keyword MATCHED: 'Resqlink Help!' (Input: '$candidatePhrase')")
                onKeywordDetected(DistressKeyword.HELP, "Resqlink Help!")
                return
            }

            // 3. Super Distress Standalone: "Tulong", "Saklolo", "Tulong po"
            // Ignore negated sentences (e.g. "wag tumulong", "hindi kailangan ng tulong")
            val isNegatedSentence = lowerCaseSpokenText.contains("wag") ||
                    lowerCaseSpokenText.contains("huwag") ||
                    lowerCaseSpokenText.contains("hindi")

            if (!isNegatedSentence) {
                val wordsList = lowerCaseSpokenText.split("\\s+|,|!|\\?|\\.".toRegex()).filter { it.isNotBlank() }
                val hasSuperDistressWord = wordsList.any { it == "tulong" || it == "saklolo" }

                if (hasSuperDistressWord) {
                    Log.i("GuardianSpeech", "Super Distress Keyword MATCHED: 'Tulong!' (Input: '$candidatePhrase')")
                    onKeywordDetected(DistressKeyword.TULONG, "Tulong!")
                    return
                }
            }

            // Discard all other casual speech, screams, or shouting without the keywords!
        }
    }

    /**
     * Pauses listening without permanently destroying the recognizer instance.
     */
    fun stopListening() {
        isActivelyListening = false
        mainThreadHandler.post { destroyRecognizerInternal() }
    }

    private fun destroyRecognizerInternal() {
        try {
            systemSpeechRecognizer?.stopListening()
            systemSpeechRecognizer?.cancel()
            systemSpeechRecognizer?.destroy()
        } catch (ignored: Exception) {
        } finally {
            systemSpeechRecognizer = null
        }
    }

    /**
     * Permanently tears down the speech recognizer.
     */
    fun destroy() {
        isRecognizerDestroyed = true
        isActivelyListening = false
        destroyRecognizerInternal()
    }
}
