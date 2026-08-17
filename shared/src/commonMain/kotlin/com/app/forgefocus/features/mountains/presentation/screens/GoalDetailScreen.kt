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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.forgefocus.core.domain.model.PeriodFilter
import com.app.forgefocus.features.mountains.presentation.components.DailyProgressBlocks
import com.app.forgefocus.features.mountains.presentation.components.FilterButtons
import com.app.forgefocus.features.mountains.presentation.components.MountainReveal
import com.app.forgefocus.features.mountains.presentation.viewmodel.DashboardViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Long,
    viewModel: DashboardViewModel = koinViewModel(),
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

    // Cor de destaque única pra tela inteira — a mesma que já ilumina a montanha,
    // em vez de indigo fixo espalhado pelos componentes.
    val accent = Color(historicalGoal.color)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        historicalGoal.title,
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
                selectedFilter = uiState.selectedPeriod,
                onFilterChange = { viewModel.changePeriod(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // NAVEGADOR DE CALENDÁRIO — pílula com botões circulares, alvo de
            // toque maior e estado desabilitado mais legível que antes.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF9FAFB))
                    .padding(vertical = 6.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigatePrevious() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Período Anterior",
                        tint = accent,
                        modifier = Modifier.size(22.dp)
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
                    enabled = uiState.timeOffset < 0, // Desabilita se já estiver no tempo presente
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (uiState.timeOffset < 0) Color.White else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Próximo Período",
                        tint = if (uiState.timeOffset < 0) accent else Color(0xFFD1D5DB),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

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
                    goal = historicalGoal,
                    seed = historicalGoal.id,
                    accentColor = accent,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CABEÇALHO DE STATUS — a porcentagem virou um anel em vez de
            // número solto, reforçando a ideia de "quanto falta pra minerar".
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = goalProgress.currentDayLabel, // Ex: "Dia 14"
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = accent
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = goalProgress.startedOnLabel, // Ex: "Iniciado em 16/05/2026"
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium
                    )
                }

                ProgressRing(
                    progress = historicalGoal.progress.toFloat() / historicalGoal.totalTarget.coerceAtLeast(1),
                    percentageLabel = goalProgress.percentageLabel,
                    accentColor = accent
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BARRA DE PROGRESSO — mesma fonte de dados, agora na cor da meta
            LinearProgressIndicator(
                progress = { (historicalGoal.progress.toFloat() / historicalGoal.totalTarget).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // MÉTRICAS — chips em vez de texto corrido, mais fácil de escanear
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    icon = "⛏️",
                    label = "${historicalGoal.progress}/${historicalGoal.totalTarget} blocos"
                )
                StatChip(
                    icon = "⏱️",
                    label = "${goalProgress.currentFormattedTime} / ${goalProgress.totalFormattedTime}"
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
                    disabledContainerColor = Color(0xFFEFF0F2),
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
                        viewModel.deleteGoal(historicalGoal)
                        showDeleteDialog = false
                        onBackClick()
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

/**
 * Anel de progresso estático (sem animação própria) que substitui o número
 * solto de porcentagem no cabeçalho. Usa exatamente os mesmos dados que já
 * alimentam a LinearProgressIndicator logo abaixo — nenhuma lógica nova.
 */
@Composable
private fun ProgressRing(
    progress: Float,
    percentageLabel: String,
    accentColor: Color,
    size: Dp = 56.dp,
    strokeWidth: Dp = 5.dp
) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            drawArc(
                color = accentColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
        Text(
            text = percentageLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )
    }
}

/** Pequeno chip de métrica — puramente visual, recebe texto já formatado. */
@Composable
private fun StatChip(icon: String, label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 13.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF374151),
            fontWeight = FontWeight.Medium
        )
    }
}