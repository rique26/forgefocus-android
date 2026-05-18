package com.app.forgefocus.features.mountains.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.features.mountains.presentation.components.DailyProgressBlocks
import com.app.forgefocus.features.mountains.presentation.components.FilterButtons
import com.app.forgefocus.features.mountains.presentation.components.MountainCanvas
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    viewModel: DashboardViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    val goalProgress = uiState.goals.firstOrNull { it.goal.id == goalId }
    // Usamos o goal vindo diretamente do mapeamento de progresso recalculado pelo UseCase
    val historicalGoal = goalProgress?.goal
    var showDeleteDialog by remember { mutableStateOf(false) }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(historicalGoal.title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF111827)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.Close, "Voltar", tint = Color(0xFF111827))
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Filled.Delete, "Deletar", tint = Color(0xFF9CA3AF))
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
                selectedFilter = uiState.selectedPeriod,
                onFilterChange = { viewModel.changePeriod(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // NAVEGADOR DE CALENDÁRIO RETROATIVO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF3F4F6), shape = RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigatePrevious() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Período Anterior",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = uiState.periodLabel, // Exibe dinamicamente "Hoje", "Ontem", etc.
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                IconButton(
                    onClick = { viewModel.navigateNext() },
                    enabled = uiState.timeOffset < 0 // Desabilita se já estiver no tempo presente
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Próximo Período",
                        tint = if (uiState.timeOffset < 0) Color(0xFF4F46E5) else Color(0xFF9CA3AF),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CANVAS DA MONTANHA (Muda dinamicamente conforme os blocos do histórico)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color(0xFFF9FAFB), shape = RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                MountainCanvas(goal = historicalGoal)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CABEÇALHO DE STATUS E PORCENTAGEM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = goalProgress.currentDayLabel, // Ex: "Dia 14"
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4F46E5)
                    )
                    Text(
                        text = goalProgress.startedOnLabel, // Ex: "Iniciado em 16/05/2026"
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                }

                Text(
                    text = goalProgress.percentageLabel, // Ex: "25%"
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SOLID PROGRESS BAR (Barra dinâmica baseada no dia visualizado)
            LinearProgressIndicator(
                progress = { (historicalGoal.progress.toFloat() / historicalGoal.totalTarget).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4F46E5),
                trackColor = Color(0xFFF3F4F6)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // METRICAS DOS BLOCOS MINERADOS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${historicalGoal.progress} de ${historicalGoal.totalTarget} blocos minerados",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${goalProgress.currentFormattedTime} / ${goalProgress.totalFormattedTime}",
                    fontSize = 13.sp,
                    color = Color(0xFF4F46E5),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // COMPONENTE DE BLOCOS DIÁRIOS (Sempre ativo no modo diário para renderizar o histórico do passado)
            if (uiState.selectedPeriod == PeriodFilter.DAILY) {
                DailyProgressBlocks(goal = historicalGoal, completedToday = historicalGoal.dayProgress)
                Spacer(modifier = Modifier.height(40.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // PRIMARY ACTION BUTTON (Registrar ou travado em modo histórico)
            Button(
                onClick = { viewModel.breakMountainBlock(historicalGoal.id) },
                enabled = uiState.timeOffset == 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF111827),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE5E7EB),
                    disabledContentColor = Color(0xFF9CA3AF)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Text(
                    text = if (uiState.timeOffset == 0) "⛏️ Registrar Bloco (30 min)" else "Visualizando Histórico",
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
            title = { Text("Deletar meta?", fontWeight = FontWeight.Bold) },
            text = { Text("Esta ação removerá a montanha inteira e todo o histórico de pedras.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(historicalGoal)
                        showDeleteDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) { Text("Deletar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}