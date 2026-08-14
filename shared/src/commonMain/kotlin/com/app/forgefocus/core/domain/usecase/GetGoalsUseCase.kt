package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.features.mountains.domain.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetGoalsUseCase (
    private val repository: GoalRepository
) {
    operator fun invoke(): Flow<List<Goal>> = repository.getAllGoals()
}