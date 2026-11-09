package com.resqlink.ai.ai

import kotlin.math.sqrt

/**
 * Stage 1: Lightweight Voice Activity Detector (VAD)
 * Calculates RMS (Root Mean Square) energy of 16-bit PCM audio frames.
 * Discards ambient silence and low-energy static to prevent constant CPU wakeups.
 */
class VadDetector(
    private val energyThresholdRms: Double = 450.0
) {

    fun isVoiceActive(pcm16Buffer: ShortArray, readSize: Int): Boolean {
        if (readSize <= 0) return false

        var sumSquare = 0.0
        for (i in 0 until readSize) {
            val sample = pcm16Buffer[i].toDouble()
            sumSquare += sample * sample
        }

        val rms = sqrt(sumSquare / readSize)
        return rms >= energyThresholdRms
    }
}
