package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Navegador retroativo de período (usado na tela de detalhe da meta): uma
 * pílula com seta anterior, o rótulo do período em exibição (ex: "Hoje",
 * "Ontem") e seta seguinte.
 *
 * Componente **totalmente controlado**: não guarda qual período está ativo
 * nem decide se dá pra avançar — isso continua no ViewModel/tela, que só
 * repassa [periodLabel], [isNextEnabled] e os dois callbacks de navegação.
 *
 * @param periodLabel texto já formatado do período atual.
 * @param isNextEnabled false quando já se está no período presente (não dá pra ir além).
 * @param accentColor cor da meta, aplicada nas setas quando habilitadas.
 * @param onPrevious chamado ao tocar na seta esquerda.
 * @param onNext chamado ao tocar na seta direita (ignorado se [isNextEnabled] for false).
 */
@Composable
fun PeriodNavigator(
    periodLabel: String,
    isNextEnabled: Boolean,
    accentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF9FAFB))
            .padding(vertical = 6.dp, horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Período Anterior",
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = periodLabel,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827)
        )

        IconButton(
            onClick = onNext,
            enabled = isNextEnabled,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isNextEnabled) Color.White else Color.Transparent)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Próximo Período",
                tint = if (isNextEnabled) accentColor else Color(0xFFD1D5DB),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}