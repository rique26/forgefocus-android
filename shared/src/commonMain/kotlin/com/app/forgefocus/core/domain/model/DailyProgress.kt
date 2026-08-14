package com.app.forgefocus.core.domain.model

data class DailyProgress(
    val goal: Goal,
    val completedBlocksToday: Int,
    val totalBlocksToday: Int
)