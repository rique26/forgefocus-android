package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Anel circular de progresso usado no cabeçalho da tela de detalhe da meta,
 * no lugar de um número de porcentagem solto.
 *
 * É puramente visual e **estático** (não anima sozinho, não guarda estado) —
 * recebe [progress] já calculado (0f..1f) e [percentageLabel] já formatado
 * pela tela que o usa. Não duplica nenhum cálculo que já existe na
 * `LinearProgressIndicator` da tela; os dois consomem a mesma fonte de progresso.
 *
 * @param progress fração já concluída da meta, entre 0f e 1f.
 * @param percentageLabel texto exibido no centro do anel (ex: "25%").
 * @param accentColor cor da meta (a mesma usada na montanha) — pinta o arco preenchido.
 * @param size diâmetro total do componente.
 * @param strokeWidth espessura do traço do anel.
 */
@Composable
fun GoalProgressRing(
    progress: Float,
    percentageLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    strokeWidth: Dp = 5.dp
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()

            // trilha de fundo — o "trilho" por onde o arco de progresso passa
            drawArc(
                color = accentColor.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            // arco preenchido, proporcional ao progresso atual
            drawArc(
                color = accentColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
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