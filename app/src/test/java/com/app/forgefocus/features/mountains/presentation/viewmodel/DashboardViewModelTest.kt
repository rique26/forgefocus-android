package com.app.forgefocus.features.mountains.presentation.viewmodel

import app.cash.turbine.test
import com.app.forgefocus.MainDispatcherRule
import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.core.domain.usecase.BreakMountainBlockUseCase
import com.app.forgefocus.core.domain.usecase.CreateGoalUseCase
import com.app.forgefocus.core.domain.usecase.DeleteGoalUseCase
import com.app.forgefocus.core.domain.usecase.GetGoalsUseCase
import com.app.forgefocus.core.domain.usecase.GetProgressLogsUseCase
import com.app.forgefocus.features.mountains.domain.GetDashboardDataUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getGoalsUseCase: GetGoalsUseCase = mockk()
    private val createGoalUseCase: CreateGoalUseCase = mockk()
    private val breakMountainBlockUseCase: BreakMountainBlockUseCase = mockk()
    private val deleteGoalUseCase: DeleteGoalUseCase = mockk()
    private val getProgressLogsUseCase: GetProgressLogsUseCase = mockk()
    private val getDashboardDataUseCase: GetDashboardDataUseCase = mockk()

    private val mockGoalsList = listOf(
        Goal(
            id = 1,
            title = "Treino Violino",
            type = GoalType.PROJECT,
            duration = 1,
            durationUnit = DurationUnit.MONTHS,
            dailyTarget = 2.0f,
            totalTarget = 120,
            progress = 5,
            dayProgress = 2,
            createdAt = System.currentTimeMillis(),
            color = 0xFF4CAF50
        )
    )

    @Before
    fun setUp() {
        every { getGoalsUseCase() } returns flowOf(mockGoalsList)
        every { getProgressLogsUseCase(any(), any()) } returns flowOf(emptyList())
        every { getDashboardDataUseCase.getTimeWindow(any(), any()) } returns Pair(1000L, 2000L)
    }

    // --- SEÇÃO 1: TESTES DE NAVEGAÇÃO TEMPORAL ---

    @Test
    fun `quando o estado inicial for carregado, deve exibir o filtro do dia de HOJE`() = runTest {
        every {
            getDashboardDataUseCase(mockGoalsList, PeriodFilter.DAILY, 0)
        } returns DashboardUiState(selectedPeriod = PeriodFilter.DAILY, timeOffset = 0, periodLabel = "Hoje")

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(PeriodFilter.DAILY, state.selectedPeriod)
            assertEquals(0, state.timeOffset)
            assertEquals("Hoje", state.periodLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quando navegar uma vez para tras no filtro diario, deve exibir os dados de ONTEM`() = runTest {
        every {
            getDashboardDataUseCase(mockGoalsList, PeriodFilter.DAILY, -1)
        } returns DashboardUiState(selectedPeriod = PeriodFilter.DAILY, timeOffset = -1, periodLabel = "Ontem")

        val viewModel = createViewModel()
        viewModel.navigatePrevious()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(PeriodFilter.DAILY, state.selectedPeriod)
            assertEquals(-1, state.timeOffset)
            assertEquals("Ontem", state.periodLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quando navegar duas vezes para tras no filtro diario, deve exibir os dados de ANTES DE ONTEM`() = runTest {
        every {
            getDashboardDataUseCase(mockGoalsList, PeriodFilter.DAILY, -2)
        } returns DashboardUiState(selectedPeriod = PeriodFilter.DAILY, timeOffset = -2, periodLabel = "15 de maio")

        val viewModel = createViewModel()
        viewModel.navigatePrevious()
        viewModel.navigatePrevious()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(PeriodFilter.DAILY, state.selectedPeriod)
            assertEquals(-2, state.timeOffset)
            assertEquals("15 de maio", state.periodLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quando alterar para o filtro semanal e navegar para tras, deve exibir os dados da SEMANA PASSADA`() = runTest {
        every {
            getDashboardDataUseCase(mockGoalsList, PeriodFilter.WEEKLY, -1)
        } returns DashboardUiState(selectedPeriod = PeriodFilter.WEEKLY, timeOffset = -1, periodLabel = "Semana de 10/05")

        val viewModel = createViewModel()
        viewModel.changePeriod(PeriodFilter.WEEKLY)
        viewModel.navigatePrevious()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(PeriodFilter.WEEKLY, state.selectedPeriod)
            assertEquals(-1, state.timeOffset)
            assertEquals("Semana de 10/05", state.periodLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `quando alterar para o filtro mensal e navegar para tras, deve exibir os dados do MES PASSADO`() = runTest {
        every {
            getDashboardDataUseCase(mockGoalsList, PeriodFilter.MONTHLY, -1)
        } returns DashboardUiState(selectedPeriod = PeriodFilter.MONTHLY, timeOffset = -1, periodLabel = "Abril de 2026")

        val viewModel = createViewModel()
        viewModel.changePeriod(PeriodFilter.MONTHLY)
        viewModel.navigatePrevious()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(PeriodFilter.MONTHLY, state.selectedPeriod)
            assertEquals(-1, state.timeOffset)
            assertEquals("Abril de 2026", state.periodLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `ao mudar de filtro periodico, o timeOffset deve ser resetado obrigatoriamente para zero`() = runTest {
        every {
            getDashboardDataUseCase(mockGoalsList, PeriodFilter.MONTHLY, 0)
        } returns DashboardUiState(selectedPeriod = PeriodFilter.MONTHLY, timeOffset = 0, periodLabel = "Este Mês")

        val viewModel = createViewModel()
        viewModel.navigatePrevious()
        viewModel.navigatePrevious()

        viewModel.changePeriod(PeriodFilter.MONTHLY)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(PeriodFilter.MONTHLY, state.selectedPeriod)
            assertEquals(0, state.timeOffset)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `nao deve permitir incrementar o timeOffset se o usuario ja estiver visualizando o tempo presente`() = runTest {
        every {
            getDashboardDataUseCase(mockGoalsList, PeriodFilter.DAILY, 0)
        } returns DashboardUiState(selectedPeriod = PeriodFilter.DAILY, timeOffset = 0)

        val viewModel = createViewModel()
        viewModel.navigateNext()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0, state.timeOffset)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- SEÇÃO 2: TESTES DE EVENTOS E AÇÕES (MÉTODOS FALTANTES) ---

    @Test
    fun `quando quebrar um bloco com sucesso, deve emitir o evento BlockBroken`() = runTest {
        // GIVEN
        val goalId = 1L
        coEvery { breakMountainBlockUseCase(goalId) } returns Result.success(Unit)
        val viewModel = createViewModel()

        // WHEN & THEN (Testando o SharedFlow de Eventos usando Turbine)
        viewModel.event.test {
            viewModel.breakMountainBlock(goalId)

            val eventEmitted = awaitItem()
            assertEquals(MountainsEvent.BlockBroken, eventEmitted)
        }
    }

    @Test
    fun `quando falhar em quebrar um bloco, deve emitir o evento Error com a mensagem correta`() = runTest {
        // GIVEN
        val goalId = 1L
        val errorMessage = "Erro ao acessar o banco Room"
        coEvery { breakMountainBlockUseCase(goalId) } returns Result.failure(Exception(errorMessage))
        val viewModel = createViewModel()

        // WHEN & THEN
        viewModel.event.test {
            viewModel.breakMountainBlock(goalId)

            val eventEmitted = awaitItem()
            assert(eventEmitted is MountainsEvent.Error)
            assertEquals(errorMessage, (eventEmitted as MountainsEvent.Error).message)
        }
    }

    @Test
    fun `quando deletar uma meta com sucesso, deve emitir o evento GoalDeleted`() = runTest {
        // GIVEN
        val targetGoal = mockGoalsList.first()
        coEvery { deleteGoalUseCase(targetGoal) } returns Result.success(Unit)
        val viewModel = createViewModel()

        // WHEN & THEN
        viewModel.event.test {
            viewModel.deleteGoal(targetGoal)

            val eventEmitted = awaitItem()
            assertEquals(MountainsEvent.GoalDeleted, eventEmitted)
        }
    }

    private fun createViewModel() = DashboardViewModel(
        getGoalsUseCase = getGoalsUseCase,
        createGoalUseCase = createGoalUseCase,
        breakMountainBlockUseCase = breakMountainBlockUseCase,
        deleteGoalUseCase = deleteGoalUseCase,
        getProgressLogsUseCase = getProgressLogsUseCase,
        getDashboardDataUseCase = getDashboardDataUseCase
    )
}