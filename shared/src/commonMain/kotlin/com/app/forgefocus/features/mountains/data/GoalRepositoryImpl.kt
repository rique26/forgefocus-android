package com.app.forgefocus.features.mountains.data

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
import kotlin.time.Instant
import kotlin.time.ExperimentalTime

class GoalRepositoryImpl (
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
    
    @OptIn(ExperimentalTime::class)
    override suspend fun logProgress(goalId: Long, blocksCompleted: Int, timestamp: Long) {
        val logEntity = ProgressLogEntity(
            goalId = goalId,
            timestamp = Instant.fromEpochMilliseconds(timestamp),
            blocksCompleted = blocksCompleted
        )
        progressLogDao.insertLog(logEntity)
    }

   
    @OptIn(ExperimentalTime::class)
    override fun getDailyLogs(goalId: Long, startOfDay: Long, endOfDay: Long): Flow<List<ProgressLog>> {
        val startInstant = Instant.fromEpochMilliseconds(startOfDay)
        val endInstant = Instant.fromEpochMilliseconds(endOfDay)

        return progressLogDao.getDailyLogsForGoal(goalId, startInstant, endInstant).map { entities ->
            entities.map { it.toDomain() }
        }
    }

   
    @OptIn(ExperimentalTime::class)
    override fun getAllLogsFrom(startTime: Long): Flow<List<ProgressLog>> {
        val startInstant = Instant.fromEpochMilliseconds(startTime)
        return progressLogDao.getLogsFromTime(startInstant).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLogsByPeriod(start: Long, end: Long): Flow<List<ProgressLogEntity>> {
        return progressLogDao.getLogsByPeriod(start, end)
    }
}