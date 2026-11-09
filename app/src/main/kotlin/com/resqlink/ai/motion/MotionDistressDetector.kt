package com.resqlink.ai.motion

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.resqlink.ai.emergency.EmergencyEventManager
import com.resqlink.ai.emergency.EmergencyTriggerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

enum class MotionDetectionState {
    IDLE,
    MONITORING,
    IMPACT_DETECTED_EVALUATING,
    DISTRESS_TRIGGERED
}

/**
 * Experimental Motion-Assisted Distress Detector.
 * Identifies high-G impact spikes (e.g. violent crash or hard fall) followed by
 * sustained physical inactivity.
 *
 * SAFETY INVARIANTS:
 * - Marked as EXPERIMENTAL.
 * - Must be explicitly opted-in by user.
 * - Triggers a 10-second interactive countdown.
 * - NEVER automatically dials 911 or public emergency services.
 */
class MotionDistressDetector(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : SensorEventListener {

    companion object {
        // Impact threshold in m/s² (Earth gravity g = 9.8 m/s²; 27.5 m/s² ≈ 2.8g)
        const val IMPACT_ACCELERATION_THRESHOLD = 27.5f

        // Inactivity window after impact (3.5 seconds of sustained stillness)
        const val INACTIVITY_WINDOW_MILLIS = 3500L

        // Inactivity delta threshold (must remain near 1.0g with minimal variation)
        const val INACTIVITY_MAX_VARIATION = 1.8f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _detectionState = MutableStateFlow(MotionDetectionState.IDLE)
    val detectionState: StateFlow<MotionDetectionState> = _detectionState.asStateFlow()

    private var impactTimestamp: Long = 0L
    private var recordedPeakG: Float = 0f
    private var evaluationJob: Job? = null
    private var isEvaluatingInactivity: Boolean = false
    private val recentMagnitudes = mutableListOf<Float>()

    var onImpactDistressDetected: ((peakG: Float) -> Unit)? = null

    fun isSensorAvailable(): Boolean = accelerometer != null

    @Synchronized
    fun startMonitoring(): Boolean {
        if (accelerometer == null || sensorManager == null) return false

        sensorManager.unregisterListener(this)
        val registered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        if (registered) {
            _detectionState.value = MotionDetectionState.MONITORING
        }
        return registered
    }

    @Synchronized
    fun stopMonitoring() {
        evaluationJob?.cancel()
        evaluationJob = null
        isEvaluatingInactivity = false
        sensorManager?.unregisterListener(this)
        _detectionState.value = MotionDetectionState.IDLE
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitude = sqrt(ax * ax + ay * ay + az * az)

        if (isEvaluatingInactivity) {
            synchronized(recentMagnitudes) {
                recentMagnitudes.add(magnitude)
                if (recentMagnitudes.size > 50) {
                    recentMagnitudes.removeAt(0)
                }
            }
            return
        }

        // Detect high-G impact spike
        if (magnitude >= IMPACT_ACCELERATION_THRESHOLD) {
            triggerInactivityEvaluation(magnitude)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    private fun triggerInactivityEvaluation(impactG: Float) {
        isEvaluatingInactivity = true
        impactTimestamp = System.currentTimeMillis()
        recordedPeakG = impactG
        _detectionState.value = MotionDetectionState.IMPACT_DETECTED_EVALUATING

        synchronized(recentMagnitudes) {
            recentMagnitudes.clear()
        }

        evaluationJob?.cancel()
        evaluationJob = scope.launch {
            // Wait for the post-impact stabilization and inactivity window
            delay(INACTIVITY_WINDOW_MILLIS)

            // Check if user remained inactive or resumed normal active movement
            val samples = synchronized(recentMagnitudes) { recentMagnitudes.toList() }
            isEvaluatingInactivity = false

            if (samples.isEmpty()) {
                _detectionState.value = MotionDetectionState.MONITORING
                return@launch
            }

            val gravity = 9.8f
            // Measure deviation from rest gravity
            val maxDeviation = samples.maxOf { Math.abs(it - gravity) }

            if (maxDeviation <= INACTIVITY_MAX_VARIATION) {
                // Inactivity confirmed: Potential incapacitated user!
                _detectionState.value = MotionDetectionState.DISTRESS_TRIGGERED
                notifyDistress(recordedPeakG)
            } else {
                // User moved normally (e.g. picked up phone, running, dropped without injury) -> False positive dismissed!
                _detectionState.value = MotionDetectionState.MONITORING
            }
        }
    }

    private fun notifyDistress(peakG: Float) {
        // Register trigger in EmergencyEventManager (triggers 10-second countdown with alarm and cancel button)
        val gValueFormatted = String.format(java.util.Locale.US, "%.1fg", peakG / 9.8f)
        EmergencyEventManager.getInstance().registerTrigger(
            source = EmergencyTriggerSource.EXPERIMENTAL_MOTION_IMPACT,
            detectedPhrase = "Impact detected ($gValueFormatted) followed by lack of movement."
        )

        onImpactDistressDetected?.invoke(peakG)
    }
}
