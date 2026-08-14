package com.app.forgefocus.features.mountains.presentation.viewmodel

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.PeriodFilter
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


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
    @OptIn(ExperimentalTime::class)
    val currentDayLabel: String
        get() {
            val nowMillis = Clock.System.now().toEpochMilliseconds()
            val millisDiff = (nowMillis - goal.createdAt).coerceAtLeast(0)
            val days = (millisDiff / (1000 * 60 * 60 * 24)).toInt() + 1
            return "Dia $days"
        }

    @OptIn(ExperimentalTime::class)
    val startedOnLabel: String
        get() {
            val timeZone = TimeZone.currentSystemDefault()
            val date = Instant.fromEpochMilliseconds(goal.createdAt)
                .toLocalDateTime(timeZone).date

            val day = date.day.toString().padStart(2, '0')
            val month = date.month.toString().padStart(2, '0')
            val year = date.year

            return "Iniciado em $day/$month/$year"
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