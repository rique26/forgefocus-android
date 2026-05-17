package com.app.forgefocus.core.data.local.mapper

import com.app.forgefocus.core.data.local.database.GoalEntity
import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    title = title,
    type = GoalType.valueOf(type),
    durationUnit = DurationUnit.valueOf(durationUnit),
    duration = duration,
    dailyTarget = dailyTarget,
    totalTarget = totalTarget,
    progress = progress,
    dayProgress = dayProgress,
    createdAt = createdAt,
    color = color,
    brokenBlocks = brokenBlocks
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    type = type.name,
    duration = duration,
    durationUnit = durationUnit.name,
    dailyTarget = dailyTarget,
    totalTarget = totalTarget,
    progress = progress,
    dayProgress = dayProgress,
    createdAt = createdAt,
    color = color,
    brokenBlocks = brokenBlocks
)