package com.app.forgefocus.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.app.forgefocus.core.data.local.database.ProgressLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ProgressLogDao {
    @Insert
    suspend fun insertLog(log: ProgressLogEntity): Long

    @Query("SELECT * FROM progress_logs WHERE goal_id = :goalId ORDER BY timestamp DESC")
    fun getLogsForGoal(goalId: Long): Flow<List<ProgressLogEntity>>

    @Query(
        """
        SELECT * FROM progress_logs 
        WHERE goal_id = :goalId 
        AND timestamp >= :startOfDay 
        AND timestamp < :endOfDay
    """
    )
    fun getDailyLogsForGoal(
        goalId: Long,
        startOfDay: Instant,
        endOfDay: Instant
    ): Flow<List<ProgressLogEntity>>

    @Query(
        """
    SELECT * FROM progress_logs 
    WHERE timestamp >= :startTime
    """
    )
    fun getLogsFromTime(startTime: Instant): Flow<List<ProgressLogEntity>>

    @Query("SELECT * FROM progress_logs WHERE timestamp BETWEEN :start AND :end")
    fun getLogsByPeriod(start: Long, end: Long): Flow<List<ProgressLogEntity>>
}