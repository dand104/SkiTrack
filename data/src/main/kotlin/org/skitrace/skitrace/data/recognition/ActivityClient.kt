package org.skitrace.skitrace.data.recognition

import kotlinx.coroutines.flow.Flow

data class ActivityData(val type: Int, val confidence: Int)

interface ActivityClient {
    fun getActivityUpdates(intervalMs: Long): Flow<ActivityData>
}