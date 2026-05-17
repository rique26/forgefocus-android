package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.DailyProgress
import com.app.forgefocus.features.mountains.domain.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetDailyProgressUseCase @Inject constructor(
    private val repository: GoalRepository
) {

    operator fun invoke(goalId: Long): Flow<DailyProgress> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val startOfDay = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        return repository.getDailyLogs(goalId, startOfDay, endOfDay).map { logs ->
            val goal = repository.getGoalById(goalId) ?: throw Exception("Goal not found")
            val completedToday = logs.sumOf { it.blocksCompleted }

            DailyProgress(
                goal = goal,
                completedBlocksToday = completedToday,
                totalBlocksToday = (goal.dailyTarget * 2).toInt()
            )
        }
    }
}