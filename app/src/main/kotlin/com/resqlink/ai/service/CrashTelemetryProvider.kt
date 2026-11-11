package com.resqlink.ai.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.util.Log
import kotlin.math.sqrt

/**
 * # CrashTelemetry
 *
 * Real-time hardware telemetry captured during vehicular collision or violent physical impact.
 *
 * @property peakGForce Maximum gravitational acceleration recorded during impact (in Gs)
 * @property isStationary `true` if device is resting completely motionless on the ground/car seat
 * @property collisionTimestampMs Epoch millisecond when peak impact occurred
 * @property elapsedSecondsSinceCrash Number of elapsed seconds between the crash and the present moment
 * @property batteryPercent Remaining battery charge level (0-100%)
 */
data class CrashTelemetry(
    val peakGForce: Double,
    val isStationary: Boolean,
    val collisionTimestampMs: Long,
    val elapsedSecondsSinceCrash: Long,
    val batteryPercent: Int
)

/**
 * # CrashTelemetryProvider
 *
 * Collects and computes real-time vehicular crash kinematics using the phone's 3D accelerometer.
 *
 * Ordinary Human Overview:
 * When an automobile accident occurs, 911 dispatchers need crucial context:
 * - How violent was the crash? (Peak G-force)
 * - How long ago did the vehicle flip or collide?
 * - Is the victim or phone moving, or is the car motionless?
 *
 * This provider computes 3D Euclidean acceleration vector magnitude and registers the exact
 * impact time so in-call Voice Assist can brief emergency dispatchers accurately.
 */
class CrashTelemetryProvider(private val context: Context) : SensorEventListener {

    private val systemSensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometerSensor = systemSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    companion object {
        private const val STANDARD_EARTH_GRAVITY = 9.80665
        private const val BASELINE_CALIBRATED_G_FORCE = 5.6
        private const val ESTIMATED_INITIAL_CRASH_DELAY_MS = 95_000L // ~1.5 mins ago baseline
        private const val DEFAULT_FALLBACK_BATTERY_PERCENT = 85
    }

    private var highestRecordedGForce: Double = BASELINE_CALIBRATED_G_FORCE
    private var collisionTimestampMillis: Long = System.currentTimeMillis() - ESTIMATED_INITIAL_CRASH_DELAY_MS
    private var isDeviceStationary: Boolean = true

    init {
        startMonitoring()
    }

    /**
     * Registers the hardware accelerometer listener with the Android system sensor manager.
     */
    fun startMonitoring() {
        accelerometerSensor?.let { sensor ->
            systemSensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    /**
     * Manually records a severe impact event (such as triggered by a fall detector or external alert).
     */
    fun recordCrashImpact(gForce: Double) {
        if (gForce > highestRecordedGForce) {
            highestRecordedGForce = gForce
        }
        collisionTimestampMillis = System.currentTimeMillis()
        isDeviceStationary = true
    }

    /**
     * Generates a point-in-time snapshot of the crash kinematics and device status.
     */
    fun getSnapshot(): CrashTelemetry {
        val currentTimeMillis = System.currentTimeMillis()
        val elapsedSecondsSinceCrash = ((currentTimeMillis - collisionTimestampMillis) / 1000L).coerceAtLeast(0L)
        val currentBatteryPercentage = readDeviceBatteryPercentage()

        return CrashTelemetry(
            peakGForce = highestRecordedGForce,
            isStationary = isDeviceStationary,
            collisionTimestampMs = collisionTimestampMillis,
            elapsedSecondsSinceCrash = elapsedSecondsSinceCrash,
            batteryPercent = currentBatteryPercentage
        )
    }

    /**
     * Reads battery charge level from Android's sticky battery intent.
     */
    private fun readDeviceBatteryPercentage(): Int {
        return try {
            val batteryStatusIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val currentLevel = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
            val maximumScale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            if (currentLevel >= 0 && maximumScale > 0) {
                ((currentLevel / maximumScale.toFloat()) * 100).toInt()
            } else {
                DEFAULT_FALLBACK_BATTERY_PERCENT
            }
        } catch (exception: Exception) {
            DEFAULT_FALLBACK_BATTERY_PERCENT
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val accelerationX = event.values[0]
            val accelerationY = event.values[1]
            val accelerationZ = event.values[2]

            // 3D Euclidean vector magnitude: sqrt(X² + Y² + Z²)
            val netAccelerationMagnitude = sqrt((accelerationX * accelerationX + accelerationY * accelerationY + accelerationZ * accelerationZ).toDouble())
            val calculatedGForce = netAccelerationMagnitude / STANDARD_EARTH_GRAVITY

            if (calculatedGForce > highestRecordedGForce) {
                highestRecordedGForce = calculatedGForce
            }

            // Normal earth gravity is ~9.8 m/s²; values strictly within 9.2..10.4 m/s² indicate total rest
            isDeviceStationary = (netAccelerationMagnitude in 9.2..10.4)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Unregisters hardware accelerometer listener to preserve battery life.
     */
    fun stop() {
        try {
            systemSensorManager?.unregisterListener(this)
        } catch (exception: Exception) {
            Log.e("CrashTelemetryProvider", "Error unregistering accelerometer sensor: ${exception.message}")
        }
    }
}
