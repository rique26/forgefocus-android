package com.app.forgefocus.features.mountains.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.core.domain.usecase.BreakMountainBlockUseCase
import com.app.forgefocus.core.domain.usecase.CreateGoalUseCase
import com.app.forgefocus.core.domain.usecase.DeleteGoalUseCase
import com.app.forgefocus.core.domain.usecase.GetGoalsUseCase
import com.app.forgefocus.features.mountains.domain.GetDashboardDataUseCase
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
    private val getDashboardDataUseCase: GetDashboardDataUseCase
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(PeriodFilter.DAILY)
    val selectedPeriod = _selectedPeriod.asStateFlow()

    private val _event = MutableSharedFlow<MountainsEvent>()
    val event = _event.asSharedFlow()


    val uiState: StateFlow<DashboardUiState> = _selectedPeriod.flatMapLatest { period ->
        combine(
            getGoalsUseCase()
        ) { (goals) ->
            getDashboardDataUseCase(goals, period)
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
        _selectedPeriod.value = period
    }
}