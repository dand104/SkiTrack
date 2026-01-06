package org.skitrace.skitrace.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread

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
    private var sensorThread: HandlerThread? = null
    private var sensorHandler: Handler? = null
    private var listener: SensorEventListener? = null

    private val MAX_REPORT_LATENCY_US = 5_000_000

    private val BUFFER_SIZE = 300

    private var currentIndex = 0

    private val typeArray = IntArray(BUFFER_SIZE)
    private val v0Array = FloatArray(BUFFER_SIZE)
    private val v1Array = FloatArray(BUFFER_SIZE)
    private val v2Array = FloatArray(BUFFER_SIZE)
    private val v3Array = FloatArray(BUFFER_SIZE)
    private val timeArray = LongArray(BUFFER_SIZE)

    fun startListening(callback: BatchSensorListener) {
        stopListening()
        sensorThread = HandlerThread("SkiTraceSensorThread").apply {
            start()
        }
        sensorHandler = Handler(sensorThread!!.looper)
        currentIndex = 0

        listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                typeArray[currentIndex] = event.sensor.type
                v0Array[currentIndex] = event.values[0]
                v1Array[currentIndex] = if (event.values.size > 1) event.values[1] else 0f
                v2Array[currentIndex] = if (event.values.size > 2) event.values[2] else 0f
                v3Array[currentIndex] = if (event.values.size > 3) event.values[3] else 0f
                timeArray[currentIndex] = event.timestamp

                currentIndex++

                if (currentIndex >= BUFFER_SIZE) {
                    dispatchBatch(callback)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val linearAccel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val pressure = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        val rotation = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        val samplingPeriodUs = SensorManager.SENSOR_DELAY_GAME

        listener?.let { l ->
            val handler = sensorHandler
            linearAccel?.let {
                sensorManager.registerListener(l, it, samplingPeriodUs, MAX_REPORT_LATENCY_US, handler)
            }
            rotation?.let {
                sensorManager.registerListener(l, it, samplingPeriodUs, MAX_REPORT_LATENCY_US, handler)
            }
            pressure?.let {
                sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_NORMAL, MAX_REPORT_LATENCY_US,handler)
            }
        }
    }

    private fun dispatchBatch(callback: BatchSensorListener) {
        if (currentIndex == 0) return

        callback.onSensorBatch(
            typeArray.copyOfRange(0, currentIndex),
            v0Array.copyOfRange(0, currentIndex),
            v1Array.copyOfRange(0, currentIndex),
            v2Array.copyOfRange(0, currentIndex),
            v3Array.copyOfRange(0, currentIndex),
            timeArray.copyOfRange(0, currentIndex),
            currentIndex
        )
        currentIndex = 0
    }

    fun stopListening() {
        listener?.let { l ->
            sensorManager.flush(l)
            sensorManager.unregisterListener(l)
        }
        sensorThread?.quitSafely()
        sensorThread = null
        sensorHandler = null
        listener = null
        currentIndex = 0
    }
}