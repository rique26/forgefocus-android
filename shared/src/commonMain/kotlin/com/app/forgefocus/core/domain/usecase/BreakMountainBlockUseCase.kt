package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.features.mountains.domain.GoalRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface BreakMountainBlockUseCase {
    suspend operator fun invoke(goalId: Long): Result<Unit>
}

class BreakMountainBlockUseCaseImpl (
    private val repository: GoalRepository
) : BreakMountainBlockUseCase {

    @OptIn(ExperimentalTime::class)
    override suspend operator fun invoke(goalId: Long): Result<Unit> = runCatching {
        val goal = repository.getGoalById(goalId) ?: throw Exception("Goal not found")

        val totalBlocksInMountain = goal.totalTarget

        if (goal.brokenBlocks.size < totalBlocksInMountain) {
            val newBrokenBlocks = goal.brokenBlocks.toMutableSet()

            val availableBlocks = (0 until totalBlocksInMountain).filter { it !in newBrokenBlocks }
            if (availableBlocks.isNotEmpty()) {
                val randomBlockIndex = availableBlocks.random()
                newBrokenBlocks.add(randomBlockIndex)
            }

            val updatedGoal = goal.copy(
                progress = minOf(goal.progress + 1, goal.totalTarget),
                dayProgress = goal.dayProgress + 1,
                brokenBlocks = newBrokenBlocks
            )

            val currentTimestamp = Clock.System.now().toEpochMilliseconds()

            repository.updateGoal(updatedGoal)
            repository.logProgress(goalId, 1, currentTimestamp)
        }
    }
}