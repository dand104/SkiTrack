package org.skitrace.skitrace.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.skitrace.skitrace.data.db.entity.TrackPointEntity
import org.skitrace.skitrace.data.db.entity.TrackRunEntity

@Dao
interface TrackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: TrackRunEntity): Long

    @Update
    suspend fun updateRun(run: TrackRunEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoints(points: List<TrackPointEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoint(point: TrackPointEntity)

    @Query("SELECT * FROM track_runs ORDER BY startTime DESC")
    fun getAllRuns(): Flow<List<TrackRunEntity>>

    @Query("SELECT * FROM track_runs ORDER BY startTime DESC")
    suspend fun getAllRunsSync(): List<TrackRunEntity>

    @Query("SELECT * FROM track_runs WHERE id = :id")
    suspend fun getRunById(id: Long): TrackRunEntity?
    
    @Query("DELETE FROM track_runs WHERE id = :id")
    suspend fun deleteRunById(id: Long)

    @Query("SELECT * FROM track_points WHERE runId = :runId ORDER BY timestamp ASC")
    suspend fun getPointsForRun(runId: Long): List<TrackPointEntity>

    @Query("SELECT SUM(totalDistance) FROM track_runs")
    fun getTotalDistance(): Flow<Double?>

    @Query("SELECT MAX(maxSpeed) FROM track_runs")
    fun getMaxSpeed(): Flow<Double?>

    @Query("SELECT SUM(verticalDrop) FROM track_runs")
    fun getTotalVerticalDrop(): Flow<Double?>
}