package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.features.mountains.domain.GoalRepository

interface DeleteGoalUseCase {
    suspend operator fun invoke(goal: Goal): Result<Unit>
}

class DeleteGoalUseCaseImpl(
    private val repository: GoalRepository
) : DeleteGoalUseCase {
    override suspend operator fun invoke(goal: Goal): Result<Unit> = runCatching {
        repository.deleteGoal(goal)
    }
}