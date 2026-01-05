package org.skitrace.skitrace.data.recognition

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AospActivityClient : ActivityClient {
    override fun getActivityUpdates(intervalMs: Long): Flow<ActivityData> = flow { }
}