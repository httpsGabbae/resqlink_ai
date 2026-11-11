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
import java.util.Locale
import kotlin.math.sqrt

/**
 * # MotionDetectionState
 *
 * Current operational phase of the motion distress detection engine.
 */
enum class MotionDetectionState {
    /** The accelerometer sensor is disconnected or idle. */
    IDLE,

    /** Actively sampling 3-axis accelerometer readings in the background. */
    MONITORING,

    /** High-G collision spike detected. Evaluating subsequent 3.5 seconds for complete stillness. */
    IMPACT_DETECTED_EVALUATING,

    /** Impact confirmed followed by sustained inactivity. Safety countdown triggered. */
    DISTRESS_TRIGGERED
}

/**
 * # MotionDistressDetector
 *
 * Experimental fall and vehicle crash detection engine using the phone's hardware accelerometer.
 *
 * Ordinary Human Overview:
 * How does a smartphone detect a severe bicycle crash, car collision, or dangerous fall?
 * 1. **Phase 1 (The Impact Spike)**: A severe collision generates an acceleration spike
 *    greater than 2.8 times Earth's gravity ($> 27.5\,\text{m/s}^2$).
 * 2. **Phase 2 (The Inactivity Window)**: If someone merely drops their phone onto a sofa or
 *    picks it up quickly, normal motion resumes immediately. But in a severe injury, the phone
 *    remains completely motionless on the ground. The app monitors for 3.5 seconds of absolute stillness.
 * 3. **Phase 3 (The Safety Countdown)**: If stillness is confirmed, ResQLink AI sounds an audible
 *    chime and displays a 10-second countdown on screen so conscious users can easily cancel it.
 *
 * Safety Requirements:
 * - Marked as EXPERIMENTAL and disabled by default.
 * - Requires explicit user opt-in in Settings.
 * - NEVER silently dials 911 or emergency services.
 */
