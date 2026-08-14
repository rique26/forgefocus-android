package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.features.mountains.domain.GoalRepository

class CreateGoalUseCase (
    private val repository: GoalRepository
) {
    suspend operator fun invoke(goal: Goal): Result<Long> = runCatching {
        repository.createGoal(goal)
    }
}