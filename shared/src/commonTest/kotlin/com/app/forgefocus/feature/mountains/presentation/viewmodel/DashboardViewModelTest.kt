package com.app.forgefocus.feature.mountains.presentation.viewmodel

import app.cash.turbine.test
import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.core.domain.model.ProgressLog
import com.app.forgefocus.core.domain.usecase.BreakMountainBlockUseCase
import com.app.forgefocus.core.domain.usecase.CreateGoalUseCase
import com.app.forgefocus.core.domain.usecase.DeleteGoalUseCase
import com.app.forgefocus.core.domain.usecase.GetGoalsUseCase
import com.app.forgefocus.core.domain.usecase.GetProgressLogsUseCase
import com.app.forgefocus.features.mountains.domain.GetDashboardDataUseCase
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardViewModel
import com.app.forgefocus.features.mountains.presentation.viewmodel.GoalProgress
import com.app.forgefocus.features.mountains.presentation.viewmodel.MountainStats
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getGoalsUseCase: GetGoalsUseCase = mock()
    private val createGoalUseCase: CreateGoalUseCase = mock()
    private val breakMountainBlockUseCase: BreakMountainBlockUseCase = mock()
    private val deleteGoalUseCase: DeleteGoalUseCase = mock()
    private val getProgressLogsUseCase: GetProgressLogsUseCase = mock()
    private val getDashboardDataUseCase: GetDashboardDataUseCase = mock()

    private lateinit var viewModel: DashboardViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DashboardViewModel {
        return DashboardViewModel(
            getGoalsUseCase = getGoalsUseCase,
            createGoalUseCase = createGoalUseCase,
            breakMountainBlockUseCase = breakMountainBlockUseCase,
            deleteGoalUseCase = deleteGoalUseCase,
            getProgressLogsUseCase = getProgressLogsUseCase,
            getDashboardDataUseCase = getDashboardDataUseCase
        )
    }

    @Test
    fun `uiState should emit initial state with loading true`() = runTest {
        // Arrange
        every { getGoalsUseCase() } returns flowOf(emptyList())
        every { getProgressLogsUseCase(any(), any()) } returns flowOf(emptyList())
        every { getDashboardDataUseCase(
            any(),
            any(),
            any(),
            any(),
            any(),
            any())
        } returns Pair(emptyList(), MountainStats())

        // Act & Assert
        viewModel = createViewModel()

        viewModel.uiState.test {
            // O initialValue do stateIn é o estado de loading
            val initialState = awaitItem()
            assertTrue(initialState.isLoading)
        }
    }

    @Test
    fun `uiState should load data successfully when use cases emit values`() = runTest {
        // Arrange
        val mockGoal = Goal(
            id = 1L,
            title = "Estudar Kotlin",
            type = GoalType.PROJECT,
            duration = 1,
            durationUnit = DurationUnit.MONTHS,
            dailyTarget = 1f,
            totalTarget = 10,
            createdAt = 100L,
            color = 0xFFFFFFL
        )

        val mockLogs = listOf(
            ProgressLog(
                id = 1L,
                goalId = 1L,
                timestamp = Instant.fromEpochMilliseconds(100L),
                blocksCompleted = 2
            )
        )

        val expectedPair = Pair(
            listOf(GoalProgress(goal = mockGoal, period = PeriodFilter.DAILY)),
            MountainStats(blocksTodayCount = 2)
        )

        every { getGoalsUseCase() } returns flowOf(listOf(mockGoal))
        every { getProgressLogsUseCase(0L, any()) } returns flowOf(mockLogs)
        every { getDashboardDataUseCase(any(),
            any(),
            any(),
            any(),
            any(),
            any())
        } returns expectedPair

        // Act
        viewModel = createViewModel()

        // Assert
        viewModel.uiState.test {
            // Ignora o estado inicial de loading
            awaitItem()

            val loadedState = awaitItem()
            assertFalse(loadedState.isLoading)
            assertEquals(1, loadedState.goals.size)
            assertEquals("Estudar Kotlin", loadedState.goals.first().goal.title)
            assertEquals(2, loadedState.stats.blocksTodayCount)
        }
    }

    @Test
    fun `uiState should handle empty lists gracefully in negative scenario`() = runTest {
        // Arrange
        every { getGoalsUseCase() } returns flowOf(emptyList())
        every { getProgressLogsUseCase(0L, any()) } returns flowOf(emptyList())
        every { getDashboardDataUseCase(
            any(),
            any(),
            any(),
            any(),
            any(),
            any())
        } returns Pair(emptyList(), MountainStats())

        // Act
        viewModel = createViewModel()

        // Assert
        viewModel.uiState.test {
            awaitItem() // Loading

            val state = awaitItem()
            assertFalse(state.isLoading)
            assertTrue(state.goals.isEmpty())
            assertEquals(0, state.stats.blocksTodayCount)
        }
    }

    @Test
    fun `uiState should recalculate and trigger use cases when period or timeOffset changes`() = runTest {
        // Arrange
        every { getGoalsUseCase() } returns flowOf(emptyList())
        every { getProgressLogsUseCase(0L, any()) } returns flowOf(emptyList())
        every { getDashboardDataUseCase(
            any(),
            any(),
            any(),
            any(),
            any(),
            any())
        } returns Pair(emptyList(), MountainStats())

        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading inicial
            awaitItem() // Primeiro carregamento padrão (DAILY, 0)

            // Act - Altera o período para semanal, o que dispara o flatMapLatest novamente
            viewModel.changePeriod(PeriodFilter.WEEKLY)

            // Assert - Verifica se reagiu à mudança de estado reativa
            val updatedState = awaitItem()
            assertEquals(PeriodFilter.WEEKLY, updatedState.selectedPeriod)
        }
    }

    @Test
    fun `uiState should recalculate when navigatePrevious changes timeOffset`() = runTest {
        // Arrange
        every { getGoalsUseCase() } returns flowOf(emptyList())
        every { getProgressLogsUseCase(any(), any()) } returns flowOf(emptyList())
        every { getDashboardDataUseCase(
            any(), any(), any(), any(), any(), any()
        ) } returns Pair(emptyList(), MountainStats())

        viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // Loading inicial
            val initialLoadedState = awaitItem() // Primeiro carregamento (timeOffset = 0)

            assertEquals(0, initialLoadedState.timeOffset)

            // Act - Decrementa o timeOffset através da função existente navigatePrevious
            viewModel.navigatePrevious()

            // Assert
            val updatedState = awaitItem()
            assertEquals(-1, updatedState.timeOffset)
        }
    }

}