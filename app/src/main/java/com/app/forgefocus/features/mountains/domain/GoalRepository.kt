package com.app.forgefocus.features.mountains.domain

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.ProgressLog
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    suspend fun createGoal(goal: Goal): Long
    suspend fun getGoalById(id: Long): Goal?
    fun getAllGoals(): Flow<List<Goal>>
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goal: Goal)
    suspend fun logProgress(goalId: Long, blocksCompleted: Int = 1, timestamp: Long)
    fun getDailyLogs(goalId: Long, startOfDay: Long, endOfDay: Long): Flow<List<ProgressLog>>
    fun getAllLogsFrom(startTime: Long): Flow<List<ProgressLog>>
}