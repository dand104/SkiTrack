package org.skitrace.skitrace.data.di

import android.content.Context
import org.skitrace.skitrace.data.location.GmsLocationClient
import org.skitrace.skitrace.data.location.LocationClient
import org.skitrace.skitrace.data.recognition.ActivityClient
import org.skitrace.skitrace.data.recognition.GmsActivityClient

object ServicesProvider {
    fun provideLocationClient(context: Context): LocationClient = GmsLocationClient(context)
    fun provideActivityClient(context: Context): ActivityClient = GmsActivityClient(context)
}