package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.forgefocus.core.domain.model.DurationUnit
import com.app.forgefocus.core.domain.model.Goal
import com.app.forgefocus.core.domain.model.GoalType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalBottomSheet(
    onDismiss: () -> Unit,
    onCreateGoal: (Goal) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(GoalType.PROJECT) }
    var duration by remember { mutableStateOf("2") }
    var durationUnit by remember { mutableStateOf(DurationUnit.MONTHS) }
    var dailyTarget by remember { mutableStateOf("2") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Nova Meta",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("O que você vai dominar?") },
                placeholder = { Text("Ex: Kotlin Avançado...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { if (it.all { c -> c.isDigit() }) duration = it },
                    label = { Text("Duração") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 1
                )

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = it },
                    modifier = Modifier.weight(1.2f)
                ) {
                    val labelUnidade = when(durationUnit) {
                        DurationUnit.DAYS -> "Dia(s)"
                        DurationUnit.WEEKS -> "Semana(s)"
                        DurationUnit.MONTHS -> "Mês(es)"
                    }

                    OutlinedTextField(
                        value = labelUnidade,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unidade") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true),
                        shape = RoundedCornerShape(10.dp),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        DurationUnit.entries.forEach { unit ->
                            val text = when(unit) {
                                DurationUnit.DAYS -> "Dia(s)"
                                DurationUnit.WEEKS -> "Semana(s)"
                                DurationUnit.MONTHS -> "Mês(es)"
                            }
                            DropdownMenuItem(
                                text = { Text(text) },
                                onClick = {
                                    durationUnit = unit
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = dailyTarget,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) dailyTarget = it },
                label = { Text("Meta diária (horas)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                maxLines = 1
            )

            val durationInt = duration.toIntOrNull() ?: 1
            val dailyHours = dailyTarget.toFloatOrNull() ?: 1f
            val totalBlocks = Goal.calculateTotalBlocks(durationInt, durationUnit, dailyHours)
            val totalHours = (totalBlocks * 30) / 60

            val unitLabelPlural = when(durationUnit) {
                DurationUnit.DAYS -> "dias"
                DurationUnit.WEEKS -> "semanas"
                DurationUnit.MONTHS -> "meses"
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                color = Color(0xFFEEF2FF),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🏔️ Escopo calculado da sua jornada:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "$durationInt $unitLabelPlural × ${dailyHours}h/dia = $totalHours horas",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E1B4B)
                    )
                    Text(
                        "Sua montanha terá exatamente $totalBlocks blocos de 30min",
                        fontSize = 12.sp,
                        color = Color(0xFF4F46E5)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        if (title.isNotBlank() && duration.isNotBlank() && dailyTarget.isNotBlank()) {
                            onCreateGoal(
                                Goal(
                                    id = 0,
                                    title = title,
                                    type = type,
                                    duration = durationInt,
                                    durationUnit = durationUnit,
                                    dailyTarget = dailyHours,
                                    totalTarget = totalBlocks,
                                    progress = 0,
                                    dayProgress = 0,
                                    createdAt = System.currentTimeMillis(),
                                    color = 0xFF667eea,
                                    brokenBlocks = emptySet()
                                )
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667eea)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Erguer Montanha", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
            }
        }
    }
}