// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.android.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

/**
 * Opt-in, foreground-only shake detector. It has no background service or
 * sensor registration until [start] is called and stops on [stop].
 */
public class ShakeDetector(
    context: Context,
    private val threshold: Float = 2.7f,
    private val debounceMs: Long = 900L,
    private val onShake: () -> Unit,
    sensorManager: SensorManager? = context.getSystemService(SensorManager::class.java),
) : SensorEventListener {
    private val sensorManager = sensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var lastShakeMs = 0L
    private var started = false

    public fun start(): Boolean {
        if (started) return true
        val manager = sensorManager ?: return false
        val sensor = accelerometer ?: return false
        started = manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        return started
    }

    public val isStarted: Boolean get() = started

    public fun stop() {
        if (!started) return
        sensorManager?.unregisterListener(this)
        started = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val magnitude = kotlin.math.sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (magnitude < threshold) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastShakeMs < debounceMs) return
        lastShakeMs = now
        onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
