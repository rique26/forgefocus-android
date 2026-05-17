package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInQuad // IMPORT ALTERADO PARA GRAVIDADE SUAVE
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType
import kotlin.math.ceil
import kotlin.math.min

@Composable
fun MountainCanvas(goal: Goal) {
    val spacing = 4f
    val paddingBorda = 8f

    val density = LocalDensity.current
    val totalHeightPx = with(density) { 180.dp.toPx() }
    val disponivelHeight = totalHeightPx - (paddingBorda * 2)

    val columnsMontanha = goal.canvasColumns
    val rowsMontanha = goal.canvasRows

    val columnsPedras = 10
    val totalPedras = goal.progress

    val animadoresProgresso = List(totalPedras) { i ->
        remember(i) { Animatable(0f) }
    }

    animadoresProgresso.forEachIndexed { i, animatable ->
        LaunchedEffect(i) {
            if (animatable.value == 0f) {
                animatable.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = EaseInQuad
                    )
                )
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().clipToBounds()) {
        val sizeMaxMontanhaWidth = size.width * 0.45f

        val blockMontanhaSize = min(
            disponivelHeight / rowsMontanha - spacing,
            (sizeMaxMontanhaWidth - (columnsMontanha - 1) * spacing) / columnsMontanha
        ).coerceIn(6f, 24f)

        // 1. Desenha a Montanha Esquerda
        for (i in 0 until goal.totalTarget) {
            val row = i / columnsMontanha
            val col = i % columnsMontanha

            val xPos = paddingBorda + (col * (blockMontanhaSize + spacing))
            val yPos = size.height - paddingBorda - ((row + 1) * (blockMontanhaSize + spacing))

            if (!goal.brokenBlocks.contains(i)) {
                drawRect(
                    color = Color(0xFF667EEA).copy(alpha = 0.85f),
                    topLeft = Offset(x = xPos, y = yPos),
                    size = Size(blockMontanhaSize, blockMontanhaSize)
                )
            } else {
                drawRect(
                    color = Color(0xFFE5E7EB),
                    topLeft = Offset(x = xPos, y = yPos),
                    size = Size(blockMontanhaSize, blockMontanhaSize),
                    style = Stroke(width = 1.5f)
                )
            }
        }

        // 2. Desenha as Pedras Direitas
        if (totalPedras > 0) {
            val baseStartX = size.width - paddingBorda - blockMontanhaSize

            for (i in 0 until totalPedras) {
                val row = i / columnsPedras
                val col = i % columnsPedras

                val targetX = baseStartX - (col * (blockMontanhaSize + spacing))
                val finalYPos = size.height - paddingBorda - ((row + 1) * (blockMontanhaSize + spacing))

                val progresso = animadoresProgresso.getOrNull(i)?.value ?: 1f
                val atualY = progresso * finalYPos

                drawRect(
                    color = Color(0xFF374151),
                    topLeft = Offset(x = targetX, y = atualY),
                    size = Size(blockMontanhaSize, blockMontanhaSize)
                )
            }
        }
    }
}

// --- PREVIEWS ---
@Preview(showBackground = true, name = "Teste Animação por Linhas")
@Composable
fun MountainCanvasAnimatedLinesPreview() {
    val mockGoal = Goal(
        id = 10L,
        title = "Organização de Linhas",
        type = GoalType.DAILY,
        duration = 1,
        durationUnit = DurationUnit.WEEKS,
        dailyTarget = 2f,
        totalTarget = 40,
        progress = 14,
        dayProgress = 2,
        createdAt = System.currentTimeMillis(),
        color = 0xFF667eeaL,
        brokenBlocks = (0..13).toSet()
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(Color(0xFFF9FAFB))
                .padding(8.dp)
        ) {
            MountainCanvas(goal = mockGoal)
        }
    }
}