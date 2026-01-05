package org.skitrace.skitrace.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

fun interface RawSensorListener {
    fun onSensorChanged(type: Int, v0: Float, v1: Float, v2: Float, v3: Float, timestamp: Long)
}

class SensorClient(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var listener: SensorEventListener? = null

    private val bootTimeOffset = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    fun startListening(callback: RawSensorListener) {
        stopListening()

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val v0 = event.values[0]
                val v1 = if (event.values.size > 1) event.values[1] else 0f
                val v2 = if (event.values.size > 2) event.values[2] else 0f
                val v3 = if (event.values.size > 3) event.values[3] else 0f

                val eventTimeMs = bootTimeOffset + (event.timestamp / 1_000_000L)

                callback.onSensorChanged(
                    event.sensor.type,
                    v0, v1, v2, v3,
                    eventTimeMs
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        val delay = SensorManager.SENSOR_DELAY_UI

        listener?.let { l ->
            linearAccel?.let { sensorManager.registerListener(l, it, delay) }
            rotation?.let { sensorManager.registerListener(l, it, delay) }
            pressure?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
    }

    fun stopListening() {
        listener?.let {
            sensorManager.unregisterListener(it)
        }
        listener = null
    }
}