package org.skitrace.skitrace.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_runs")
data class TrackRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long = 0,
    val totalDistance: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val avgSpeed: Double = 0.0,
    val verticalDrop: Double = 0.0,
    val durationMs: Long = 0,
    val activeSkiingMs: Long = 0,
    val liftMs: Long = 0,
    val descentsCount: Int = 0,
    val note: String? = null
)