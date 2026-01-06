package org.skitrace.skitrace.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.skitrace.skitrace.data.db.entity.TrackRunEntity
import org.skitrace.skitrace.data.repository.TrackerRepository

data class StatsUiState(
    val totalDistanceKm: Double = 0.0,
    val totalVertDropM: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val runs: List<TrackRunEntity> = emptyList()
)

class StatsViewModel(
    repository: TrackerRepository
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = combine(
        repository.allRuns,
        repository.totalLifetimeDistance,
        repository.totalLifetimeVertical,
        repository.maxLifetimeSpeed
    ) { runs, dist, vert, speed ->
        StatsUiState(
            totalDistanceKm = dist / 1000.0,
            totalVertDropM = vert,
            maxSpeedKmh = speed * 3.6,
            runs = runs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    class Factory(private val repository: TrackerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(repository) as T
        }
    }
}