package com.resqlink.ai.ai

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

/**
 * # DistressKeyword
 *
 * Vocabulary of recognized spoken words supported by the acoustic spotting engine.
 *
 * @property label Clean textual name of the keyword.
 * @property isEmergency True if hearing this word constitutes an emergency distress call.
 */
enum class DistressKeyword(val label: String, val isEmergency: Boolean) {
    /** Filipino primary distress cry ("Tulong!" / "Help!"). */
    TULONG("Tulong", true),

    /** Filipino urgent distress cry ("Saklolo!" / "Rescue!"). */
    SAKLOLO("Saklolo", true),

    /** English universal distress cry ("Help!"). */
    HELP("Help", true),

    /** Universal emergency indicator ("Emergency!"). */
    EMERGENCY("Emergency", true),

    /** Spoken cancellation word to abort a pending false alarm ("Cancel!"). */
    CANCEL("Cancel", false),

    /** Unrecognized human speech or words that are not emergency keywords. */
    UNKNOWN("Unknown", false),

    /** Silence, ambient static, or background noise. */
    SILENCE("Silence", false)
}

/**
 * # SpotterResult
 *
 * Holds the output of a single keyword recognition evaluation.
 *
 * @property keyword The classified word category.
 * @property confidence Neural network probability score between 0.0 (0%) and 1.0 (100%).
 */
data class SpotterResult(
    val keyword: DistressKeyword,
    val confidence: Float
)

/**
 * # KeywordSpotter
 *
 * Stage 2 of the audio distress pipeline.
 *
 * Ordinary Human Overview:
 * When the [VadDetector] detects speech energy, this class evaluates the 1.0-second
 * audio recording using an on-device TensorFlow Lite (`kws_ph.tflite`) neural network model:
 * 1. It normalizes 16,000 raw microphone numbers into decimal values between -1.0 and 1.0.
 * 2. It feeds the audio into the machine learning model.
 * 3. The model outputs a probability score for each keyword.
 * 4. If the confidence is high, the app triggers the emergency countdown.
 *
 * Safety Requirement:
 * Loud yelling, screaming, or sudden bangs alone MUST NEVER trigger an emergency.
 * The system strictly requires an articulated distress keyword ("Tulong", "Saklolo", "Help").
 */
class KeywordSpotter(private val context: Context) {

    private var tfliteInterpreter: Interpreter? = null
    private val modelAssetPath = "models/kws_ph.tflite"

    init {
        loadModelIfAvailable()
    }

    /**
     * Attempts to map and load the on-device TensorFlow Lite model file from app assets.
     */
    private fun loadModelIfAvailable() {
        try {
            val assetFileDescriptor = context.assets.openFd(modelAssetPath)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val modelMemoryBuffer: ByteBuffer = fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
            )

            val interpreterOptions = Interpreter.Options().apply {
                setNumThreads(2) // Utilize 2 CPU cores for fast parallel mathematical matrix math
            }
            tfliteInterpreter = Interpreter(modelMemoryBuffer, interpreterOptions)
        } catch (loadException: Exception) {
            // Model file not yet bundled or falling back to linguistic speech recognizer
            tfliteInterpreter = null
        }
    }

    /**
     * Evaluates a 1-second rolling 16kHz audio buffer (16,000 PCM numbers).
     *
     * @param rollingPcmBuffer The raw 16-bit audio samples from the microphone.
     * @return [SpotterResult] indicating the detected keyword and confidence score.
     */
    fun classifyAudio(rollingPcmBuffer: ShortArray): SpotterResult {
        val activeInterpreter = tfliteInterpreter
        if (activeInterpreter != null) {
            return runTensorFlowLiteInference(activeInterpreter, rollingPcmBuffer)
        }

        // Failsafe: When running without the optional local TFLite binary,
        // speech recognition is handled accurately by the Android SpeechRecognizer service.
        return SpotterResult(DistressKeyword.SILENCE, 0.0f)
    }

    /**
     * Closes the TensorFlow Lite interpreter and releases native C++ memory resources.
     */
    fun close() {
        tfliteInterpreter?.close()
        tfliteInterpreter = null
    }

    /**
     * Feeds the normalized audio into the TensorFlow Lite interpreter and parses probability scores.
     */
    private fun runTensorFlowLiteInference(
        interpreter: Interpreter,
        pcmBuffer: ShortArray
    ): SpotterResult {
        try {
            // 1. Allocate a direct byte buffer: 16,000 samples * 4 bytes per 32-bit float
            val inputByteBuffer = ByteBuffer.allocateDirect(pcmBuffer.size * 4).apply {
                order(ByteOrder.nativeOrder())
                for (audioSample in pcmBuffer) {
                    // Normalize 16-bit integer [-32768, 32767] into floating point [-1.0, 1.0]
                    putFloat(audioSample / 32768.0f)
                }
                rewind()
            }

            // 2. Output tensor: [1 batch, 7 keyword classes]
            val outputProbabilities = Array(1) { FloatArray(7) }
            interpreter.run(inputByteBuffer, outputProbabilities)

            // 3. Find the highest probability class (ArgMax)
            val classScores = outputProbabilities[0]
            var highestScoreIndex = 0
            var highestConfidenceScore = 0f
            for (classIndex in classScores.indices) {
                if (classScores[classIndex] > highestConfidenceScore) {
                    highestConfidenceScore = classScores[classIndex]
                    highestScoreIndex = classIndex
                }
            }

            val classifiedKeyword = when (highestScoreIndex) {
                0 -> DistressKeyword.SILENCE
                1 -> DistressKeyword.UNKNOWN
                2 -> DistressKeyword.TULONG
                3 -> DistressKeyword.SAKLOLO
                4 -> DistressKeyword.HELP
                5 -> DistressKeyword.EMERGENCY
                6 -> DistressKeyword.CANCEL
                else -> DistressKeyword.UNKNOWN
            }

            return SpotterResult(classifiedKeyword, highestConfidenceScore)
        } catch (inferenceException: Exception) {
            return SpotterResult(DistressKeyword.UNKNOWN, 0f)
        }
    }
}
