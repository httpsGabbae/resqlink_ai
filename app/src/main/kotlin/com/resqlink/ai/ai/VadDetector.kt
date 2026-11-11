package com.resqlink.ai.ai

import kotlin.math.sqrt

/**
 * # VadDetector (Voice Activity Detector)
 *
 * Stage 1 of the audio distress pipeline.
 *
 * Ordinary Human Overview:
 * Constantly running complex speech recognition models on phone CPUs drains the battery in a few hours.
 * To solve this, [VadDetector] acts as a high-speed energy filter:
 * 1. It calculates the Root Mean Square (RMS) energy of every 50ms audio chunk.
 * 2. If the room is quiet or only low-frequency background hum is present (RMS < 450.0), it reports false.
 * 3. The CPU immediately skips the heavy neural network, allowing the phone to rest.
 * 4. Only when someone actually speaks does it wake up the [KeywordSpotter].
 */
class VadDetector(
    private val energyThresholdRms: Double = 450.0
) {

    /**
     * Inspects a PCM 16-bit audio buffer to determine if acoustic energy exceeds the speech threshold.
     *
     * @param pcm16Buffer Array of 16-bit signed audio numbers captured from the microphone.
     * @param readSize Number of valid audio samples in the array.
     * @return True if sound energy indicates active speech, false if silence or ambient noise.
     */
    fun isVoiceActive(pcm16Buffer: ShortArray, readSize: Int): Boolean {
        if (readSize <= 0) return false

        var sumOfSquaredSamples = 0.0
        for (sampleIndex in 0 until readSize) {
            val sampleValue = pcm16Buffer[sampleIndex].toDouble()
            sumOfSquaredSamples += sampleValue * sampleValue
        }

        // Root Mean Square (RMS) = SquareRoot( Sum of Squares / Sample Count )
        val rootMeanSquareEnergy = sqrt(sumOfSquaredSamples / readSize)
        return rootMeanSquareEnergy >= energyThresholdRms
    }
}
