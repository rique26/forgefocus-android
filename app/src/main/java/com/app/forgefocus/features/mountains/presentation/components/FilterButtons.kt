package com.app.forgefocus.features.mountains.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.forgefocus.core.domain.model.PeriodFilter

@Composable
fun FilterButtons(
    selectedFilter: PeriodFilter,
    onFilterChange: (PeriodFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PeriodFilter.entries.forEach { filter ->
            val isSelected = selectedFilter == filter
            Button(
                onClick = { onFilterChange(filter) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF667eea) else Color.White,
                    contentColor = if (isSelected) Color.White else Color(0xFF4B5563)
                ),
                border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE5E7EB)) else null,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                Text(
                    text = filter.displayName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}