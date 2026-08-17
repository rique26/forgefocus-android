package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.forgefocus.core.domain.model.Goal
import kotlin.math.ceil

/**
 * Grade de blocos de 30min representando a meta diária de foco.
 *
 * Quando [Goal.dailyTarget] é alto, o número de blocos pode facilmente
 * ultrapassar a largura da tela (ex: 4h/dia = 8 blocos, 6h/dia = 12) — por
 * isso os blocos rolam horizontalmente em vez de espremer ou cortar os
 * últimos. O contador "X/Y" no cabeçalho garante que o progresso continue
 * visível mesmo sem rolar até o fim.
 *
 * @param goal meta atual (fornece a carga diária em horas via [Goal.dailyTarget]).
 * @param completedToday quantos blocos de 30min já foram registrados hoje.
 */
@Composable
fun DailyProgressBlocks(goal: Goal, completedToday: Int) {
    val blocosNecessariosNoDia = ceil((goal.dailyTarget * 60) / 30).toInt().coerceAtLeast(1)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Foco de Hoje (${goal.dailyTarget}h/dia)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )

                Text(
                    text = "$completedToday/$blocosNecessariosNoDia",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(blocosNecessariosNoDia) { i ->
                    val isDone = i < completedToday
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                color = if (isDone) Color(0xFF10B981) else Color(0xFFE5E7EB)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}