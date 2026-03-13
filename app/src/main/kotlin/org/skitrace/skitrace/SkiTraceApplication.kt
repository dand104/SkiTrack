package org.skitrace.skitrace

import android.app.Application
import org.skitrace.skitrace.data.repository.TrackerRepository

class SkiTraceApplication : Application() {

    lateinit var trackerRepository: TrackerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        org.maplibre.android.MapLibre.getInstance(this)
        trackerRepository = TrackerRepository(this)
    }
}