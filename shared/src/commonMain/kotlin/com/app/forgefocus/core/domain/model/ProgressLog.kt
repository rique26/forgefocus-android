package com.app.forgefocus.core.domain.model

import kotlin.time.Instant
import kotlin.time.ExperimentalTime

data class ProgressLog @OptIn(ExperimentalTime::class) constructor(
    val id: Long = 0,
    val goalId: Long,
    val timestamp: Instant,
    val blocksCompleted: Int
)
