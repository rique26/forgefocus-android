package com.app.forgefocus.features.mountains.presentation.util

import com.app.forgefocus.core.domain.model.PeriodFilter
import kotlinx.datetime.DateTimeUnit
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlin.time.ExperimentalTime
import kotlin.time.Clock

object DashboardDateTimeHelper {

    private val portugueseMonths = MonthNames(
        listOf(
            "janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"
        )
    )

    private val dailyFormat = LocalDate.Format {
        day()
        chars(" de ")
        monthName(portugueseMonths)
    }

    private val weeklyFormat = LocalDate.Format {
        chars("Semana de ")
        day()
        char('/')
        monthNumber()
    }

    private val monthlyFormat = LocalDate.Format {
        monthName(portugueseMonths)
        chars(" de ")
        year()
    }

    @OptIn(ExperimentalTime::class)
    fun calculateTimeWindow(period: PeriodFilter, offset: Int): Pair<Long, Long> {
        val timeZone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(timeZone)

        val (startLocalDate, endLocalDate) = when (period) {
            PeriodFilter.DAILY -> {
                val targetDay = today.plus(offset, DateTimeUnit.DAY)
                Pair(targetDay, targetDay.plus(1, DateTimeUnit.DAY))
            }
            PeriodFilter.WEEKLY -> {
                val daysToSubtract = today.dayOfWeek.ordinal
                val startOfWeek = today.minus(daysToSubtract, DateTimeUnit.DAY)
                    .plus(offset, DateTimeUnit.WEEK)
                Pair(startOfWeek, startOfWeek.plus(1, DateTimeUnit.WEEK))
            }
            PeriodFilter.MONTHLY -> {
                val startOfMonth = LocalDate(today.year, today.month, 1)
                    .plus(offset, DateTimeUnit.MONTH)
                Pair(startOfMonth, startOfMonth.plus(1, DateTimeUnit.MONTH))
            }
            PeriodFilter.YEARLY -> {
                val startOfYear = LocalDate(today.year + offset, 1, 1)
                Pair(startOfYear, startOfYear.plus(1, DateTimeUnit.YEAR))
            }
        }

        val start = startLocalDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        val end = endLocalDate.atStartOfDayIn(timeZone).toEpochMilliseconds() - 1

        return Pair(start, end)
    }

    @OptIn(ExperimentalTime::class)
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

        val timeZone = TimeZone.currentSystemDefault()
        val date = Instant.fromEpochMilliseconds(startTime).toLocalDateTime(timeZone).date

        val formattedText = when (period) {
            PeriodFilter.DAILY -> date.format(dailyFormat)
            PeriodFilter.WEEKLY -> date.format(weeklyFormat)
            PeriodFilter.MONTHLY -> date.format(monthlyFormat)
            PeriodFilter.YEARLY -> date.year.toString()
        }

        return formattedText.replaceFirstChar { it.uppercase() }
    }
}