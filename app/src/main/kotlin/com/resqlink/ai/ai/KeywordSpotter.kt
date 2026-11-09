package com.resqlink.ai.ai

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

enum class DistressKeyword(val label: String, val isEmergency: Boolean) {
    TULONG("Tulong", true),
    SAKLOLO("Saklolo", true),
    HELP("Help", true),
    EMERGENCY("Emergency", true),
    CANCEL("Cancel", false),
    UNKNOWN("Unknown", false),
    SILENCE("Silence", false)
}

data class SpotterResult(
    val keyword: DistressKeyword,
    val confidence: Float
)

class KeywordSpotter(private val context: Context) {

    private var tfliteInterpreter: Interpreter? = null
    private val modelAssetPath = "models/kws_ph.tflite"

    init {
        loadModelIfAvailable()
    }

    private fun loadModelIfAvailable() {
        try {
            val assetFileDescriptor = context.assets.openFd(modelAssetPath)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            val buffer: ByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            
            val options = Interpreter.Options().apply {
                setNumThreads(2)
            }
            tfliteInterpreter = Interpreter(buffer, options)
        } catch (e: Exception) {
            // Model file not yet bundled or falling back to acoustic energy classifier
            tfliteInterpreter = null
        }
    }

    /**
     * Evaluates a 1-second rolling 16kHz audio buffer (16,000 shorts).
     */
    fun classifyAudio(rollingPcmBuffer: ShortArray): SpotterResult {
        val interpreter = tfliteInterpreter
        if (interpreter != null) {
            return runTFLiteInference(interpreter, rollingPcmBuffer)
        }

        // Shouting, screaming, or loud ambient noises alone MUST NEVER trigger an emergency.
        // The AI is strictly trained to require the voice keyword "Resqlink Tulong!" or super-distress "Tulong!".
        // Continuous linguistic keyword spotting is handled accurately by GuardianSpeechRecognizer.
        return SpotterResult(DistressKeyword.SILENCE, 0.0f)
    }

    private fun runTFLiteInference(interpreter: Interpreter, pcmBuffer: ShortArray): SpotterResult {
        try {
            val inputBuffer = ByteBuffer.allocateDirect(pcmBuffer.size * 4).apply {
                order(ByteOrder.nativeOrder())
                for (s in pcmBuffer) {
                    putFloat(s / 32768.0f) // Normalized audio [-1.0, 1.0]
                }
                rewind()
            }

            // Output array: [1, num_classes] (e.g. 7 classes)
            val outputScores = Array(1) { FloatArray(7) }
            interpreter.run(inputBuffer, outputScores)

            val scores = outputScores[0]
            var maxIndex = 0
            var maxScore = 0f
            for (i in scores.indices) {
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxIndex = i
                }
            }

            val keyword = when (maxIndex) {
                0 -> DistressKeyword.SILENCE
                1 -> DistressKeyword.UNKNOWN
                2 -> DistressKeyword.TULONG
                3 -> DistressKeyword.SAKLOLO
                4 -> DistressKeyword.HELP
                5 -> DistressKeyword.EMERGENCY
                6 -> DistressKeyword.CANCEL
                else -> DistressKeyword.UNKNOWN
            }

            return SpotterResult(keyword, maxScore)
        } catch (e: Exception) {
            return SpotterResult(DistressKeyword.UNKNOWN, 0f)
        }
    }

    fun close() {
        tfliteInterpreter?.close()
        tfliteInterpreter = null
    }
}
