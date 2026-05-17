package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatsBar(pedrasHoje: Int, progresso: Int, metas: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), label = "PEDRAS", value = "$pedrasHoje")
        StatCard(modifier = Modifier.weight(1f), label = "PROGRESSO", value = "$progresso%")
        StatCard(modifier = Modifier.weight(1f), label = "METAS", value = "$metas")
    }
}