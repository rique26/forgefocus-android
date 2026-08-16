package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.features.mountains.domain.GoalRepository
import kotlinx.coroutines.flow.Flow

interface GetGoalsUseCase {
    operator fun invoke(): Flow<List<Goal>>
}

class GetGoalsUseCaseImpl(
    private val repository: GoalRepository
) : GetGoalsUseCase {
    override operator fun invoke(): Flow<List<Goal>> = repository.getAllGoals()
}