package com.app.forgefocus.features.mountains.data

import android.os.Build
import androidx.annotation.RequiresApi
import com.app.forgefocus.core.data.local.dao.GoalDao
import com.app.forgefocus.core.data.local.dao.ProgressLogDao
import com.app.forgefocus.core.data.local.database.ProgressLogEntity
import com.app.forgefocus.core.data.local.mapper.toDomain
import com.app.forgefocus.core.data.local.mapper.toEntity
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.ProgressLog
import com.app.forgefocus.features.mountains.domain.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao,
    private val progressLogDao: ProgressLogDao
) : GoalRepository {

    override suspend fun createGoal(goal: Goal): Long {
        return goalDao.insertGoal(goal.toEntity())
    }

    override suspend fun getGoalById(id: Long): Goal? {
        return goalDao.getGoalById(id)?.toDomain()
    }

    override fun getAllGoals(): Flow<List<Goal>> {
        return goalDao.getAllGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updateGoal(goal: Goal) {
        goalDao.updateGoal(goal.toEntity())
    }

    override suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal.toEntity())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun logProgress(goalId: Long, blocksCompleted: Int, timestamp: Long) {
        val logEntity = ProgressLogEntity(
            goalId = goalId,
            timestamp = Instant.ofEpochMilli(timestamp),
            blocksCompleted = blocksCompleted
        )
        progressLogDao.insertLog(logEntity)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getDailyLogs(goalId: Long, startOfDay: Long, endOfDay: Long): Flow<List<ProgressLog>> {
        val startInstant = Instant.ofEpochMilli(startOfDay)
        val endInstant = Instant.ofEpochMilli(endOfDay)

        return progressLogDao.getDailyLogsForGoal(goalId, startInstant, endInstant).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getAllLogsFrom(startTime: Long): Flow<List<ProgressLog>> {
        val startInstant = Instant.ofEpochMilli(startTime)
        return progressLogDao.getLogsFromTime(startInstant).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLogsByPeriod(start: Long, end: Long): Flow<List<ProgressLogEntity>> {
        return progressLogDao.getLogsByPeriod(start, end)
    }
}