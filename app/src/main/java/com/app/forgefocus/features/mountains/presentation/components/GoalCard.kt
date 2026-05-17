package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType

@Composable
fun GoalCard(
    goal: Goal,
    progress: Float,
    onBreakClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
        onClick = onCardClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column (
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ){
                    Text(
                        text = goal.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        CustomBadge(text = goal.durationFormatted)
                        CustomBadge(text = "${(progress * 100).toInt()}%")
                        CustomBadge(text = goal.type.name)
                    }
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Deletar Meta",
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFFF9FAFB), shape = RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                MountainCanvas(goal = goal)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${goal.progress}/${goal.totalTarget} blocos quebrados",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )

                Button(
                    onClick = onBreakClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("⛏️ Quebrar!", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomBadge(text: String) {
    Box(
        modifier = Modifier
            .background(Color(0xFFF3F4F6), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 11.sp, color = Color(0xFF4B5563), fontWeight = FontWeight.Medium)
    }
}

@Preview(showBackground = true, name = "Card com Progresso")
@Composable
fun GoalCardProgressPreview() {
    val mockGoal = Goal(
        id = 1L,
        title = "Treino de Violino (Fischer)",
        type = GoalType.DAILY,
        duration = 3,
        durationUnit = DurationUnit.WEEKS,
        dailyTarget = 1.5f,
        totalTarget = 120,
        progress = 48,
        dayProgress = 1,
        createdAt = System.currentTimeMillis(),
        color = 0xFF667eeaL
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GoalCard(
                goal = mockGoal,
                progress = 0.40f, // 40%
                onBreakClick = {},
                onDeleteClick = {},
                onCardClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Card Zerado / Novo")
@Composable
fun GoalCardNewPreview() {
    val mockGoal = Goal(
        id = 2L,
        title = "Estudar Arquitetura Android",
        type = GoalType.PROJECT,
        duration = 1,
        durationUnit = DurationUnit.WEEKS,
        dailyTarget = 2.0f,
        totalTarget = 60,
        progress = 0,
        dayProgress = 0,
        createdAt = System.currentTimeMillis(),
        color = 0xFF4CAF50L
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GoalCard(
                goal = mockGoal,
                progress = 0.0f,
                onBreakClick = {},
                onDeleteClick = {},
                onCardClick = {}
            )
        }
    }
}