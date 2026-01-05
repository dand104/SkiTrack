package org.skitrace.skitrace.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock

fun interface BatchSensorListener {
    fun onSensorBatch(
        types: IntArray,
        v0s: FloatArray,
        v1s: FloatArray,
        v2s: FloatArray,
        v3s: FloatArray,
        timestamps: LongArray,
        count: Int
    )
}

class SensorClient(context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var listener: SensorEventListener? = null
    private val bootTimeOffset = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    private val BATCH_SIZE = 50
    private var currentIndex = 0

    private val typeArray = IntArray(BATCH_SIZE)
    private val v0Array = FloatArray(BATCH_SIZE)
    private val v1Array = FloatArray(BATCH_SIZE)
    private val v2Array = FloatArray(BATCH_SIZE)
    private val v3Array = FloatArray(BATCH_SIZE)
    private val timeArray = LongArray(BATCH_SIZE)

    fun startListening(callback: BatchSensorListener) {
        stopListening()
        currentIndex = 0

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                typeArray[currentIndex] = event.sensor.type
                v0Array[currentIndex] = event.values[0]
                v1Array[currentIndex] = if (event.values.size > 1) event.values[1] else 0f
                v2Array[currentIndex] = if (event.values.size > 2) event.values[2] else 0f
                v3Array[currentIndex] = if (event.values.size > 3) event.values[3] else 0f
                timeArray[currentIndex] = bootTimeOffset + (event.timestamp / 1_000_000L)

                currentIndex++

                if (currentIndex >= BATCH_SIZE) {
                    callback.onSensorBatch(
                        typeArray, v0Array, v1Array, v2Array, v3Array, timeArray, currentIndex
                    )
                    currentIndex = 0
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val delay = SensorManager.SENSOR_DELAY_UI

        listener?.let { l ->
            linearAccel?.let { sensorManager.registerListener(l, it, delay) }
            rotation?.let { sensorManager.registerListener(l, it, delay) }
            pressure?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_NORMAL) }
        }
    }

    fun stopListening() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        currentIndex = 0
    }
}