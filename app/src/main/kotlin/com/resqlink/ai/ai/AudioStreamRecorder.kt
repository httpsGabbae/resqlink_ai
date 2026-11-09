package com.resqlink.ai.ai

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AudioStreamRecorder(
    private val vadDetector: VadDetector,
    private val keywordSpotter: KeywordSpotter
) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        const val ROLLING_WINDOW_SAMPLES = SAMPLE_RATE // 1.0 second of audio
        const val CHUNK_SIZE_SAMPLES = 800 // 50ms chunks (16000 * 0.05)
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private val _detectionEvents = MutableSharedFlow<SpotterResult>(extraBufferCapacity = 10)
    val detectionEvents = _detectionEvents.asSharedFlow()

    @SuppressLint("MissingPermission")
    fun startMonitoring(scope: CoroutineScope) {
        if (isRecording) return

        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufferSize = maxOf(minBufferSize, CHUNK_SIZE_SAMPLES * 2 * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingJob = scope.launch(Dispatchers.Default) {
                val rollingBuffer = ShortArray(ROLLING_WINDOW_SAMPLES)
                var bufferWriteIndex = 0
                val readChunk = ShortArray(CHUNK_SIZE_SAMPLES)

                while (isActive && isRecording) {
                    val readResult = audioRecord?.read(readChunk, 0, CHUNK_SIZE_SAMPLES) ?: -1
                    if (readResult <= 0) continue

                    // 1. Stage 1: Fast Energy VAD check
                    val hasVoice = vadDetector.isVoiceActive(readChunk, readResult)

                    // Write into rolling buffer
                    for (i in 0 until readResult) {
                        rollingBuffer[(bufferWriteIndex + i) % ROLLING_WINDOW_SAMPLES] = readChunk[i]
                    }
                    bufferWriteIndex = (bufferWriteIndex + readResult) % ROLLING_WINDOW_SAMPLES

                    // 2. Stage 2: If voice is active, run Keyword Spotting inference
                    if (hasVoice) {
                        val result = keywordSpotter.classifyAudio(rollingBuffer)
                        if (result.keyword.isEmergency && result.confidence >= 0.75f) {
                            _detectionEvents.emit(result)
                        } else if (result.keyword == DistressKeyword.CANCEL && result.confidence >= 0.75f) {
                            _detectionEvents.emit(result)
                        }
                    }

                    // Yield to prevent CPU starvation
                    yield()
                }
            }
        } catch (e: Exception) {
            stopMonitoring()
        }
    }

    fun stopMonitoring() {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            audioRecord = null
        }
    }
}
