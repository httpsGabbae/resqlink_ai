package com.resqlink.ai.ai

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * # AudioStreamRecorder
 *
 * Operates an ultra-low-latency 16kHz audio recording loop to capture ambient speech
 * for on-device voice distress detection.
 *
 * Ordinary Human Overview:
 * Imagine keeping a 1-second sliding tape recorder running in the phone's memory.
 * 1. **50-millisecond Chunks**: The microphone captures audio in tiny 50ms slices (800 numbers).
 * 2. **Stage 1 (Energy Check)**: It asks [VadDetector] "Is someone speaking, or is the room quiet?".
 *    If quiet, it sleeps to conserve phone battery.
 * 3. **Stage 2 (Keyword Recognition)**: If a human voice is heard, it sends the full 1-second
 *    rolling tape to [KeywordSpotter] to check for distress words like "Tulong!" or "Help!".
 * 4. If a distress word is detected with high confidence (>= 75%), it emits an event to trigger
 *    the 10-second emergency safety countdown.
 */
class AudioStreamRecorder(
    private val voiceActivityDetector: VadDetector,
    private val keywordSpotter: KeywordSpotter
) {

    companion object {
        /** Standard speech recognition sampling frequency: 16,000 samples per second (16kHz). */
        const val SAMPLE_RATE_HERTZ = 16000

        /** Single-channel mono microphone input. */
        const val CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_MONO

        /** Linear 16-bit Pulse Code Modulation (standard CD-grade uncompressed audio encoding). */
        const val AUDIO_ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        /** Number of audio samples in a 1.0-second sliding circular window (16,000 audio points). */
        const val ROLLING_WINDOW_SAMPLES = SAMPLE_RATE_HERTZ

        /** Small 50ms audio chunk size processed per iteration: 16,000 * 0.05 = 800 audio samples. */
        const val CHUNK_SIZE_SAMPLES = 800
    }

    private var hardwareAudioRecord: AudioRecord? = null
    private var backgroundRecordingJob: Job? = null
    private var isCurrentlyMonitoring = false

    /**
     * Shared reactive flow emitting detected distress events to the rest of the application.
     */
    private val _detectionEvents = MutableSharedFlow<SpotterResult>(extraBufferCapacity = 10)
    val detectionEvents = _detectionEvents.asSharedFlow()

    /**
     * Starts the microphone recording and background analysis loop.
     *
     * @param coroutineScope The lifecycle scope in which the background audio loop executes.
     */
    @SuppressLint("MissingPermission")
    fun startMonitoring(coroutineScope: CoroutineScope) {
        if (isCurrentlyMonitoring) return

        val minimumBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HERTZ,
            CHANNEL_CONFIGURATION,
            AUDIO_ENCODING_FORMAT
        )
        val calculatedBufferSize = maxOf(minimumBufferSize, CHUNK_SIZE_SAMPLES * 2 * 4)

        try {
            hardwareAudioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE_HERTZ,
                CHANNEL_CONFIGURATION,
                AUDIO_ENCODING_FORMAT,
                calculatedBufferSize
            )

            if (hardwareAudioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            hardwareAudioRecord?.startRecording()
            isCurrentlyMonitoring = true

            backgroundRecordingJob = coroutineScope.launch(Dispatchers.Default) {
                // Rolling 1-second circular buffer holding the most recent 16,000 audio samples
                val rollingAudioBuffer = ShortArray(ROLLING_WINDOW_SAMPLES)
                var bufferWriteIndex = 0
                val readChunkBuffer = ShortArray(CHUNK_SIZE_SAMPLES)

                while (isActive && isCurrentlyMonitoring) {
                    val samplesReadCount = hardwareAudioRecord?.read(readChunkBuffer, 0, CHUNK_SIZE_SAMPLES) ?: -1
                    if (samplesReadCount <= 0) continue

                    // Stage 1: Quick Root-Mean-Square (RMS) Voice Activity check
                    val isHumanVoicePresent = voiceActivityDetector.isVoiceActive(readChunkBuffer, samplesReadCount)

                    // Write the fresh 50ms chunk into the circular 1-second rolling buffer
                    for (sampleIndex in 0 until samplesReadCount) {
                        rollingAudioBuffer[(bufferWriteIndex + sampleIndex) % ROLLING_WINDOW_SAMPLES] = readChunkBuffer[sampleIndex]
                    }
                    bufferWriteIndex = (bufferWriteIndex + samplesReadCount) % ROLLING_WINDOW_SAMPLES

                    // Stage 2: If voice energy was detected, run Keyword Spotting inference
                    if (isHumanVoicePresent) {
                        val classificationResult = keywordSpotter.classifyAudio(rollingAudioBuffer)

                        // If an emergency distress keyword was recognized with >= 75% confidence
                        if (classificationResult.keyword.isEmergency && classificationResult.confidence >= 0.75f) {
                            _detectionEvents.emit(classificationResult)
                        } else if (classificationResult.keyword == DistressKeyword.CANCEL && classificationResult.confidence >= 0.75f) {
                            _detectionEvents.emit(classificationResult)
                        }
                    }

                    // Yield CPU briefly to prevent thread starvation and preserve battery life
                    yield()
                }
            }
        } catch (recordingException: Exception) {
            stopMonitoring()
        }
    }

    /**
     * Stops the audio recording loop and releases the microphone hardware.
     */
    fun stopMonitoring() {
        isCurrentlyMonitoring = false
        backgroundRecordingJob?.cancel()
        backgroundRecordingJob = null
        try {
            hardwareAudioRecord?.stop()
            hardwareAudioRecord?.release()
        } catch (ignoredException: Exception) {
            // Safe cleanup
        } finally {
            hardwareAudioRecord = null
        }
    }
}
