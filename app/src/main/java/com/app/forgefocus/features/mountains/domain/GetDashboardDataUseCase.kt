package com.app.forgefocus.features.mountains.domain

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardUiState
import com.app.forgefocus.features.mountains.presentation.viewmodel.GoalProgress
import com.app.forgefocus.features.mountains.presentation.viewmodel.MountainStats
import java.util.Calendar
import javax.inject.Inject

class GetDashboardDataUseCase @Inject constructor() {

    operator fun invoke(goals: List<Goal>, period: PeriodFilter): DashboardUiState {
        val goalsCount = goals.size
        if (goalsCount == 0) return DashboardUiState(selectedPeriod = period)

        val goalProgressList = goals.map { goal ->
            GoalProgress(
                goal = goal,
                period = period,
                currentMinutes = goal.progress * 30,
                totalMinutes = goal.totalTarget * 30
            )
        }

        val startTime = calculateStartTimeForPeriod(period)

        val stats = when (period) {
            PeriodFilter.DAILY -> {
                val totalDailyMax = goals.sumOf { it.maxDailyBlocks }
                val totalDailyProgress = goals.sumOf { it.dayProgress }

                MountainStats(
                    blocksTodayCount = totalDailyProgress,
                    overallProgress = if (totalDailyMax == 0) 0f else (totalDailyProgress.toFloat() / totalDailyMax).coerceIn(0f, 1f),
                    goalsCount = goalsCount
                )
            }
            PeriodFilter.WEEKLY -> {
                val totalWeeklyProgress = goals.sumOf { goal ->
                    if (goal.createdAt >= startTime) goal.progress else goal.dayProgress
                }
                val totalTarget = goals.sumOf { it.totalTarget }

                MountainStats(
                    blocksTodayCount = totalWeeklyProgress,
                    overallProgress = if (totalTarget == 0) 0f else (totalWeeklyProgress.toFloat() / totalTarget).coerceIn(0f, 1f),
                    goalsCount = goalsCount
                )
            }
            PeriodFilter.MONTHLY -> {
                val totalMonthlyProgress = goals.sumOf { goal ->
                    if (goal.createdAt >= startTime) goal.progress else goal.dayProgress
                }
                val totalTarget = goals.sumOf { it.totalTarget }

                MountainStats(
                    blocksTodayCount = totalMonthlyProgress,
                    overallProgress = if (totalTarget == 0) 0f else (totalMonthlyProgress.toFloat() / totalTarget).coerceIn(0f, 1f),
                    goalsCount = goalsCount
                )
            }
            PeriodFilter.YEARLY -> {
                val totalProgress = goals.sumOf { it.progress }
                val totalTarget = goals.sumOf { it.totalTarget }

                MountainStats(
                    blocksTodayCount = totalProgress,
                    overallProgress = if (totalTarget == 0) 0f else (totalProgress.toFloat() / totalTarget).coerceIn(0f, 1f),
                    goalsCount = goalsCount
                )
            }
        }

        return DashboardUiState(
            goals = goalProgressList,
            selectedPeriod = period,
            stats = stats
        )
    }

    private fun calculateStartTimeForPeriod(period: PeriodFilter): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        when (period) {
            PeriodFilter.DAILY -> { /* Início do dia de hoje */ }
            PeriodFilter.WEEKLY -> calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            PeriodFilter.MONTHLY -> calendar.set(Calendar.DAY_OF_MONTH, 1)
            PeriodFilter.YEARLY -> calendar.set(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}