package com.app.forgefocus.features.mountains.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.features.mountains.presentation.components.CreateGoalBottomSheet
import com.app.forgefocus.features.mountains.presentation.components.DashboardTopBar
import com.app.forgefocus.features.mountains.presentation.components.EmptyStateContent
import com.app.forgefocus.features.mountains.presentation.components.FilterButtons
import com.app.forgefocus.features.mountains.presentation.components.GoalCard
import com.app.forgefocus.features.mountains.presentation.components.StatsBar
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardUiState
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardViewModel
import com.app.forgefocus.features.mountains.presentation.viewmodel.MountainsEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel(),
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