package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.data.local.database.ProgressLogEntity
import com.app.forgefocus.features.mountains.domain.GoalRepository
import kotlinx.coroutines.flow.Flow

class GetProgressLogsUseCase (
    private val repository: GoalRepository
) {

    operator fun invoke(start: Long, end: Long): Flow<List<ProgressLogEntity>> {
        return repository.getLogsByPeriod(start, end)
    }
}