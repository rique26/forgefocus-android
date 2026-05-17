package com.app.forgefocus.core.domain.model

import java.time.Instant

data class ProgressLog(
    val id: Long = 0,
    val goalId: Long,
    val timestamp: Instant,
    val blocksCompleted: Int
)
