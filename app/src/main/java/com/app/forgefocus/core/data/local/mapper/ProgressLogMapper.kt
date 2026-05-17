package com.app.forgefocus.core.data.local.mapper

import com.app.forgefocus.core.data.local.database.ProgressLogEntity
import com.app.forgefocus.core.domain.model.ProgressLog

fun ProgressLogEntity.toDomain(): ProgressLog = ProgressLog(
    id = id,
    goalId = goalId,
    timestamp = timestamp,
    blocksCompleted = blocksCompleted
)

fun ProgressLog.toEntity(): ProgressLogEntity = ProgressLogEntity(
    id = id,
    goalId = goalId,
    timestamp = timestamp,
    blocksCompleted = blocksCompleted
)