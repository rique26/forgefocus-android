package com.app.forgefocus.core.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.forgefocus.core.data.local.dao.GoalDao
import com.app.forgefocus.core.data.local.dao.ProgressLogDao

@Database(entities = [GoalEntity::class, ProgressLogEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class ForgeFocusDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun progressLogDao(): ProgressLogDao
}