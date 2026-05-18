package com.app.forgefocus.features.mountains.presentation.util

import com.app.forgefocus.core.domain.model.PeriodFilter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DashboardDateTimeHelper {

    fun calculateTimeWindow(period: PeriodFilter, offset: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        when (period) {
            PeriodFilter.DAILY -> {
                calendar.add(Calendar.DAY_OF_YEAR, offset)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                return Pair(start, calendar.timeInMillis - 1)
            }
            PeriodFilter.WEEKLY -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.add(Calendar.WEEK_OF_YEAR, offset)
                val start = calendar.timeInMillis
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                return Pair(start, calendar.timeInMillis - 1)
            }
            PeriodFilter.MONTHLY -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, offset)
                val start = calendar.timeInMillis
                calendar.add(Calendar.MONTH, 1)
                return Pair(start, calendar.timeInMillis - 1)
            }
            PeriodFilter.YEARLY -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.add(Calendar.YEAR, offset)
                val start = calendar.timeInMillis
                calendar.add(Calendar.YEAR, 1)
                return Pair(start, calendar.timeInMillis - 1)
            }
        }
    }

    fun generatePeriodLabel(period: PeriodFilter, startTime: Long, offset: Int): String {
        if (offset == 0) {
            return when (period) {
                PeriodFilter.DAILY -> "Hoje"
                PeriodFilter.WEEKLY -> "Esta Semana"
                PeriodFilter.MONTHLY -> "Este Mês"
                PeriodFilter.YEARLY -> "Este Ano"
            }
        }
        if (offset == -1 && period == PeriodFilter.DAILY) return "Ontem"

        val sdf = when (period) {
            PeriodFilter.DAILY -> SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR"))
            PeriodFilter.WEEKLY -> SimpleDateFormat("'Semana de' dd/MM", Locale("pt", "BR"))
            PeriodFilter.MONTHLY -> SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
            PeriodFilter.YEARLY -> SimpleDateFormat("yyyy", Locale("pt", "BR"))
        }
        return sdf.format(Date(startTime)).replaceFirstChar { it.uppercase() }
    }
}