class MotionDistressDetector(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : SensorEventListener {

    companion object {
        /**
         * Impact acceleration threshold in meters per second squared.
         * Earth standard gravity is 9.8 m/s². 27.5 m/s² represents approximately 2.8g of force.
         */
        const val IMPACT_ACCELERATION_THRESHOLD = 27.5f

        /**
         * Window of time (3.5 seconds) following an impact during which the user's motion is evaluated.
         */
        const val INACTIVITY_WINDOW_MILLIS = 3500L

        /**
         * Maximum allowable gravitational deviation in m/s² to qualify as "complete stillness".
         */
        const val INACTIVITY_MAX_VARIATION = 1.8f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val hardwareAccelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _detectionState = MutableStateFlow(MotionDetectionState.IDLE)
    val detectionState: StateFlow<MotionDetectionState> = _detectionState.asStateFlow()

    private var impactTimestampEpochMillis: Long = 0L
    private var recordedPeakAccelerationG: Float = 0f
    private var inactivityEvaluationJob: Job? = null
    private var isCurrentlyEvaluatingInactivity: Boolean = false
    private val postImpactAccelerationMagnitudes = mutableListOf<Float>()

    var onImpactDistressDetected: ((peakAccelerationG: Float) -> Unit)? = null

    /**
     * Checks if the device has a physical 3-axis accelerometer sensor.
     */
    fun isSensorAvailable(): Boolean = hardwareAccelerometer != null

    /**
     * Registers the sensor listener and begins monitoring acceleration changes.
     */
    @Synchronized
    fun startMonitoring(): Boolean {
        if (hardwareAccelerometer == null || sensorManager == null) return false

        sensorManager.unregisterListener(this)
        val isRegisteredSuccessfully = sensorManager.registerListener(
            this,
            hardwareAccelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        if (isRegisteredSuccessfully) {
            _detectionState.value = MotionDetectionState.MONITORING
        }
        return isRegisteredSuccessfully
    }

    /**
     * Unregisters sensor callbacks and releases background evaluation tasks.
     */
    @Synchronized
    fun stopMonitoring() {
        inactivityEvaluationJob?.cancel()
        inactivityEvaluationJob = null
        isCurrentlyEvaluatingInactivity = false
        sensorManager?.unregisterListener(this)
        _detectionState.value = MotionDetectionState.IDLE
    }

    /**
     * Triggered by Android hardware whenever the phone moves or accelerates in 3D space.
     */
    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        if (sensorEvent == null || sensorEvent.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        // 3D vector acceleration components along X, Y, and Z physical axes
        val accelerationX = sensorEvent.values[0]
        val accelerationY = sensorEvent.values[1]
        val accelerationZ = sensorEvent.values[2]

        // 3D Euclidean vector magnitude: sqrt(X² + Y² + Z²)
        val totalAccelerationMagnitude = sqrt(
            accelerationX * accelerationX +
            accelerationY * accelerationY +
            accelerationZ * accelerationZ
        )

        // If currently in the 3.5-second post-impact evaluation phase, record stillness samples
        if (isCurrentlyEvaluatingInactivity) {
            synchronized(postImpactAccelerationMagnitudes) {
                postImpactAccelerationMagnitudes.add(totalAccelerationMagnitude)
                if (postImpactAccelerationMagnitudes.size > 50) {
                    postImpactAccelerationMagnitudes.removeAt(0)
                }
            }
            return
        }

        // Detect a high-G collision or fall impact spike
        if (totalAccelerationMagnitude >= IMPACT_ACCELERATION_THRESHOLD) {
            triggerInactivityEvaluation(totalAccelerationMagnitude)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Accelerometer calibration accuracy change (no action needed)
    }

    /**
     * Initiates the 3.5-second post-impact countdown to verify if the user remains incapacitated.
     */
    private fun triggerInactivityEvaluation(impactAccelerationMagnitude: Float) {
        isCurrentlyEvaluatingInactivity = true
        impactTimestampEpochMillis = System.currentTimeMillis()
        recordedPeakAccelerationG = impactAccelerationMagnitude
        _detectionState.value = MotionDetectionState.IMPACT_DETECTED_EVALUATING

        synchronized(postImpactAccelerationMagnitudes) {
            postImpactAccelerationMagnitudes.clear()
        }

        inactivityEvaluationJob?.cancel()
        inactivityEvaluationJob = coroutineScope.launch {
            // Wait for 3.5 seconds while collecting motion data
            delay(INACTIVITY_WINDOW_MILLIS)

            val collectedMotionSamples = synchronized(postImpactAccelerationMagnitudes) {
                postImpactAccelerationMagnitudes.toList()
            }
            isCurrentlyEvaluatingInactivity = false

            if (collectedMotionSamples.isEmpty()) {
                _detectionState.value = MotionDetectionState.MONITORING
                return@launch
            }

            val standardEarthGravity = 9.8f
            // Measure the maximum deviation from resting Earth gravity (9.8 m/s²)
            val maximumGravitationalDeviation = collectedMotionSamples.maxOf { sample ->
                Math.abs(sample - standardEarthGravity)
            }

            if (maximumGravitationalDeviation <= INACTIVITY_MAX_VARIATION) {
                // Inactivity confirmed: The phone experienced a violent impact and has remained motionless
                _detectionState.value = MotionDetectionState.DISTRESS_TRIGGERED
                notifyDistress(recordedPeakAccelerationG)
            } else {
                // Normal motion resumed (the user picked up the phone or was uninjured) -> Dismiss as false alarm
                _detectionState.value = MotionDetectionState.MONITORING
            }
        }
    }

    /**
     * Registers the trigger into the central [EmergencyEventManager] to launch the 10s countdown.
     */
    private fun notifyDistress(peakAccelerationG: Float) {
        val formattedGForce = String.format(Locale.US, "%.1fg", peakAccelerationG / 9.8f)
        EmergencyEventManager.getInstance().registerTrigger(
            source = EmergencyTriggerSource.EXPERIMENTAL_MOTION_IMPACT,
            detectedPhrase = "Impact detected ($formattedGForce) followed by lack of movement."
        )

        onImpactDistressDetected?.invoke(peakAccelerationG)
    }
}
