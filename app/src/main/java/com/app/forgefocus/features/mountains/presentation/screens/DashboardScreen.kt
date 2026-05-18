package com.app.forgefocus.features.mountains.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.features.mountains.presentation.components.CreateGoalBottomSheet
import com.app.forgefocus.features.mountains.presentation.components.DashboardTopBar
import com.app.forgefocus.features.mountains.presentation.components.EmptyStateContent
import com.app.forgefocus.features.mountains.presentation.components.FilterButtons
import com.app.forgefocus.features.mountains.presentation.components.GoalCard
import com.app.forgefocus.features.mountains.presentation.components.StatsBar
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardUiState
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardViewModel
import com.app.forgefocus.features.mountains.presentation.viewmodel.GoalProgress
import com.app.forgefocus.features.mountains.presentation.viewmodel.MountainStats
import com.app.forgefocus.features.mountains.presentation.viewmodel.MountainsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    DashboardScreenContent(
        uiState = uiState,
        eventFlow = viewModel.event,
        onNavigateToDetail = onNavigateToDetail,
        onChangePeriod = { viewModel.changePeriod(it) },
        onBreakMountainBlock = { viewModel.breakMountainBlock(it) },
        onDeleteGoal = { viewModel.deleteGoal(it) },
        onCreateGoal = { viewModel.createGoal(it) }
    )

}

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    eventFlow: Flow<MountainsEvent>,
    onNavigateToDetail: (Long) -> Unit,
    onChangePeriod: (PeriodFilter) -> Unit,
    onBreakMountainBlock: (Long) -> Unit,
    onDeleteGoal: (Goal) -> Unit,
    onCreateGoal: (Goal) -> Unit
) {
    var showCreateModal by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        eventFlow.collect { event ->
            when (event) {
                is MountainsEvent.Error -> {
                    errorMessage = event.message
                    scope.launch { snackbarHostState.showSnackbar(event.message) }
                }
                MountainsEvent.BlockBroken -> {
                    scope.launch { snackbarHostState.showSnackbar("Bloco quebrado! 🎉") }
                }
                MountainsEvent.GoalCreated -> {
                    showCreateModal = false
                    scope.launch { snackbarHostState.showSnackbar("Meta criada com sucesso!") }
                }
                MountainsEvent.GoalDeleted -> {
                    scope.launch { snackbarHostState.showSnackbar("Meta deletada") }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            DashboardTopBar(
                goalsCount = uiState.stats.goalsCount,
                onAddClick = { showCreateModal = true }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FilterButtons(
                selectedFilter = uiState.selectedPeriod,
                onFilterChange = onChangePeriod
            )

            StatsBar(
                pedrasHoje = uiState.stats.blocksTodayCount,
                progresso = (uiState.stats.overallProgress * 100).toInt(),
                metas = uiState.stats.goalsCount
            )

            if (uiState.goals.isEmpty()) {
                EmptyStateContent()
            } else {
                // Separando as metas em andamento das concluídas
                val (inProgress, completed) = remember(uiState.goals) {
                    uiState.goals.partition { it.goal.progress < it.goal.totalTarget }
                }

                var isCompletedExpanded by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {

                    if (inProgress.isNotEmpty()) {
                        items(
                            items = inProgress,
                            key = { it.goal.id }
                        ) { goalProgress ->
                            GoalCard(
                                goal = goalProgress.goal,
                                progress = goalProgress.progress,
                                onBreakClick = { onBreakMountainBlock(goalProgress.goal.id) },
                                onDeleteClick = { onDeleteGoal(goalProgress.goal) },
                                onCardClick = { onNavigateToDetail(goalProgress.goal.id) }
                            )
                        }
                    } else if (completed.isNotEmpty()) {
                        item {
                            Text(
                                text = "Todas as metas foram montadas! 🏔️🎉",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    }

                    if (completed.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = { isCompletedExpanded = !isCompletedExpanded },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                val arrow = if (isCompletedExpanded) "▼" else "►"
                                Text(
                                    text = "$arrow Concluídas (${completed.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }

                        if (isCompletedExpanded) {
                            items(
                                items = completed,
                                key = { "completed_${it.goal.id}" }
                            ) { goalProgress ->
                                GoalCard(
                                    goal = goalProgress.goal,
                                    progress = goalProgress.progress,
                                    onBreakClick = { /* Disabled for completed goals */ },
                                    onDeleteClick = { onDeleteGoal(goalProgress.goal) },
                                    onCardClick = { onNavigateToDetail(goalProgress.goal.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateModal) {
        CreateGoalBottomSheet(
            onDismiss = { showCreateModal = false },
            onCreateGoal = onCreateGoal
        )
    }
}

@Preview(showBackground = true, name = "Dashboard com Metas")
@Composable
fun DashboardScreenPopulatedPreview() {
    // Mock simples de dados fictícios para renderizar no painel do Android Studio
    val mockGoals = listOf(
        GoalProgress(
            goal = Goal(
                id = 1L,
                title = "Estudar Kotlin Avançado",
                type = GoalType.DAILY,
                duration = 3,
                durationUnit = DurationUnit.WEEKS,
                dailyTarget = 2.0f,
                totalTarget = 180,
                progress = 45,
                dayProgress = 2,
                createdAt = System.currentTimeMillis(),
                color = 0xFF4CAF50L
            ),
            period = PeriodFilter.DAILY // Usando o construtor reativo inteligente
        ),
        GoalProgress(
            goal = Goal(
                id = 2L,
                title = "Treino de Violino (Fischer)",
                type = "PROJECT".let { GoalType.PROJECT },
                duration = 6,
                durationUnit = DurationUnit.WEEKS,
                dailyTarget = 1.5f,
                totalTarget = 240,
                progress = 12,
                dayProgress = 1,
                createdAt = System.currentTimeMillis(),
                color = 0xFFFF9800L
            ),
            period = PeriodFilter.DAILY
        )
    )

    val mockState = DashboardUiState(
        goals = mockGoals,
        selectedPeriod = PeriodFilter.DAILY,
        stats = MountainStats(
            goalsCount = 2,
            blocksTodayCount = 6,
            overallProgress = 0.6f
        )
    )

    MaterialTheme {
        DashboardScreenContent(
            uiState = mockState,
            eventFlow = emptyFlow(),
            onNavigateToDetail = {},
            onChangePeriod = {},
            onBreakMountainBlock = {},
            onDeleteGoal = {},
            onCreateGoal = {}
        )
    }
}

@Preview(showBackground = true, name = "Dashboard Vazio")
@Composable
fun DashboardScreenEmptyPreview() {
    val mockState = DashboardUiState(
        goals = emptyList(),
        selectedPeriod = PeriodFilter.DAILY,
        stats = MountainStats(
            goalsCount = 0,
            blocksTodayCount = 0,
            overallProgress = 0.0f
        )
    )

    MaterialTheme {
        DashboardScreenContent(
            uiState = mockState,
            eventFlow = emptyFlow(),
            onNavigateToDetail = {},
            onChangePeriod = {},
            onBreakMountainBlock = {},
            onDeleteGoal = {},
            onCreateGoal = {}
        )
    }
}