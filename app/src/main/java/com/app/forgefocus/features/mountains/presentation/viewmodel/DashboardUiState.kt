package com.app.forgefocus.features.mountains.presentation.viewmodel

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.PeriodFilter


data class DashboardUiState(
    val goals: List<GoalProgress> = emptyList(),
    val selectedPeriod: PeriodFilter = PeriodFilter.DAILY,
    val timeOffset: Int = 0,
    val periodLabel: String = PeriodFilter.DAILY.toString(),
    val stats: MountainStats = MountainStats(),
    val isLoading: Boolean = false
)

data class GoalProgress(
    val goal: Goal,
    private val period: PeriodFilter,
    val currentMinutes: Int = 0,
    val totalMinutes: Int = 0
) {
    val currentDayLabel: String
        get() {
            val millisDiff = (System.currentTimeMillis() - goal.createdAt).coerceAtLeast(0)
            val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(millisDiff).toInt() + 1
            return "Dia $days"
        }

    val startedOnLabel: String
        get() {
            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            return "Iniciado em ${formatter.format(java.util.Date(goal.createdAt))}"
        }

    val percentageLabel: String
        get() = "${((goal.progress.toFloat() / goal.totalTarget) * 100).toInt()}%"

    val currentFormattedTime: String
        get() = formatMinutesToText(currentMinutes)

    val totalFormattedTime: String
        get() = formatMinutesToText(totalMinutes)

    val progress: Float
        get() = goal.getProgressPercentageForPeriod(period)

    val dailyProgress: Float
        get() = goal.getProgressPercentageForPeriod(PeriodFilter.DAILY)

    private fun formatMinutesToText(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}min"
            hours > 0 -> "${hours}h"
            else -> "${minutes}min"
        }
    }
}

data class MountainStats(
    val goalsCount: Int = 0,
    val blocksTodayCount: Int = 0,
    val overallProgress: Float = 0.0f
)