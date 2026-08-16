package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.ProgressLog
import com.app.forgefocus.features.mountains.domain.GoalRepository
import kotlinx.coroutines.flow.Flow

interface GetProgressLogsUseCase {
    operator fun invoke(start: Long, end: Long): Flow<List<ProgressLog>>
}

class GetProgressLogsUseCaseImpl(
    private val repository: GoalRepository
) : GetProgressLogsUseCase {
    override operator fun invoke(start: Long, end: Long): Flow<List<ProgressLog>> {
        return repository.getLogsByPeriod(start, end)
    }
}