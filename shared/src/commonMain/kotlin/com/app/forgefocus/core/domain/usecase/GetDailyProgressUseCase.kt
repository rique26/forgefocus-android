package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.DailyProgress
import com.app.forgefocus.features.mountains.domain.GoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface GetDailyProgressUseCase {
    operator fun invoke(goalId: Long): Flow<DailyProgress>
}

class GetDailyProgressUseCaseImpl(
    private val repository: GoalRepository
) : GetDailyProgressUseCase {

    @OptIn(ExperimentalTime::class)
    override operator fun invoke(goalId: Long): Flow<DailyProgress> {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        val startOfDay = today.atStartOfDayIn(zone).toEpochMilliseconds()
        val endOfDay = today.plus(1, DateTimeUnit.DAY)
            .atStartOfDayIn(zone)
            .toEpochMilliseconds()

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