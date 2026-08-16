package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType
import forgefocus.shared.generated.resources.Res
import forgefocus.shared.generated.resources.mountain_illustration
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val VISUAL_CHUNKS = 24
private const val GRID_COLS = 6
private const val GRID_ROWS = 4

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
                Column(
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
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

            MountainSnapshot(
                goal = goal,
                progressFraction = progress,
                seed = goal.id,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )


//            Spacer(modifier = Modifier.height(16.dp))
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(180.dp)
//                    .background(Color(0xFFF9FAFB), shape = RoundedCornerShape(12.dp))
//                    .padding(8.dp)
//            ) {
//                MountainCanvas(goal = goal)
//            }

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
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFF4B5563),
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------- Previews (sem mudanças) ----------

@OptIn(ExperimentalTime::class)
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
        createdAt = Clock.System.now().toEpochMilliseconds(),
        color = 0xFF667eeaL
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GoalCard(
                goal = mockGoal,
                progress = 0.40f,
                onBreakClick = {},
                onDeleteClick = {},
                onCardClick = {}
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
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
        createdAt = Clock.System.now().toEpochMilliseconds(),
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