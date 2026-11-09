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

data class CrashTelemetry(
    val peakGForce: Double,
    val isStationary: Boolean,
    val collisionTimestampMs: Long,
    val elapsedSecondsSinceCrash: Long,
    val batteryPercent: Int
)

class CrashTelemetryProvider(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private var peakGForce: Double = 5.6 // Default calibrated baseline for accident simulation
    private var collisionTimestampMs: Long = System.currentTimeMillis() - 95_000 // ~1.5 mins ago baseline
    private var isStationary: Boolean = true

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun recordCrashImpact(gForce: Double) {
        if (gForce > peakGForce) {
            peakGForce = gForce
        }
        collisionTimestampMs = System.currentTimeMillis()
        isStationary = true
    }

    fun getSnapshot(): CrashTelemetry {
        val now = System.currentTimeMillis()
        val elapsedSec = ((now - collisionTimestampMs) / 1000L).coerceAtLeast(0L)
        val batteryPct = getBatteryPercentage()

        return CrashTelemetry(
            peakGForce = peakGForce,
            isStationary = isStationary,
            collisionTimestampMs = collisionTimestampMs,
            elapsedSecondsSinceCrash = elapsedSec,
            batteryPercent = batteryPct
        )
    }

    private fun getBatteryPercentage(): Int {
        return try {
            val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 80
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
            if (level >= 0 && scale > 0) ((level / scale.toFloat()) * 100).toInt() else 85
        } catch (e: Exception) {
            85
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val netAccel = sqrt((x * x + y * y + z * z).toDouble())
            val gForce = netAccel / 9.80665

            if (gForce > peakGForce) {
                peakGForce = gForce
            }
            // If movement is very small, phone remains stationary on the floor/ground
            isStationary = (netAccel in 9.2..10.4)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.e("CrashTelemetryProvider", "Error unregistering sensor: ${e.message}")
        }
    }
}
