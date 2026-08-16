package com.app.forgefocus.core.domain.usecase

import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType
import com.app.forgefocus.features.mountains.domain.GoalRepository
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CreateGoalUseCaseTest {

    private lateinit var repository: GoalRepository
    private val useCase: CreateGoalUseCase = mock()

    @BeforeTest
    fun setUp() {
        repository = mock()
    }

    @Test
    fun `invoke should insert goal and return id successfully when repository succeeds`() = runTest {
        // Arrange
        val goalToCreate = Goal(
            id = 0,
            title = "Learn Kotlin",
            type = GoalType.PROJECT,
            duration = 2,
            durationUnit = DurationUnit.MONTHS,
            dailyTarget = 2.0f,
            totalTarget = 240,
            progress = 0,
            dayProgress = 0,
            createdAt = 1000L,
            color = 0xFF667eea,
            brokenBlocks = emptySet()
        )
        val expectedCreatedId = 1L

        everySuspend { repository.createGoal(goalToCreate) } returns expectedCreatedId

        // Act
        val result = useCase(goalToCreate)

        // Assert
        assertTrue(result.isSuccess)
        assertEquals(expectedCreatedId, result.getOrNull())
        verifySuspend(VerifyMode.exactly(1)) { repository.createGoal(goalToCreate) }
    }

    @Test
    fun `invoke should return failure when repository throws exception`() = runTest {
        // Arrange
        val goalToCreate = Goal(
            id = 0,
            title = "Failed Goal",
            type = GoalType.PROJECT,
            duration = 1,
            durationUnit = DurationUnit.DAYS,
            dailyTarget = 1.0f,
            totalTarget = 2,
            progress = 0,
            dayProgress = 0,
            createdAt = 1000L,
            color = 0xFF667eea,
            brokenBlocks = emptySet()
        )
        val expectedErrorMessage = "Failed to save to database"

        everySuspend { repository.createGoal(goalToCreate) } throws Exception(expectedErrorMessage)

        // Act
        val result = useCase(goalToCreate)

        // Assert
        assertTrue(result.isFailure)
        assertEquals(expectedErrorMessage, result.exceptionOrNull()?.message)
        verifySuspend(VerifyMode.exactly(1)) { repository.createGoal(goalToCreate) }
    }
}