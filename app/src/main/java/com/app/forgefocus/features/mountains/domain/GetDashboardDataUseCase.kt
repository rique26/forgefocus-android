package com.app.forgefocus.features.mountains.domain

import android.os.Build
import androidx.annotation.RequiresApi
import com.app.forgefocus.core.data.local.database.ProgressLogEntity
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.core.domain.model.ProgressLog
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardUiState
import com.app.forgefocus.features.mountains.presentation.viewmodel.GoalProgress
import com.app.forgefocus.features.mountains.presentation.viewmodel.MountainStats
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class GetDashboardDataUseCase @Inject constructor() {

    @RequiresApi(Build.VERSION_CODES.O)
    operator fun invoke(
        goals: List<Goal>,
        logs: List<ProgressLog>,
        period: PeriodFilter,
        timeOffset: Int,
        startTimeWindow: Long,
        endTimeWindow: Long
    ): Pair<List<GoalProgress>, MountainStats> {

        if (goals.isEmpty()) {
            return Pair(emptyList(), MountainStats())
        }

        val logsByGoal = logs.groupBy { it.goalId }

        val goalProgressList = goals.map { goal ->
            val goalLogs = logsByGoal[goal.id].orEmpty()

            val blocksInPeriod = goalLogs
                .filter { it.timestamp.toEpochMilli() in startTimeWindow..endTimeWindow }
                .sumOf { it.blocksCompleted }

            val blocksAccumulatedUntilNow = goalLogs
                .sumOf { it.blocksCompleted }

            val computedDayProgress = if (period == PeriodFilter.DAILY) {
                if (timeOffset == 0) goal.dayProgress else blocksInPeriod
            } else {
                goal.dayProgress
            }

            val computedTotalProgress = if (timeOffset == 0) {
                goal.progress
            } else {
                blocksAccumulatedUntilNow
            }

            GoalProgress(
                goal = goal.copy(
                    dayProgress = computedDayProgress,
                    progress = computedTotalProgress
                ),
                period = period,
                currentMinutes = computedTotalProgress * 30,
                totalMinutes = goal.totalTarget * 30
            )
        }

        val stats = calculateMountainStats(goals, goalProgressList, period)

        return Pair(goalProgressList, stats)
    }

    private fun calculateMountainStats(
        goals: List<Goal>,
        goalProgressList: List<GoalProgress>,
        period: PeriodFilter
    ): MountainStats {
        val goalsCount = goals.size

        return if (period == PeriodFilter.DAILY) {
            val totalDailyMax = goals.sumOf { it.maxDailyBlocks }
            val totalDailyProgress = goalProgressList.sumOf { it.goal.dayProgress }
            MountainStats(
                goalsCount = goalsCount,
                blocksTodayCount = totalDailyProgress,
                overallProgress = if (totalDailyMax == 0) 0f else (totalDailyProgress.toFloat() / totalDailyMax).coerceIn(
                    0f,
                    1f
                )
            )
        } else {
            val totalProgress = goalProgressList.sumOf { it.goal.progress }
            val totalTarget = goals.sumOf { it.totalTarget }
            MountainStats(
                goalsCount = goalsCount,
                blocksTodayCount = totalProgress,
                overallProgress = if (totalTarget == 0) 0f else (totalProgress.toFloat() / totalTarget).coerceIn(
                    0f,
                    1f
                )
            )
        }
    }
}