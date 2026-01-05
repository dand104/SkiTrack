package org.skitrace.skitrace.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

fun interface RawSensorListener {
    fun onSensorChanged(type: Int, v0: Float, v1: Float, v2: Float, v3: Float, timestamp: Long)
}

class SensorClient(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var listener: SensorEventListener? = null

    fun startListening(callback: RawSensorListener) {
        stopListening()

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val v0 = event.values[0]
                val v1 = if (event.values.size > 1) event.values[1] else 0f
                val v2 = if (event.values.size > 2) event.values[2] else 0f
                val v3 = if (event.values.size > 3) event.values[3] else 0f

                callback.onSensorChanged(
                    event.sensor.type,
                    v0, v1, v2, v3,
                    System.currentTimeMillis()
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register needed sensors
        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        listener?.let { l ->
            linearAccel?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
            rotation?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
            pressure?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        listener?.let {
            sensorManager.unregisterListener(it)
        }
        listener = null
    }
}