package org.skitrace.skitrace.ui.trace

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.skitrace.skitrace.core.model.SkiStatistics
import org.skitrace.skitrace.core.model.TrackState
import org.skitrace.skitrace.data.repository.TrackerRepository
import org.skitrace.skitrace.service.TrackerService

class TraceViewModel(
    private val application: Application,
    private val repository: TrackerRepository
) : AndroidViewModel(application) {

    val stats: StateFlow<SkiStatistics> = repository.currentStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SkiStatistics())

    val isTracking: StateFlow<Boolean> = repository.isTracking
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currentStateLabel: StateFlow<String> = repository.currentStats.map {
        when(it.state()) {
            TrackState.SKIING -> "Skiing \uD83C\uDFBF"
            TrackState.LIFT -> "Lift \uD83D\uDEA1"
            TrackState.IDLE -> "Idle \u23F8\uFE0F"
            else -> "Ready"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Ready")

    fun toggleTracking() {
        val intent = Intent(application, TrackerService::class.java)
        if (isTracking.value) {
            intent.action = TrackerService.ACTION_STOP
            application.startService(intent)
        } else {
            intent.action = TrackerService.ACTION_START
            application.startForegroundService(intent)
        }
    }

    class Factory(
        private val application: Application,
        private val repository: TrackerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TraceViewModel(application, repository) as T
        }
    }
}