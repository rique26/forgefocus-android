package com.app.forgefocus.features.mountains.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.features.mountains.presentation.components.DailyProgressBlocks
import com.app.forgefocus.features.mountains.presentation.components.FilterButtons
import com.app.forgefocus.features.mountains.presentation.components.GoalProgressRing
import com.app.forgefocus.features.mountains.presentation.components.MetricChip
import com.app.forgefocus.features.mountains.presentation.components.MountainReveal
import com.app.forgefocus.features.mountains.presentation.components.PeriodNavigator
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardViewModel
import org.koin.compose.viewmodel.koinViewModel

/**
 * Tela de detalhe de uma meta (montanha) — ponto de entrada **com estado**.
 *
 * Só resolve o [goalProgress] a partir do [goalId] no `uiState` do
 * [DashboardViewModel], trata o caso de carregamento e repassa tudo já
 * "achatado" em tipos simples pro [GoalDetailContent], que é quem realmente
 * desenha a tela. Essa separação existe pra permitir dar `@Preview` no
 * conteúdo sem precisar de Koin/ViewModel rodando.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    viewModel: DashboardViewModel = koinViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val goalProgress = uiState.goals.firstOrNull { it.goal.id == goalId }
    val historicalGoal = goalProgress?.goal

    DisposableEffect(Unit) {
        onDispose {
            viewModel.changePeriod(PeriodFilter.DAILY)
        }
    }

    if (goalProgress == null || historicalGoal == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF667eea))
        }
        return
    }

    GoalDetailContent(
        goal = historicalGoal,
        accent = Color(historicalGoal.color),
        selectedPeriod = uiState.selectedPeriod,
        periodLabel = uiState.periodLabel,
        isNextPeriodEnabled = uiState.timeOffset < 0,
        currentDayLabel = goalProgress.currentDayLabel,
        startedOnLabel = goalProgress.startedOnLabel,
        percentageLabel = goalProgress.percentageLabel,
        currentFormattedTime = goalProgress.currentFormattedTime,
        totalFormattedTime = goalProgress.totalFormattedTime,
        isRegisterEnabled = uiState.timeOffset == 0,
        onFilterChange = { viewModel.changePeriod(it) },
        onPreviousPeriod = { viewModel.navigatePrevious() },
        onNextPeriod = { viewModel.navigateNext() },
        onRegisterBlock = { viewModel.breakMountainBlock(historicalGoal.id) },
        onBackClick = onBackClick,
        onDeleteConfirmed = {
            viewModel.deleteGoal(historicalGoal)
            onBackClick()
        }
    )
}

/**
 * Conteúdo **sem estado** da tela de detalhe da meta.
 *
 * Recebe tudo já pronto (labels formatados, flags de habilitação, a própria
 * [Goal] pra alimentar a montanha) e só callbacks pros eventos — não conhece
 * `ViewModel`, `uiState` nem Koin. Isso é o que permite o [GoalDetailPreview]
 * logo abaixo existir sem precisar de nenhuma infraestrutura rodando.
 *
 * O estado de abrir/fechar o diálogo de exclusão é o único `remember` daqui
 * dentro: é puramente visual (não é decisão de negócio), então não precisa
 * subir até a tela com estado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalDetailContent(
    goal: Goal,
    accent: Color,
    selectedPeriod: PeriodFilter,
    periodLabel: String,
    isNextPeriodEnabled: Boolean,
    currentDayLabel: String,
    startedOnLabel: String,
    percentageLabel: String,
    currentFormattedTime: String,
    totalFormattedTime: String,
    isRegisterEnabled: Boolean,
    onFilterChange: (PeriodFilter) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onRegisterBlock: () -> Unit,
    onBackClick: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        goal.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = Color(0xFF111827)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.Close, "Voltar", tint = Color(0xFF111827))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Deletar", tint = Color(0xFFB0B5BE))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {

            // SELETORES DE PERÍODO (Dia, Semana, Mês, Ano)
            FilterButtons(
                selectedFilter = selectedPeriod,
                onFilterChange = onFilterChange
            )

            Spacer(modifier = Modifier.height(12.dp))

            // NAVEGADOR DE CALENDÁRIO RETROATIVO
            PeriodNavigator(
                periodLabel = periodLabel, // Ex: "Hoje", "Ontem"
                isNextEnabled = isNextPeriodEnabled, // Desabilita se já estiver no tempo presente
                accentColor = accent,
                onPrevious = onPreviousPeriod,
                onNext = onNextPeriod
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CANVAS DA MONTANHA — mesmo componente/animações de sempre, só
            // ganhou uma moldura sutil pra parar de "flutuar" no fundo branco.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF0F1F3), RoundedCornerShape(16.dp))
            ) {
                MountainReveal(
                    goal = goal,
                    seed = goal.id,
                    accentColor = accent,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CABEÇALHO DE STATUS — a porcentagem virou um anel (GoalProgressRing)
            // em vez de número solto, reforçando "quanto falta pra minerar".
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = currentDayLabel, // Ex: "Dia 14"
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = startedOnLabel, // Ex: "Iniciado em 16/05/2026"
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                }

                GoalProgressRing(
                    progress = goal.progress.toFloat() / goal.totalTarget.coerceAtLeast(1),
                    percentageLabel = percentageLabel,
                    accentColor = accent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BARRA DE PROGRESSO — mesma fonte de dados, agora na cor da meta
            LinearProgressIndicator(
                progress = { (goal.progress.toFloat() / goal.totalTarget).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // MÉTRICAS — chips (MetricChip) em vez de texto corrido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip(
                    icon = "⛏️",
                    label = "${goal.progress}/${goal.totalTarget} blocos"
                )
                MetricChip(
                    icon = "⏱️",
                    label = "$currentFormattedTime / $totalFormattedTime"
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // COMPONENTE DE BLOCOS DIÁRIOS (Sempre ativo no modo diário para renderizar o histórico do passado)
            if (selectedPeriod == PeriodFilter.DAILY) {
                DailyProgressBlocks(goal = goal, completedToday = goal.dayProgress)
                Spacer(modifier = Modifier.height(40.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // PRIMARY ACTION BUTTON (Registrar ou travado em modo histórico)
            Button(
                onClick = onRegisterBlock,
                enabled = isRegisterEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111827),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFEFF0F2),
                    disabledContentColor = Color(0xFF9CA3AF)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text = if (isRegisterEnabled) "⛏️ Registrar Bloco (30 min)" else "Visualizando Histórico",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // CONFIRMATION DIALOG (Deletar Meta)
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFDECEC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFEF4444)
                    )
                }
            },
            title = { Text("Deletar meta?", fontWeight = FontWeight.Bold) },
            text = { Text("Esta ação removerá a montanha inteira e todo o histórico de pedras.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }
}