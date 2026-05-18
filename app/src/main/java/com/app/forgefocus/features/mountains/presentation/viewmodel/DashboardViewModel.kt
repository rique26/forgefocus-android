package com.app.forgefocus.features.mountains.presentation.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.core.domain.model.ProgressLog
import com.app.forgefocus.core.domain.usecase.BreakMountainBlockUseCase
import com.app.forgefocus.core.domain.usecase.CreateGoalUseCase
import com.app.forgefocus.core.domain.usecase.DeleteGoalUseCase
import com.app.forgefocus.core.domain.usecase.GetGoalsUseCase
import com.app.forgefocus.core.domain.usecase.GetProgressLogsUseCase
import com.app.forgefocus.features.mountains.domain.GetDashboardDataUseCase
import com.app.forgefocus.features.mountains.presentation.util.DashboardDateTimeHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val createGoalUseCase: CreateGoalUseCase,
    private val breakMountainBlockUseCase: BreakMountainBlockUseCase,
    private val deleteGoalUseCase: DeleteGoalUseCase,
    private val getProgressLogsUseCase: GetProgressLogsUseCase,
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.DAILY)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _timeOffset = MutableStateFlow(0)
    val timeOffset = _timeOffset.asStateFlow()

    private val _event = MutableSharedFlow<MountainsEvent>()
    val event = _event.asSharedFlow()


    @RequiresApi(Build.VERSION_CODES.O)
    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedPeriod,
        _timeOffset
    ) { period, offset ->
        Pair(period, offset)
    }.flatMapLatest { (period, offset) ->
        val timeWindow = DashboardDateTimeHelper.calculateTimeWindow(period, offset)

        combine(
            getGoalsUseCase(),
            getProgressLogsUseCase(0L, timeWindow.second)
        ) { goals, entityLogs ->

            val domainLogs = entityLogs.map { entity ->
                ProgressLog(
                    id = entity.id,
                    goalId = entity.goalId,
                    timestamp = entity.timestamp,
                    blocksCompleted = entity.blocksCompleted
                )
            }

            val (goalProgressList, stats) = getDashboardDataUseCase(
                goals = goals,
                logs = domainLogs,
                period = period,
                timeOffset = offset,
                startTimeWindow = timeWindow.first,
                endTimeWindow = timeWindow.second
            )

            val periodLabel = DashboardDateTimeHelper.generatePeriodLabel(period, timeWindow.first, offset)

            DashboardUiState(
                goals = goalProgressList,
                selectedPeriod = period,
                timeOffset = offset,
                periodLabel = periodLabel,
                stats = stats,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(isLoading = true)
    )

    fun breakMountainBlock(goalId: Long) {
        viewModelScope.launch {
            breakMountainBlockUseCase(goalId)
                .onSuccess {
                    _event.emit(MountainsEvent.BlockBroken)
                }
                .onFailure { error ->
                    _event.emit(MountainsEvent.Error(error.message ?: "Unknown error"))
                }
        }
    }

    fun createGoal(goal: Goal) {
        viewModelScope.launch {
            createGoalUseCase(goal)
                .onSuccess {
                    _event.emit(MountainsEvent.GoalCreated)
                }
                .onFailure { error ->
                    _event.emit(MountainsEvent.Error(error.message ?: "Failed to create goal"))
                }
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            deleteGoalUseCase(goal)
                .onSuccess {
                    _event.emit(MountainsEvent.GoalDeleted)
                }
                .onFailure { error ->
                    _event.emit(MountainsEvent.Error(error.message ?: "Failed to delete goal"))
                }
        }
    }

    fun changePeriod(period: PeriodFilter) {
        _timeOffset.value = 0
        _selectedPeriod.value = period
    }

    fun navigatePrevious() {
        _timeOffset.value -= 1
    }

    fun navigateNext() {
        if (_timeOffset.value < 0) {
            _timeOffset.value += 1
        }
    }
}