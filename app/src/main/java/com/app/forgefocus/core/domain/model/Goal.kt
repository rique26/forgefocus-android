package com.app.forgefocus.core.domain.model

import kotlin.math.ceil

data class Goal(
    val id: Long = 0,
    val title: String,
    val type: GoalType,
    val duration: Int,
    val durationUnit: DurationUnit,
    val dailyTarget: Float, // horas
    val totalTarget: Int, // blocos calculados
    val progress: Int = 0,
    val dayProgress: Int = 0,
    val createdAt: Long,
    val color: Long,
    val brokenBlocks: Set<Int> = emptySet()
) {
    // Teto diário de blocos
    val maxDailyBlocks: Int
        get() = (dailyTarget * 2).toInt()

    val canvasColumns: Int
        get() = when {
            totalTarget <= 30 -> 6
            totalTarget <= 80 -> 10
            else -> 14
        }

    val canvasRows: Int
        get() = ceil(totalTarget.toDouble() / canvasColumns).toInt().coerceAtLeast(1)

    val durationFormatted: String
        get() = when (durationUnit) {
            DurationUnit.DAYS -> if (duration == 1) "1 dia" else "$duration dias"
            DurationUnit.WEEKS -> if (duration == 1) "1 semana" else "$duration semanas"
            DurationUnit.MONTHS -> if (duration == 1) "1 mês" else "$duration meses"
        }

    companion object {
        fun calculateTotalBlocks(duration: Int, unit: DurationUnit, dailyTarget: Float): Int {
            val days = when (unit) {
                DurationUnit.DAYS -> duration
                DurationUnit.WEEKS -> duration * 7
                DurationUnit.MONTHS -> duration * 30
            }
            val totalHours = days * dailyTarget
            return ((totalHours * 60) / 30).toInt()
        }
    }

    /**
     * Calcula a porcentagem de progresso normalizada (0.0f a 1.0f)
     * com base no período selecionado e no tipo de meta.
     */
    fun getProgressPercentageForPeriod(period: PeriodFilter): Float {
        return when (period) {
            PeriodFilter.DAILY -> {
                if (maxDailyBlocks == 0) 0f
                else (dayProgress.toFloat() / maxDailyBlocks).coerceIn(0f, 1f)
            }
            PeriodFilter.WEEKLY,
            PeriodFilter.MONTHLY,
            PeriodFilter.YEARLY -> {
                if (totalTarget == 0) 0f
                else (progress.toFloat() / totalTarget).coerceIn(0f, 1f)
            }
        }
    }
}

enum class GoalType {
    PROJECT,
    DAILY
}

enum class DurationUnit {
    DAYS,
    WEEKS,
    MONTHS
}