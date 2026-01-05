package org.skitrace.skitrace.data.di

import android.content.Context
import org.skitrace.skitrace.data.location.AospLocationClient
import org.skitrace.skitrace.data.location.LocationClient
import org.skitrace.skitrace.data.recognition.ActivityClient
import org.skitrace.skitrace.data.recognition.AospActivityClient

object ServicesProvider {
    fun provideLocationClient(context: Context): LocationClient = AospLocationClient(context)
    fun provideActivityClient(context: Context): ActivityClient = AospActivityClient()
}