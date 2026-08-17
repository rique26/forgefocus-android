package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * "Pill" com um ícone (emoji) e um rótulo curto — usado pra mostrar métricas
 * (ex: blocos minerados, tempo registrado) de forma mais escaneável do que
 * texto corrido numa Row.
 *
 * Componente só recebe o texto já formatado por quem o usa. Não guarda
 * estado nem sabe de onde vêm os números — só exibe.
 *
 * @param icon emoji ou glifo curto exibido à esquerda do texto.
 * @param label texto já formatado (ex: "12/40 blocos").
 */
@Composable
fun MetricChip(
    icon: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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