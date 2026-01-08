package org.skitrace.skitrace.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.skitrace.skitrace.core.model.TrackState

@RunWith(AndroidJUnit4::class)
class JniTest {

    private lateinit var processor: TrackProcessor

    @Before
    fun setUp() {
        processor = TrackProcessor()
    }

    @After
    fun tearDown() {
        processor.close()
    }

    @Test
    fun testSimplePointProcessing() {
        val result = processor.processPoint(55.75, 37.61, 150.0, 5.0, 1000L)

        assertNotNull(result)
        assertEquals(55.75, result.latitude, 0.001)
        assertEquals(150.0, result.altitude, 0.1)
    }

    @Test
    fun testStatisticsDefaults() {
        val stats = processor.statistics

        assertEquals(0.0, stats.totalDistanceMeters, 0.001)
        assertEquals(TrackState.IDLE, stats.state)
    }

    @Test
    fun testSensorBatchUpdate() {
        val types = intArrayOf(10)
        val v0 = floatArrayOf(0.1f)
        val v1 = floatArrayOf(0.0f)
        val v2 = floatArrayOf(9.8f)
        val v3 = floatArrayOf(0.0f)
        val timestamps = longArrayOf(2000L)

        try {
            processor.updateSensorsBatch(types, v0, v1, v2, v3, timestamps, 1)
        } catch (e: Exception) {
            fail("JNI Crash on array passing: ${e.message}")
        }
    }
}
