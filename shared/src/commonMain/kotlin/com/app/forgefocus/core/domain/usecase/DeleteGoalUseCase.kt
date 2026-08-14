package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.features.mountains.domain.GoalRepository

class DeleteGoalUseCase (
    private val repository: GoalRepository
) {
    suspend operator fun invoke(goal: Goal): Result<Unit> = runCatching {
        repository.deleteGoal(goal)
    }
}