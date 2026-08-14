package com.app.forgefocus.core.data.local.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val type: String,
    val duration: Int,
    @ColumnInfo(name = "duration_unit") val durationUnit: String,
    val dailyTarget: Float,
    val totalTarget: Int,
    val progress: Int,
    val dayProgress: Int,
    val createdAt: Long,
    val color: Long,
    @ColumnInfo(name = "broken_blocks")
    val brokenBlocks: Set<Int> = emptySet()
)